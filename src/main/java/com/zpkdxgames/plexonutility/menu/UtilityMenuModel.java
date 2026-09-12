package com.zpkdxgames.plexonutility.menu;

import com.zpkdxgames.plexoncore.integration.IntegrationRegistry.IntegrationState;

/**
 * Pure menu-state rules kept separate from Bukkit rendering so availability precedence stays
 * deterministic and easy to regression-test.
 */
final class UtilityMenuModel {
    enum Availability {
        AVAILABLE,
        NO_PERMISSION,
        FEATURE_DISABLED,
        INTEGRATION_MISSING,
        TEMPORARILY_UNAVAILABLE,
        ADMIN_ONLY
    }

    private UtilityMenuModel() {
    }

    static Availability local(boolean enabled, boolean permitted) {
        if (!enabled) return Availability.FEATURE_DISABLED;
        return permitted ? Availability.AVAILABLE : Availability.NO_PERMISSION;
    }

    static Availability integration(IntegrationState state, boolean permitted) {
        if (state == null || state == IntegrationState.MISSING) return Availability.INTEGRATION_MISSING;
        if (state != IntegrationState.READY) return Availability.TEMPORARILY_UNAVAILABLE;
        return permitted ? Availability.AVAILABLE : Availability.NO_PERMISSION;
    }

    static Availability admin(boolean permitted) {
        return permitted ? Availability.AVAILABLE : Availability.ADMIN_ONLY;
    }
}
