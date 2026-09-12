package com.zpkdxgames.plexonutility.admin.gui;

import org.junit.jupiter.api.Test;

import static com.zpkdxgames.plexonutility.admin.gui.AdminMenuModel.ActionState.AVAILABLE;
import static com.zpkdxgames.plexonutility.admin.gui.AdminMenuModel.ActionState.FEATURE_DISABLED;
import static com.zpkdxgames.plexonutility.admin.gui.AdminMenuModel.ActionState.NO_PERMISSION;
import static com.zpkdxgames.plexonutility.admin.gui.AdminMenuModel.ActionState.SELF_BLOCKED;
import static com.zpkdxgames.plexonutility.admin.gui.AdminMenuModel.ActionState.TARGET_OFFLINE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminMenuModelTest {
    @Test void modelsFeaturePermissionAndTargetState() {
        assertEquals(FEATURE_DISABLED, AdminMenuModel.action(false, true, true));
        assertEquals(NO_PERMISSION, AdminMenuModel.action(true, false, true));
        assertEquals(TARGET_OFFLINE, AdminMenuModel.action(true, true, false));
        assertEquals(AVAILABLE, AdminMenuModel.action(true, true, true));
    }

    @Test void destructiveSelfActionIsExplicitlyBlocked() {
        assertEquals(SELF_BLOCKED, AdminMenuModel.destructive(true, true, true, true));
        assertEquals(AVAILABLE, AdminMenuModel.destructive(true, true, true, false));
    }
}
