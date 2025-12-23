package com.baddcamden.playerinteractionutils;

import com.baddcamden.playerinteractionutils.BlockDataManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public class BlockTagStorage {
    private final DataKeys keys;
    private final boolean chunkPdcEnabled;
    private final BlockDataManager blockDataManager;

    public BlockTagStorage(DataKeys keys, boolean chunkPdcEnabled, BlockDataManager blockDataManager) {
        this.keys = keys;
        this.chunkPdcEnabled = chunkPdcEnabled;
        this.blockDataManager = blockDataManager;
    }

    public void setOwner(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockOwner, ownerId.toString());
        } else if (blockDataManager != null) {
            blockDataManager.get(block).setOwner(ownerId);
        }
    }

    public Optional<UUID> getOwner(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockOwner).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).ownerId() : Optional.empty();
    }

    public Optional<UUID> getGrownFromPlayer(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockGrownFromPlayer).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).grownFromPlayerId() : Optional.empty();
    }

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

        public org.bukkit.NamespacedKey key() {
            return key;
        }
    }
}
