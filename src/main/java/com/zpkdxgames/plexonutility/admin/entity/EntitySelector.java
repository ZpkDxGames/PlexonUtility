package com.zpkdxgames.plexonutility.admin.entity;

import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.WaterMob;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Normalizes entity-management selectors without hard-coding a stale living-entity list. */
public final class EntitySelector {
    private static final Set<EntityType> BOSSES = Set.of(EntityType.ENDER_DRAGON, EntityType.WITHER);

    private EntitySelector() { }

    public static Selection parse(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("selector");
        String normalized = normalize(raw);
        Category category = Category.from(normalized);
        if (category != null) return new Selection(normalized.toLowerCase(Locale.ROOT), category, null);
        try {
            EntityType type = EntityType.valueOf(normalized);
            if (type == EntityType.PLAYER || !type.isAlive()) throw new IllegalArgumentException("selector");
            return new Selection(normalized.toLowerCase(Locale.ROOT), null, type);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("selector", exception);
        }
    }

    public static boolean matches(Selection selection, Entity entity) {
        if (entity instanceof Player) return false;
        if (selection.type() != null) return entity.getType() == selection.type();
        return switch (selection.category()) {
            case HOSTILE -> entity instanceof Monster;
            case PASSIVE -> entity instanceof Animals;
            case AMBIENT -> entity instanceof Ambient;
            case AQUATIC -> entity instanceof WaterMob;
            case BOSSES -> BOSSES.contains(entity.getType());
            case MOBS -> entity instanceof Mob;
            case ALL -> true;
        };
    }

    public static List<String> livingNames() {
        List<String> names = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (type != EntityType.PLAYER && type.isAlive()) names.add(type.name().toLowerCase(Locale.ROOT));
        }
        names.sort(String::compareTo);
        return List.copyOf(names);
    }

    public static String normalize(String input) {
        return input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    public enum Category {
        HOSTILE, PASSIVE, AMBIENT, AQUATIC, BOSSES, MOBS, ALL;

        static Category from(String normalized) {
            return switch (normalized) {
                case "HOSTILE", "MONSTERS" -> HOSTILE;
                case "PASSIVE", "ANIMALS" -> PASSIVE;
                case "AMBIENT" -> AMBIENT;
                case "AQUATIC", "WATER" -> AQUATIC;
                case "BOSSES" -> BOSSES;
                case "MOBS" -> MOBS;
                case "ALL" -> ALL;
                default -> null;
            };
        }
    }

    public record Selection(String canonical, Category category, EntityType type) {
        public boolean explicitlyAllowsBoss() {
            return category == Category.BOSSES || (type != null && BOSSES.contains(type));
        }
    }
}
