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
        int matched = 0;
        int protectedCount = 0;
        for (Entity entity : candidates(query)) {
            if (!EntitySelector.matches(query.selection(), entity)) continue;
            matched++;
            if (isProtected(query.selection(), entity)) protectedCount++;
        }
        return new Preview(matched, protectedCount, Math.max(0, matched - protectedCount));
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
    public record Result(int matched, int protectedCount, int removed) { }
}
