package com.baddcamden.playerinteractionutils;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;

public class ConfigSettings {
    private final boolean blockPlacementTagging;
    private final boolean blockGrowthTagging;
    private final boolean blockTransformTagging;
    private final boolean entitySpawnTagging;
    private final boolean damageTracking;
    private final boolean playerCounters;
    private final List<String> entityTagWhitelist;
    private final WhitelistEvaluator entityWhitelist;

    public ConfigSettings(
            boolean blockPlacementTagging,
            boolean blockGrowthTagging,
            boolean blockTransformTagging,
            boolean entitySpawnTagging,
            boolean damageTracking,
            boolean playerCounters,
            List<String> entityTagWhitelist) {
        this(
                blockPlacementTagging,
                blockGrowthTagging,
                blockTransformTagging,
                entitySpawnTagging,
                damageTracking,
                playerCounters,
                entityTagWhitelist,
                new WhitelistEvaluator(entityTagWhitelist)
        );
    }

    public ConfigSettings(
            boolean blockPlacementTagging,
            boolean blockGrowthTagging,
            boolean blockTransformTagging,
            boolean entitySpawnTagging,
            boolean damageTracking,
            boolean playerCounters,
            List<String> entityTagWhitelist,
            WhitelistEvaluator entityWhitelist) {
        this.blockPlacementTagging = blockPlacementTagging;
        this.blockGrowthTagging = blockGrowthTagging;
        this.blockTransformTagging = blockTransformTagging;
        this.entitySpawnTagging = entitySpawnTagging;
        this.damageTracking = damageTracking;
        this.playerCounters = playerCounters;
        this.entityTagWhitelist = entityTagWhitelist;
        this.entityWhitelist = Objects.requireNonNull(entityWhitelist, "entityWhitelist");
    }

    public static ConfigSettings fromConfiguration(FileConfiguration configuration) {
        boolean blockPlacement = configuration.getBoolean("features.block-placement-tagging", true);
        boolean blockGrowth = configuration.getBoolean("features.block-growth-tagging", true);
        boolean blockTransform = configuration.getBoolean("features.block-transform-tagging", true);
        boolean spawnTagging = configuration.getBoolean("features.entity-spawn-tagging", true);
        boolean damage = configuration.getBoolean("features.damage-tracking", true);
        boolean counters = configuration.getBoolean("features.player-counters", true);
        List<String> whitelist = configuration.getStringList("whitelists.entity-tags");

        return new ConfigSettings(
                blockPlacement,
                blockGrowth,
                blockTransform,
                spawnTagging,
                damage,
                counters,
                whitelist
        );
    }

    public boolean blockPlacementTagging() {
        return blockPlacementTagging;
    }

    public boolean blockGrowthTagging() {
        return blockGrowthTagging;
    }

    public boolean blockTransformTagging() {
        return blockTransformTagging;
    }

    public boolean entitySpawnTagging() {
        return entitySpawnTagging;
    }

    public boolean damageTracking() {
        return damageTracking;
    }

    public boolean playerCounters() {
        return playerCounters;
    }

    public List<String> entityTagWhitelist() {
        return entityTagWhitelist;
    }

    public WhitelistEvaluator entityWhitelist() {
        return entityWhitelist;
    }
}
