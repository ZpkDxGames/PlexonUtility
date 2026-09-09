package com.zpkdxgames.plexonutility.api;

import com.zpkdxgames.plexonutility.feature.Feature;

import java.util.Set;

public interface PlexonUtilityAPI {
    String version();
    boolean isEnabled(Feature feature);
    Set<Feature> enabledFeatures();
}
