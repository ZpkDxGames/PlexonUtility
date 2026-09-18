package com.zpkdxgames.plexonutility.admin.entity;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** One-pass bounded entity cleanup with conservative protection defaults. */
public final class EntityCleanupService {
    private static final Set<EntityType> BOSSES = Set.of(EntityType.ENDER_DRAGON, EntityType.WITHER);
    private static final Set<EntityType> ALWAYS_PROTECTED = Set.of(
            EntityType.PLAYER, EntityType.INTERACTION, EntityType.MARKER);

    private final Supplier<UtilityConfig> config;
    private final AdminAuditService audit;

    public EntityCleanupService(Supplier<UtilityConfig> config, AdminAuditService audit) {
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

    public record Result(int matched, int protectedCount, int removed) { }
}
