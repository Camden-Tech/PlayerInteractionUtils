package com.baddcamden.playerinteractionutils;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Disk-backed storage for block metadata when chunk {@code PersistentDataContainer} is not available or persistent.
 * Data is loaded when a chunk enters memory and saved when it leaves, mirroring the player directory layout.
 */
public class BlockDataManager {
    private final File blockDirectory;
    private final Logger logger;
    private final Map<UUID, BlockData> dataByBlock = new ConcurrentHashMap<>();
    private final Map<Long, Set<UUID>> blockIdsByChunk = new ConcurrentHashMap<>();

    public BlockDataManager(File dataFolder, Logger logger) {
        this.blockDirectory = new File(dataFolder, "blocks");
        this.logger = logger;
        if (!blockDirectory.exists()) {
            blockDirectory.mkdirs();
        }
    }

    public BlockData get(Block block) {
        UUID blockId = BlockKey.of(block);
        long chunkKey = BlockKey.chunkKey(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        blockIdsByChunk.computeIfAbsent(chunkKey, key -> ConcurrentHashMap.newKeySet()).add(blockId);
        return dataByBlock.computeIfAbsent(blockId, id -> loadOrCreate(block, id));
    }

    public void load(Block block) {
        UUID blockId = BlockKey.of(block);
        File file = blockFile(blockId);
        try {
            BlockData data = BlockData.load(block, file);
            dataByBlock.put(blockId, data);
        } catch (IOException e) {
            logger.warning("Failed to load block data for " + blockId + ": " + e.getMessage());
        }
    }

    public void save(Block block) {
        save(BlockKey.of(block));
    }

    public void saveChunk(Chunk chunk) {
        long key = BlockKey.chunkKey(chunk.getWorld(), chunk.getX(), chunk.getZ());
        Set<UUID> blocks = blockIdsByChunk.getOrDefault(key, Set.of());
        blocks.forEach(this::save);
        blockIdsByChunk.remove(key);
    }

    public void saveAllTracked() {
        dataByBlock.keySet().forEach(this::save);
    }

    private File blockFile(UUID blockId) {
        return new File(blockDirectory, blockId.toString() + ".yml");
    }

    private BlockData loadOrCreate(Block block, UUID blockId) {
        File file = blockFile(blockId);
        if (file.exists()) {
            try {
                return BlockData.load(block, file);
            } catch (IOException e) {
                logger.warning("Failed to load block data for " + blockId + ": " + e.getMessage());
            }
        }
        return new BlockData(block);
    }

    private void save(UUID blockId) {
        Optional.ofNullable(dataByBlock.get(blockId)).ifPresent(data -> {
            File file = blockFile(blockId);
            try {
                data.save(file);
            } catch (IOException e) {
                logger.warning("Failed to save block data for " + blockId + ": " + e.getMessage());
            }
        });
    }
}
