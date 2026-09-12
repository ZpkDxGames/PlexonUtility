package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** Join/quit reconciliation only; native vanish intentionally has no polling task. */
public final class VanishListener implements Listener {
    private final Supplier<UtilityConfig> config;
    private final VanishService vanish;

    public VanishListener(Supplier<UtilityConfig> config, VanishService vanish) {
        this.config = Objects.requireNonNull(config, "config");
        this.vanish = Objects.requireNonNull(vanish, "vanish");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        UtilityConfig.AdminConfig admin = config.get().admin();
        if (admin.enabled() && admin.vanish().enabled()
                && admin.vanish().suppressJoinQuit()
                && vanish.isVanished(event.getPlayer().getUniqueId())) {
            event.joinMessage(null);
        }
        vanish.onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        UtilityConfig.AdminConfig admin = config.get().admin();
        if (admin.enabled() && admin.vanish().enabled()
                && admin.vanish().suppressJoinQuit()
                && vanish.isVanished(event.getPlayer().getUniqueId())) {
            event.quitMessage(null);
        }
    }
}
