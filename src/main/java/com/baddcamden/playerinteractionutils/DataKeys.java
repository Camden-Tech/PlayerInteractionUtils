package com.baddcamden.playerinteractionutils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class DataKeys {
    public final NamespacedKey blockOwner;
    public final NamespacedKey blockGrownFromPlayer;
    public final NamespacedKey blockTransformedFromPlayer;

    public final NamespacedKey entitySpawnCause;
    public final NamespacedKey entitySpawnPlayer;
    public final NamespacedKey lastHitBy;
    public final NamespacedKey lastHitAt;
    public final NamespacedKey damageByPlayer;

    public DataKeys(Plugin plugin) {
        this.blockOwner = new NamespacedKey(plugin, "block-owner");
        this.blockGrownFromPlayer = new NamespacedKey(plugin, "block-grown-from-player");
        this.blockTransformedFromPlayer = new NamespacedKey(plugin, "block-transformed-from-player");
        this.entitySpawnCause = new NamespacedKey(plugin, "spawn-cause");
        this.entitySpawnPlayer = new NamespacedKey(plugin, "spawn-player");
        this.lastHitBy = new NamespacedKey(plugin, "last-hit-by");
        this.lastHitAt = new NamespacedKey(plugin, "last-hit-at");
        this.damageByPlayer = new NamespacedKey(plugin, "damage-by-player");
    }
}
