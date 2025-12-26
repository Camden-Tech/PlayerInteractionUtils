package com.baddcamden.playerinteractionutils;

import com.baddcamden.playerinteractionutils.BlockDataManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides read/write helpers for block ownership tags, delegating to chunk PDC when available
 * and falling back to disk-backed storage otherwise.
 */
public class BlockTagStorage {
    private final DataKeys keys;
    private final boolean chunkPdcEnabled;
    private final BlockDataManager blockDataManager;

    /**
     * @param keys             persistent data keys used to store ownership values
     * @param chunkPdcEnabled  whether chunk PDC can be relied on for persistence
     * @param blockDataManager fallback manager for disk-backed storage when PDC is unavailable
     */
    public BlockTagStorage(DataKeys keys, boolean chunkPdcEnabled, BlockDataManager blockDataManager) {
        this.keys = keys;
        this.chunkPdcEnabled = chunkPdcEnabled;
        this.blockDataManager = blockDataManager;
    }

    /**
     * Records the owner of a placed block either in chunk PDC or in the disk-backed manager.
     */
    public void setOwner(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockOwner, ownerId.toString());
        } else if (blockDataManager != null) {
            blockDataManager.get(block).setOwner(ownerId);
        }
    }

    /**
     * Retrieves the stored owner for the given block, if any.
     */
    public Optional<UUID> getOwner(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockOwner).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).ownerId() : Optional.empty();
    }

    /**
     * Retrieves the player who owned the source block that produced this grown block, if recorded.
     */
    public Optional<UUID> getGrownFromPlayer(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockGrownFromPlayer).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).grownFromPlayerId() : Optional.empty();
    }

    /**
     * Retrieves the player who owned the source block that transformed into this block, if recorded.
     */
    public Optional<UUID> getTransformedFromPlayer(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockTransformedFromPlayer).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).transformedFromPlayerId() : Optional.empty();
    }

    /**
     * Tag a block that is the product of growth from a player-owned source block.
     * The stored value reflects the original owner, not necessarily the actor that caused the growth.
     */
    public void setGrownFromPlayer(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockGrownFromPlayer, ownerId.toString());
        } else if (blockDataManager != null) {
            blockDataManager.get(block).setGrownFromPlayerId(ownerId);
        }
    }

    /**
     * Tag a block that is the product of a transformation of a player-owned block.
     * The stored value reflects the original owner, not necessarily the actor that triggered the change.
     */
    public void setTransformedFromPlayer(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockTransformedFromPlayer, ownerId.toString());
        } else if (blockDataManager != null) {
            blockDataManager.get(block).setTransformedFromPlayerId(ownerId);
        }
    }

    /**
     * Writes a string value to the chunk PDC under a composite key that includes block coordinates.
     */
    private void setBlockValue(Block block, org.bukkit.NamespacedKey key, String value) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        container.set(compositeKey.key(), PersistentDataType.STRING, value);
    }

    /**
     * Reads a string value from the chunk PDC using the composite key for the given block.
     */
    private Optional<String> getBlockValue(Block block, org.bukkit.NamespacedKey key) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        return Optional.ofNullable(container.get(compositeKey.key(), PersistentDataType.STRING));
    }

    /**
     * Provides a unique {@link org.bukkit.NamespacedKey} per block coordinate to avoid collisions
     * when storing values in a chunk's persistent data container.
     */
    private record NamespacedKeyWithLocation(org.bukkit.NamespacedKey key) {
        /**
         * Builds a composite key by appending block coordinates to the provided root key.
         */
        static NamespacedKeyWithLocation of(org.bukkit.NamespacedKey root, Location location) {
            String suffix = location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
            return new NamespacedKeyWithLocation(new org.bukkit.NamespacedKey(root.getNamespace(), root.getKey() + "_" + suffix));
        }

        /**
         * @return the fully qualified key used for PDC access
         */
        public org.bukkit.NamespacedKey key() {
            return key;
        }
    }
}
