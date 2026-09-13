package com.zpkdxgames.plexonutility.api;

import com.zpkdxgames.plexonutility.feature.Feature;

import java.util.Set;
import java.util.UUID;

public interface PlexonUtilityAPI {
    String version();
    boolean isEnabled(Feature feature);
    Set<Feature> enabledFeatures();

    /** Returns the current ephemeral AFK state for an online/tracked player. */
    default boolean isAfk(UUID playerId) { return false; }

    /** Returns the current native PlexonUtility vanish state when the admin toolkit is active. */
    default boolean isVanished(UUID playerId) { return false; }

    /** Returns the current restart-ephemeral god-mode state for an online/tracked player. */
    default boolean isGodMode(UUID playerId) { return false; }
}
