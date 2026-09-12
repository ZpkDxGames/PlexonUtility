package com.zpkdxgames.plexonutility.feature;

import java.util.Locale;

public enum Feature {
    MENU,
    FEED,
    HEAL,
    ENDERCHEST,
    WORKBENCH,
    AFK,
    TRASH;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
