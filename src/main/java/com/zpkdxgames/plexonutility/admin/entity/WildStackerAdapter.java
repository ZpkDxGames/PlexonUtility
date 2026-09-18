package com.zpkdxgames.plexonutility.admin.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Optional WildStacker API adapter. No WildStacker classes are linked at compile time.
 *
 * <p>When WildStacker is active, managed living entities are removed through
 * StackedEntity#remove(), which is the API's cache/server removal primitive. If the API is present
 * but cannot be resolved safely, living entities fail closed instead of falling back to
 * Entity#remove().</p>
 */
public final class WildStackerAdapter {
    private final JavaPlugin plugin;
    private volatile Api api;
    private volatile boolean resolutionAttempted;

    public WildStackerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Inspection inspect(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (!(entity instanceof LivingEntity living) || !wildStackerEnabled()) {
            return Inspection.vanilla();
        }

        Api resolved = resolve();
        if (resolved == null) return Inspection.protectedFailure();

        try {
            Object stacked = resolved.getStackedEntity().invoke(null, living);
            if (stacked == null) return Inspection.protectedFailure();
            int amount = ((Number) resolved.getStackAmount().invoke(stacked)).intValue();
            if (amount < 1) return Inspection.protectedFailure();
            return new Inspection(Mode.WILDSTACKER, amount, stacked);
        } catch (InvocationTargetException exception) {
            // WildStacker explicitly rejects living entities it does not manage/consider stackable.
            if (exception.getCause() instanceof IllegalArgumentException) return Inspection.vanilla();
            return Inspection.protectedFailure();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Inspection.protectedFailure();
        }
    }

    public Removal remove(Entity entity, Inspection inspection) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(inspection, "inspection");
        if (inspection.mode() == Mode.PROTECT) return new Removal(false, 0, "wildstacker-api-unresolved");
        if (inspection.mode() == Mode.VANILLA) {
            entity.remove();
            return new Removal(true, 1, "vanilla");
        }

        Api resolved = resolve();
        if (resolved == null || inspection.stackedObject() == null) {
            return new Removal(false, 0, "wildstacker-api-unresolved");
        }
        try {
            resolved.remove().invoke(inspection.stackedObject());
            return new Removal(true, inspection.logicalAmount(), "wildstacker");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return new Removal(false, 0, "wildstacker-remove-failed");
        }
    }

    public boolean active() {
        return wildStackerEnabled();
    }

    private boolean wildStackerEnabled() {
        if (plugin == null || plugin.getServer() == null) return false;
        Plugin wildStacker = plugin.getServer().getPluginManager().getPlugin("WildStacker");
        return wildStacker != null && wildStacker.isEnabled();
    }

    private Api resolve() {
        if (!wildStackerEnabled()) return null;
        Api current = api;
        if (current != null) return current;
        if (resolutionAttempted) return null;
        synchronized (this) {
            if (api != null) return api;
            if (resolutionAttempted) return null;
            resolutionAttempted = true;
            try {
                ClassLoader loader = plugin.getServer().getPluginManager().getPlugin("WildStacker").getClass().getClassLoader();
                Class<?> apiClass = Class.forName("com.bgsoftware.wildstacker.api.WildStackerAPI", true, loader);
                Class<?> stackedEntityClass = Class.forName(
                        "com.bgsoftware.wildstacker.api.objects.StackedEntity", true, loader);
                Method getStackedEntity = apiClass.getMethod("getStackedEntity", LivingEntity.class);
                Method getStackAmount = stackedEntityClass.getMethod("getStackAmount");
                Method remove = stackedEntityClass.getMethod("remove");
                api = new Api(getStackedEntity, getStackAmount, remove);
                return api;
            } catch (ReflectiveOperationException | LinkageError exception) {
                return null;
            }
        }
    }

    public enum Mode {
        VANILLA,
        WILDSTACKER,
        PROTECT
    }

    public record Inspection(Mode mode, int logicalAmount, Object stackedObject) {
        public Inspection {
            Objects.requireNonNull(mode, "mode");
            if (logicalAmount < 0) throw new IllegalArgumentException("logicalAmount");
        }
        static Inspection vanilla() { return new Inspection(Mode.VANILLA, 1, null); }
        static Inspection protectedFailure() { return new Inspection(Mode.PROTECT, 0, null); }
    }

    public record Removal(boolean removed, int logicalAmount, String mode) { }

    private record Api(Method getStackedEntity, Method getStackAmount, Method remove) { }
}
