package com.baddcamden.playerinteractionutils;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSettingsTest {

    @Test
    void loadsDefaultsWhenMissing() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        ConfigSettings settings = ConfigSettings.fromConfiguration(configuration);

        assertTrue(settings.blockPlacementTagging());
        assertTrue(settings.blockGrowthTagging());
        assertTrue(settings.blockTransformTagging());
        assertTrue(settings.entitySpawnTagging());
        assertTrue(settings.damageTracking());
        assertTrue(settings.playerCounters());
        assertEquals(List.of(), settings.entityTagWhitelist());
    }

    @Test
    void loadsConfiguredValues() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("features.block-placement-tagging", false);
        configuration.set("features.block-growth-tagging", false);
        configuration.set("features.block-transform-tagging", false);
        configuration.set("features.entity-spawn-tagging", false);
        configuration.set("features.damage-tracking", false);
        configuration.set("features.player-counters", false);
        configuration.set("whitelists.entity-tags", List.of("zombie", "cow"));

        ConfigSettings settings = ConfigSettings.fromConfiguration(configuration);

        assertEquals(false, settings.blockPlacementTagging());
        assertEquals(false, settings.blockGrowthTagging());
        assertEquals(false, settings.blockTransformTagging());
        assertEquals(false, settings.entitySpawnTagging());
        assertEquals(false, settings.damageTracking());
        assertEquals(false, settings.playerCounters());
        assertEquals(List.of("zombie", "cow"), settings.entityTagWhitelist());
    }
}
