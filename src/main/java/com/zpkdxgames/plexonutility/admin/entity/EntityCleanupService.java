package com.zpkdxgames.plexonutility.admin.entity;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** One-pass bounded entity cleanup with conservative protection defaults. */
public final class EntityCleanupService {
    static final int MAX_REMOVALS_PER_TICK = 40;
    private static final Duration NEXT_BATCH_DELAY = Duration.ofMillis(50);
    private static final Set<EntityType> BOSSES = Set.of(EntityType.ENDER_DRAGON, EntityType.WITHER);
    private static final Set<EntityType> ALWAYS_PROTECTED = Set.of(
            EntityType.PLAYER, EntityType.INTERACTION, EntityType.MARKER);

    private final JavaPlugin plugin;
    private final CoreScheduler scheduler;
    private final Supplier<UtilityConfig> config;
    private final AdminAuditService audit;

    public EntityCleanupService(JavaPlugin plugin, CoreScheduler scheduler,
                                Supplier<UtilityConfig> config, AdminAuditService audit) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /** Compatibility constructor for unit tests/source callers. */
    public EntityCleanupService(Supplier<UtilityConfig> config, AdminAuditService audit) {
        this.plugin = null;
        this.scheduler = null;
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public Preview preview(Query query) {
        return plan(query).preview();
    }

    public Plan plan(Query query) {
        int matched = 0;
        int protectedCount = 0;
        Set<UUID> removable = new java.util.LinkedHashSet<>();
        for (Entity entity : candidates(query)) {
            if (!EntitySelector.matches(query.selection(), entity)) continue;
            matched++;
            if (isProtected(query.selection(), entity)) {
                protectedCount++;
            } else {
                removable.add(entity.getUniqueId());
            }
        }
        return new Plan(query, Set.copyOf(removable), matched, protectedCount);
    }

    public Result execute(CommandSender actor, Query query) {
        int matched = 0;
        int protectedCount = 0;
        int removed = 0;
        for (Entity entity : candidates(query)) {
            if (!EntitySelector.matches(query.selection(), entity)) continue;
            matched++;
            if (isProtected(query.selection(), entity)) {
                protectedCount++;
                continue;
            }
            entity.remove();
            removed++;
        }
        audit.log("KILLALL", actor,
                "selector=" + query.selection().canonical()
                        + " world=" + query.world().getName()
                        + " scope=" + query.scopeDescription()
                        + " removed=" + removed
                        + " protected=" + protectedCount
                        + " matched=" + matched);
        return new Result(matched, protectedCount, removed);
    }

    /**
     * Executes only UUIDs captured by a prior plan. Every surviving candidate is revalidated
     * immediately before removal; entities spawned after preview can never enter the operation.
     */
    public Result execute(CommandSender actor, Plan plan) {
        Query query = plan.query();
        int removed = 0;
        int newlyProtected = 0;
        Set<UUID> remaining = new java.util.HashSet<>(plan.candidateIds());
        for (Entity entity : candidates(query)) {
            if (!remaining.remove(entity.getUniqueId())) continue;
            if (!EntitySelector.matches(query.selection(), entity) || isProtected(query.selection(), entity)) {
                newlyProtected++;
                continue;
            }
            entity.remove();
            removed++;
        }
        int protectedCount = plan.protectedCount() + newlyProtected;
        audit.log("KILLALL", actor,
                "selector=" + query.selection().canonical()
                        + " world=" + query.world().getName()
                        + " scope=" + query.scopeDescription()
                        + " planned=" + plan.candidateIds().size()
                        + " removed=" + removed
                        + " protected=" + protectedCount
                        + " missing=" + remaining.size());
        return new Result(plan.matched(), protectedCount, removed);
    }

    /**
     * Executes a confirmed immutable plan with one Core-owned coordinator. Small plans remain
     * immediate; larger plans process at most {@value MAX_REMOVALS_PER_TICK} candidates per tick.
     */
    public CompletableFuture<Result> executeBatched(CommandSender actor, Plan plan) {
        if (plugin == null || scheduler == null || plan.candidateIds().size() <= MAX_REMOVALS_PER_TICK) {
            return CompletableFuture.completedFuture(execute(actor, plan));
        }
        BatchState state = new BatchState(actor, plan, new ArrayList<>(plan.candidateIds()));
        CompletableFuture<Result> result = new CompletableFuture<>();
        runBatch(state, result);
        return result;
    }

    private void runBatch(BatchState state, CompletableFuture<Result> result) {
        if (result.isDone()) return;
        if (plugin == null || !plugin.isEnabled()) {
            result.completeExceptionally(new IllegalStateException("PlexonUtility disabled during cleanup batch"));
            return;
        }

        int processed = 0;
        while (processed < MAX_REMOVALS_PER_TICK && state.index < state.ids.size()) {
            UUID id = state.ids.get(state.index++);
            processed++;
            Entity entity = state.plan.query().world().getEntity(id);
            if (entity == null || !entity.isValid()) {
                state.missing++;
                continue;
            }
            if (!withinScope(state.plan.query(), entity)
                    || !EntitySelector.matches(state.plan.query().selection(), entity)
                    || isProtected(state.plan.query().selection(), entity)) {
                state.newlyProtected++;
                continue;
            }
            entity.remove();
            state.removed++;
        }

        if (state.index >= state.ids.size()) {
            result.complete(finishBatch(state));
            return;
        }

        CoreScheduler.ObservedTaskHandle next = scheduler.schedulePrimaryObserved(
                plugin, NEXT_BATCH_DELAY, () -> runBatch(state, result));
        next.completion().whenComplete((ignored, error) -> {
            if (error != null && !result.isDone()) result.completeExceptionally(error);
        });
    }

    private Result finishBatch(BatchState state) {
        Query query = state.plan.query();
        int protectedCount = state.plan.protectedCount() + state.newlyProtected;
        audit.log("KILLALL", state.actor,
                "selector=" + query.selection().canonical()
                        + " world=" + query.world().getName()
                        + " scope=" + query.scopeDescription()
                        + " planned=" + state.plan.candidateIds().size()
                        + " removed=" + state.removed
                        + " protected=" + protectedCount
                        + " missing=" + state.missing
                        + " batched=true");
        return new Result(state.plan.matched(), protectedCount, state.removed);
    }

    private static boolean withinScope(Query query, Entity entity) {
        if (!entity.getWorld().equals(query.world())) return false;
        if (query.radius() == null) return true;
        return entity.getLocation().distanceSquared(query.center()) <= query.radius() * query.radius();
    }

    private Collection<Entity> candidates(Query query) {
        if (query.radius() == null) return query.world().getEntities();
        double radius = query.radius();
        Collection<Entity> nearby = query.world().getNearbyEntities(query.center(), radius, radius, radius);
        double squared = radius * radius;
        List<Entity> spherical = new ArrayList<>(nearby.size());
        for (Entity entity : nearby) {
            if (entity.getLocation().distanceSquared(query.center()) <= squared) spherical.add(entity);
        }
        return spherical;
    }

    private boolean isProtected(EntitySelector.Selection selection, Entity entity) {
        if (ALWAYS_PROTECTED.contains(entity.getType()) || entity instanceof Player) return true;
        UtilityConfig.KillAllConfig policy = config.get().admin().entityManagement().killall();
        if (BOSSES.contains(entity.getType()) && !selection.explicitlyAllowsBoss()) return true;
        if (policy.protectNamed() && entity.getCustomName() != null) return true;
        if (policy.protectTamed() && entity instanceof Tameable tameable && tameable.isTamed()) return true;
        if (policy.protectVillagers() && entity instanceof AbstractVillager) return true;
        if (policy.protectArmorStands() && entity instanceof ArmorStand) return true;
        if (policy.protectDisplays() && entity instanceof Display) return true;
        return policy.protectPluginMetadata() && looksPluginOwned(entity);
    }

    private static boolean looksPluginOwned(Entity entity) {
        if (entity.hasMetadata("NPC") || entity.hasMetadata("npc") || entity.hasMetadata("CitizensNPC")) return true;
        for (String tag : entity.getScoreboardTags()) {
            String value = tag.toLowerCase(Locale.ROOT);
            if (value.equals("npc") || value.startsWith("npc:") || value.startsWith("plugin:")) return true;
        }
        return false;
    }

    public record Query(EntitySelector.Selection selection, World world, Location center, Double radius) {
        public Query {
            Objects.requireNonNull(selection, "selection");
            Objects.requireNonNull(world, "world");
            if (radius != null) {
                Objects.requireNonNull(center, "center");
                if (center.getWorld() == null || !center.getWorld().equals(world) || radius <= 0.0D) {
                    throw new IllegalArgumentException("radius");
                }
                center = center.clone();
            }
        }

        public String scopeDescription() {
            return radius == null ? "WORLD" : "RADIUS:" + radius.intValue();
        }
    }

    public record Preview(int matched, int protectedCount, int removable) { }

    public record Plan(Query query, Set<UUID> candidateIds, int matched, int protectedCount) {
        public Plan {
            Objects.requireNonNull(query, "query");
            candidateIds = Set.copyOf(Objects.requireNonNull(candidateIds, "candidateIds"));
            if (matched < 0 || protectedCount < 0 || protectedCount > matched) throw new IllegalArgumentException("counts");
        }

        public Preview preview() {
            return new Preview(matched, protectedCount, candidateIds.size());
        }
    }

    private static final class BatchState {
        private final CommandSender actor;
        private final Plan plan;
        private final List<UUID> ids;
        private int index;
        private int removed;
        private int newlyProtected;
        private int missing;

        private BatchState(CommandSender actor, Plan plan, List<UUID> ids) {
            this.actor = actor;
            this.plan = plan;
            this.ids = ids;
        }
    }

    public record Result(int matched, int protectedCount, int removed) { }
}
