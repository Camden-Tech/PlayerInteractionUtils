package com.baddcamden.playerinteractionutils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public class BlockTagStorage {
    private final DataKeys keys;

    public BlockTagStorage(DataKeys keys) {
        this.keys = keys;
    }

    public void setOwner(Block block, UUID ownerId) {
        setBlockValue(block, keys.blockOwner, ownerId.toString());
    }

    public Optional<UUID> getOwner(Block block) {
        return getBlockValue(block, keys.blockOwner).map(UUID::fromString);
    }

    /**
     * Tag a block that is the product of growth from a player-owned source block.
     * The stored value reflects the original owner, not necessarily the actor that caused the growth.
     */
    public void setGrownFromPlayer(Block block, UUID ownerId) {
        setBlockValue(block, keys.blockGrownFromPlayer, ownerId.toString());
    }

    /**
     * Tag a block that is the product of a transformation of a player-owned block.
     * The stored value reflects the original owner, not necessarily the actor that triggered the change.
     */
    public void setTransformedFromPlayer(Block block, UUID ownerId) {
        setBlockValue(block, keys.blockTransformedFromPlayer, ownerId.toString());
    }

    private void setBlockValue(Block block, org.bukkit.NamespacedKey key, String value) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        container.set(compositeKey.key(), PersistentDataType.STRING, value);
    }

    private Optional<String> getBlockValue(Block block, org.bukkit.NamespacedKey key) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        return Optional.ofNullable(container.get(compositeKey.key(), PersistentDataType.STRING));
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
