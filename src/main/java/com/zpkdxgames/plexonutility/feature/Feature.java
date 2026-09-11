package com.zpkdxgames.plexonutility.feature;

import java.util.Locale;

public enum Feature {
    FEED,
    HEAL,
    ENDERCHEST,
    WORKBENCH,
    AFK;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
