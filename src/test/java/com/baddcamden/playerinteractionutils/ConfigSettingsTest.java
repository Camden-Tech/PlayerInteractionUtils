package com.baddcamden.playerinteractionutils;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.UUID;

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

    @Test
    void defaultsNewCountersWhenMissing(@TempDir File tempDir) throws Exception {
        UUID playerId = UUID.randomUUID();
        File file = new File(tempDir, playerId.toString() + ".yml");
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("counters.blocks_placed", 2L);
        configuration.save(file);

        PlayerData loaded = PlayerData.load(playerId, file);

        assertEquals(2L, loaded.counters().get(PlayerData.CounterType.BLOCKS_PLACED));
        assertEquals(0L, loaded.counters().get(PlayerData.CounterType.BLOCK_GROWTH_TAGS));
        assertEquals(0L, loaded.counters().get(PlayerData.CounterType.BLOCK_TRANSFORM_TAGS));
        assertEquals(0L, loaded.counters().get(PlayerData.CounterType.LAST_HIT_UPDATES));
        assertEquals(0L, loaded.counters().get(PlayerData.CounterType.DAMAGE_RECORDS));
    }
}
