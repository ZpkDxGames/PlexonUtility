package com.zpkdxgames.plexonutility.menu;

import com.zpkdxgames.plexoncore.integration.IntegrationRegistry.IntegrationState;
import org.junit.jupiter.api.Test;

import static com.zpkdxgames.plexonutility.menu.UtilityMenuModel.Availability.ADMIN_ONLY;
import static com.zpkdxgames.plexonutility.menu.UtilityMenuModel.Availability.AVAILABLE;
import static com.zpkdxgames.plexonutility.menu.UtilityMenuModel.Availability.FEATURE_DISABLED;
import static com.zpkdxgames.plexonutility.menu.UtilityMenuModel.Availability.INTEGRATION_MISSING;
import static com.zpkdxgames.plexonutility.menu.UtilityMenuModel.Availability.NO_PERMISSION;
import static com.zpkdxgames.plexonutility.menu.UtilityMenuModel.Availability.TEMPORARILY_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilityMenuModelTest {
    @Test
    void localFeatureIsAvailableOnlyWhenEnabledAndPermitted() {
        assertEquals(AVAILABLE, UtilityMenuModel.local(true, true));
        assertEquals(NO_PERMISSION, UtilityMenuModel.local(true, false));
    }

    @Test
    void disabledFeatureTakesPrecedenceOverPermission() {
        assertEquals(FEATURE_DISABLED, UtilityMenuModel.local(false, true));
        assertEquals(FEATURE_DISABLED, UtilityMenuModel.local(false, false));
    }

    @Test
    void missingIntegrationIsDistinctFromPermissionFailure() {
        assertEquals(INTEGRATION_MISSING, UtilityMenuModel.integration(IntegrationState.MISSING, true));
        assertEquals(INTEGRATION_MISSING, UtilityMenuModel.integration(IntegrationState.MISSING, false));
        assertEquals(INTEGRATION_MISSING, UtilityMenuModel.integration(null, true));
    }

    @Test
    void degradedIntegrationIsTemporarilyUnavailable() {
        assertEquals(TEMPORARILY_UNAVAILABLE, UtilityMenuModel.integration(IntegrationState.DEGRADED, true));
        assertEquals(TEMPORARILY_UNAVAILABLE, UtilityMenuModel.integration(IntegrationState.INCOMPATIBLE, true));
        assertEquals(TEMPORARILY_UNAVAILABLE, UtilityMenuModel.integration(IntegrationState.FAILED, true));
    }

    @Test
    void readyIntegrationStillHonorsPermission() {
        assertEquals(AVAILABLE, UtilityMenuModel.integration(IntegrationState.READY, true));
        assertEquals(NO_PERMISSION, UtilityMenuModel.integration(IntegrationState.READY, false));
    }

    @Test
    void adminVisibilityHasExplicitAdminOnlyState() {
        assertEquals(AVAILABLE, UtilityMenuModel.admin(true));
        assertEquals(ADMIN_ONLY, UtilityMenuModel.admin(false));
    }
}
