package com.baddcamden.playerinteractionutils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Centralizes the {@link NamespacedKey} instances used to tag persistent data values for
 * blocks and entities across the plugin.
 */
public class DataKeys {
    public final NamespacedKey blockOwner;
    public final NamespacedKey blockGrownFromPlayer;
    public final NamespacedKey blockTransformedFromPlayer;

    public final NamespacedKey eggSpawnPlayer;
    public final NamespacedKey spawnEggSpawnPlayer;
    public final NamespacedKey breedingSpawnPlayer;
    public final NamespacedKey lastHitBy;
    public final NamespacedKey lastHitAt;
    public final NamespacedKey damageByPlayer;

    /**
     * Creates keys that are namespaced to the provided plugin instance, ensuring they do not
     * collide with data from other plugins.
     *
     * @param plugin plugin used to derive the namespace for each key
     */
    public DataKeys(Plugin plugin) {
        this.blockOwner = new NamespacedKey(plugin, "block-owner");
        this.blockGrownFromPlayer = new NamespacedKey(plugin, "block-grown-from-player");
        this.blockTransformedFromPlayer = new NamespacedKey(plugin, "block-transformed-from-player");
        this.eggSpawnPlayer = new NamespacedKey(plugin, "egg-spawn-player");
        this.spawnEggSpawnPlayer = new NamespacedKey(plugin, "spawn-egg-spawn-player");
        this.breedingSpawnPlayer = new NamespacedKey(plugin, "breeding-spawn-player");
        this.lastHitBy = new NamespacedKey(plugin, "last-hit-by");
        this.lastHitAt = new NamespacedKey(plugin, "last-hit-at");
        this.damageByPlayer = new NamespacedKey(plugin, "damage-by-player");
    }

    /**
     * Creates keys with an explicit namespace, useful for tests where no plugin instance is
     * available.
     *
     * @param namespace namespace string to prefix each key with
     */
    public DataKeys(String namespace) {
        this.blockOwner = new NamespacedKey(namespace, "block-owner");
        this.blockGrownFromPlayer = new NamespacedKey(namespace, "block-grown-from-player");
        this.blockTransformedFromPlayer = new NamespacedKey(namespace, "block-transformed-from-player");
        this.eggSpawnPlayer = new NamespacedKey(namespace, "egg-spawn-player");
        this.spawnEggSpawnPlayer = new NamespacedKey(namespace, "spawn-egg-spawn-player");
        this.breedingSpawnPlayer = new NamespacedKey(namespace, "breeding-spawn-player");
        this.lastHitBy = new NamespacedKey(namespace, "last-hit-by");
        this.lastHitAt = new NamespacedKey(namespace, "last-hit-at");
        this.damageByPlayer = new NamespacedKey(namespace, "damage-by-player");
    }
}
