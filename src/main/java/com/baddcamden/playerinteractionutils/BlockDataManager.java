package com.baddcamden.playerinteractionutils;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

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
    private final File chunkDirectory;
    private final Logger logger;
    private final Map<UUID, BlockData> dataByBlock = new ConcurrentHashMap<>();
    private final Map<Long, Set<UUID>> blockIdsByChunk = new ConcurrentHashMap<>();

    /**
     * @param dataFolder plugin data folder
     * @param logger     logger used to report load/save issues
     */
    public BlockDataManager(File dataFolder, Logger logger) {
        this.blockDirectory = new File(dataFolder, "blocks");
        this.chunkDirectory = new File(dataFolder, "chunk-blocks");
        this.logger = logger;
        if (!blockDirectory.exists()) {
            blockDirectory.mkdirs();
        }
        if (!chunkDirectory.exists()) {
            chunkDirectory.mkdirs();
        }
    }

    /**
     * Retrieves or creates block data for the given block, tracking it under its chunk for later persistence.
     */
    public BlockData get(Block block) {
        UUID blockId = BlockKey.of(block);
        long chunkKey = BlockKey.chunkKey(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        trackBlock(chunkKey, blockId);
        return dataByBlock.computeIfAbsent(blockId, id -> loadOrCreate(block, id));
    }

    /**
     * Loads block data from disk for a specific block and caches it, registering the block under its chunk.
     */
    public void load(Block block) {
        UUID blockId = BlockKey.of(block);
        long chunkKey = BlockKey.chunkKey(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        trackBlock(chunkKey, blockId);
        File file = blockFile(blockId);
        try {
            BlockData data = BlockData.load(block, file);
            dataByBlock.put(blockId, data);
        } catch (IOException e) {
            logger.warning("Failed to load block data for " + blockId + ": " + e.getMessage());
        }
    }

    /**
     * Loads all tracked block data for a chunk when it is loaded into memory.
     */
    public void loadChunk(Chunk chunk) {
        long chunkKey = BlockKey.chunkKey(chunk.getWorld(), chunk.getX(), chunk.getZ());
        Set<UUID> trackedBlocks = blockIdsByChunk.computeIfAbsent(chunkKey, key -> ConcurrentHashMap.newKeySet());
        loadChunkMapping(chunkKey).forEach(blockId -> {
            trackedBlocks.add(blockId);
            File file = blockFile(blockId);
            try {
                Optional<BlockData> data = BlockData.load(file);
                data.filter(blockData -> belongsToChunk(chunk, blockData)).ifPresent(blockData -> {
                    dataByBlock.put(blockId, blockData);
                });
            } catch (IOException e) {
                logger.warning("Failed to load block data for " + blockId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Persists block data for the specified block, if present.
     */
    public void save(Block block) {
        save(BlockKey.of(block));
    }

    /**
     * Saves all block data tracked for a chunk and writes the chunk-to-block mapping to disk.
     */
    public void saveChunk(Chunk chunk) {
        long key = BlockKey.chunkKey(chunk.getWorld(), chunk.getX(), chunk.getZ());
        Set<UUID> blocks = blockIdsByChunk.getOrDefault(key, Set.of());
        blocks.forEach(this::save);
        persistChunkMapping(key, blocks);
        blockIdsByChunk.remove(key);
    }

    /**
     * Saves all blocks currently cached and persists outstanding chunk mappings.
     */
    public void saveAllTracked() {
        dataByBlock.keySet().forEach(this::save);
        blockIdsByChunk.forEach(this::persistChunkMapping);
    }

    /**
     * @return file location for the given block ID
     */
    private File blockFile(UUID blockId) {
        return new File(blockDirectory, blockId.toString() + ".yml");
    }

    /**
     * @return file location for the chunk mapping referenced by the chunk key
     */
    private File chunkFile(long chunkKey) {
        return new File(chunkDirectory, chunkKey + ".yml");
    }

    /**
     * Loads block data if it exists on disk or constructs a new instance for the given block.
     */
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

    /**
     * Saves cached data for a block ID if available.
     */
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

    /**
     * Persists the mapping from a chunk to its tracked block IDs or deletes it if empty.
     */
    private void persistChunkMapping(long chunkKey, Set<UUID> blockIds) {
        File file = chunkFile(chunkKey);
        if (blockIds.isEmpty()) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.set("blocks", blockIds.stream().map(UUID::toString).toList());
            configuration.save(file);
        } catch (IOException e) {
            logger.warning("Failed to save chunk block mapping for " + chunkKey + ": " + e.getMessage());
        }
    }

    /**
     * Reads the list of block IDs tracked for a given chunk key.
     */
    private Set<UUID> loadChunkMapping(long chunkKey) {
        File file = chunkFile(chunkKey);
        if (!file.exists()) {
            return Set.of();
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        return configuration.getStringList("blocks").stream()
                .map(this::parseUuid)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Safely parses a UUID from a string, warning on invalid values.
     */
    private Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            logger.warning("Encountered invalid block UUID in chunk mapping: " + raw);
            return Optional.empty();
        }
    }

    /**
     * Verifies that a {@link BlockData} instance belongs to the provided chunk coordinates.
     */
    private boolean belongsToChunk(Chunk chunk, BlockData data) {
        int chunkX = Math.floorDiv(data.x(), 16);
        int chunkZ = Math.floorDiv(data.z(), 16);
        return chunk.getWorld().getUID().equals(data.worldId()) && chunk.getX() == chunkX && chunk.getZ() == chunkZ;
    }

    /**
     * Tracks a block ID against its chunk so it can be saved alongside the chunk lifecycle.
     */
    private void trackBlock(long chunkKey, UUID blockId) {
        blockIdsByChunk.computeIfAbsent(chunkKey, key -> ConcurrentHashMap.newKeySet()).add(blockId);
    }
}
