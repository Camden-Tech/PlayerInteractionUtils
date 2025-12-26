package com.baddcamden.playerinteractionutils;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of the plugin's configuration flags and whitelists.
 */
public class ConfigSettings {
    private final boolean blockPlacementTagging;
    private final boolean blockGrowthTagging;
    private final boolean blockTransformTagging;
    private final boolean entitySpawnTagging;
    private final boolean damageTracking;
    private final boolean playerCounters;
    private final boolean chunkPdcEnabled;
    private final boolean entityPdcEnabled;
    private final List<String> entityTagWhitelist;
    private final WhitelistEvaluator entityWhitelist;

    /**
     * Creates a configuration container and builds a {@link WhitelistEvaluator} from the
     * provided whitelist entries.
     */
    public ConfigSettings(
            boolean blockPlacementTagging,
            boolean blockGrowthTagging,
            boolean blockTransformTagging,
            boolean entitySpawnTagging,
            boolean damageTracking,
            boolean playerCounters,
            boolean chunkPdcEnabled,
            boolean entityPdcEnabled,
            List<String> entityTagWhitelist) {
        this(
                blockPlacementTagging,
                blockGrowthTagging,
                blockTransformTagging,
                entitySpawnTagging,
                damageTracking,
                playerCounters,
                chunkPdcEnabled,
                entityPdcEnabled,
                entityTagWhitelist,
                new WhitelistEvaluator(entityTagWhitelist)
        );
    }

    /**
     * Creates a configuration container with an injected {@link WhitelistEvaluator}, which is
     * primarily used for testing to avoid tightly coupling construction with evaluator creation.
     */
    public ConfigSettings(
            boolean blockPlacementTagging,
            boolean blockGrowthTagging,
            boolean blockTransformTagging,
            boolean entitySpawnTagging,
            boolean damageTracking,
            boolean playerCounters,
            boolean chunkPdcEnabled,
            boolean entityPdcEnabled,
            List<String> entityTagWhitelist,
            WhitelistEvaluator entityWhitelist) {
        this.blockPlacementTagging = blockPlacementTagging;
        this.blockGrowthTagging = blockGrowthTagging;
        this.blockTransformTagging = blockTransformTagging;
        this.entitySpawnTagging = entitySpawnTagging;
        this.damageTracking = damageTracking;
        this.playerCounters = playerCounters;
        this.chunkPdcEnabled = chunkPdcEnabled;
        this.entityPdcEnabled = entityPdcEnabled;
        this.entityTagWhitelist = entityTagWhitelist;
        this.entityWhitelist = Objects.requireNonNull(entityWhitelist, "entityWhitelist");
    }

    /**
     * Reads configuration values from the plugin's {@link FileConfiguration} and produces a
     * {@link ConfigSettings} instance.
     *
     * @param configuration configuration source to read from
     * @return hydrated settings reflecting the current config file
     */
    public static ConfigSettings fromConfiguration(FileConfiguration configuration) {
        boolean blockPlacement = configuration.getBoolean("features.block-placement-tagging", true);
        boolean blockGrowth = configuration.getBoolean("features.block-growth-tagging", true);
        boolean blockTransform = configuration.getBoolean("features.block-transform-tagging", true);
        boolean spawnTagging = configuration.getBoolean("features.entity-spawn-tagging", true);
        boolean damage = configuration.getBoolean("features.damage-tracking", true);
        boolean counters = configuration.getBoolean("features.player-counters", true);
        boolean chunkPdc = configuration.getBoolean("storage.chunk-pdc-enabled", true);
        boolean entityPdc = configuration.getBoolean("storage.entity-pdc-enabled", true);
        List<String> whitelist = configuration.getStringList("whitelists.entity-tags");

        return new ConfigSettings(
                blockPlacement,
                blockGrowth,
                blockTransform,
                spawnTagging,
                damage,
                counters,
                chunkPdc,
                entityPdc,
                whitelist
        );
    }

    /**
     * @return whether blocks should be tagged with their placing player
     */
    public boolean blockPlacementTagging() {
        return blockPlacementTagging;
    }

    /**
     * @return whether growth-based block mutations should retain ownership tags
     */
    public boolean blockGrowthTagging() {
        return blockGrowthTagging;
    }

    /**
     * @return whether block transformations should copy ownership metadata
     */
    public boolean blockTransformTagging() {
        return blockTransformTagging;
    }

    /**
     * @return whether entity spawn events should be attributed to players
     */
    public boolean entitySpawnTagging() {
        return entitySpawnTagging;
    }

    /**
     * @return whether per-player damage tracking is enabled
     */
    public boolean damageTracking() {
        return damageTracking;
    }

    /**
     * @return whether player counters should be incremented for actions
     */
    public boolean playerCounters() {
        return playerCounters;
    }

    /**
     * @return configured whitelist entries for entity tags/types
     */
    public List<String> entityTagWhitelist() {
        return entityTagWhitelist;
    }

    /**
     * @return evaluator for checking whether an entity type is allowed by the whitelist
     */
    public WhitelistEvaluator entityWhitelist() {
        return entityWhitelist;
    }

    /**
     * @return whether chunk persistent data containers can be used
     */
    public boolean chunkPdcEnabled() {
        return chunkPdcEnabled;
    }

    /**
     * @return whether entity persistent data containers can be used
     */
    public boolean entityPdcEnabled() {
        return entityPdcEnabled;
    }
}
