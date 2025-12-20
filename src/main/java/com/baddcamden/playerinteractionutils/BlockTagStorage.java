package com.baddcamden.playerinteractionutils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class BlockTagStorage {
    private final DataKeys keys;

    public BlockTagStorage(DataKeys keys) {
        this.keys = keys;
    }

    public void setOwner(Block block, UUID ownerId) {
        setBlockValue(block, keys.blockOwner, ownerId.toString());
    }

    public void setGrownBy(Block block, UUID growerId) {
        setBlockValue(block, keys.blockGrownBy, growerId.toString());
    }

    public void setTransformedBy(Block block, UUID playerId) {
        setBlockValue(block, keys.blockTransformedBy, playerId.toString());
    }

    private void setBlockValue(Block block, org.bukkit.NamespacedKey key, String value) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        container.set(compositeKey.key(), PersistentDataType.STRING, value);
    }

    private record NamespacedKeyWithLocation(org.bukkit.NamespacedKey key) {
        static NamespacedKeyWithLocation of(org.bukkit.NamespacedKey root, Location location) {
            String suffix = location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
            return new NamespacedKeyWithLocation(new org.bukkit.NamespacedKey(root.getNamespace(), root.getKey() + "_" + suffix));
        }

        org.bukkit.NamespacedKey key() {
            return key;
        }
    }
}
