package com.baddcamden.playerinteractionutils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class DataKeys {
    public final NamespacedKey blockOwner;
    public final NamespacedKey blockGrownBy;
    public final NamespacedKey blockTransformedBy;

    public final NamespacedKey entitySpawnCause;
    public final NamespacedKey entitySpawnPlayer;
    public final NamespacedKey lastHitBy;
    public final NamespacedKey lastHitAt;

    public DataKeys(Plugin plugin) {
        this.blockOwner = new NamespacedKey(plugin, "block-owner");
        this.blockGrownBy = new NamespacedKey(plugin, "block-grown-by");
        this.blockTransformedBy = new NamespacedKey(plugin, "block-transformed-by");
        this.entitySpawnCause = new NamespacedKey(plugin, "spawn-cause");
        this.entitySpawnPlayer = new NamespacedKey(plugin, "spawn-player");
        this.lastHitBy = new NamespacedKey(plugin, "last-hit-by");
        this.lastHitAt = new NamespacedKey(plugin, "last-hit-at");
    }
}
