package com.zpkdxgames.plexonutility.admin.gui;

/** Pure state model for admin menu action availability. */
public final class AdminMenuModel {
    private AdminMenuModel() {
    }

    public static ActionState action(boolean featureEnabled, boolean permitted, boolean targetOnline) {
        if (!featureEnabled) return ActionState.FEATURE_DISABLED;
        if (!permitted) return ActionState.NO_PERMISSION;
        if (!targetOnline) return ActionState.TARGET_OFFLINE;
        return ActionState.AVAILABLE;
    }

    public static ActionState destructive(boolean featureEnabled, boolean permitted, boolean targetOnline, boolean selfTarget) {
        ActionState base = action(featureEnabled, permitted, targetOnline);
        if (base != ActionState.AVAILABLE) return base;
        return selfTarget ? ActionState.SELF_BLOCKED : ActionState.AVAILABLE;
    }

    public enum ActionState {
        AVAILABLE,
        FEATURE_DISABLED,
        NO_PERMISSION,
        TARGET_OFFLINE,
        SELF_BLOCKED
    }
}
