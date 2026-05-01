package com.baddcamden.playerinteractionutils;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Chunk-scoped disk-backed storage for block metadata when chunk {@code PersistentDataContainer}
 * is not available or persistent.
 */
public class BlockDataManager {
    private final File chunksDirectory;
    private final Logger logger;
    private final Map<Long, Map<UUID, BlockData>> dataByChunk = new ConcurrentHashMap<>();

    public BlockDataManager(File dataFolder, Logger logger) {
        this.chunksDirectory = new File(dataFolder, "chunks");
        this.logger = logger;
        if (!chunksDirectory.exists()) {
            chunksDirectory.mkdirs();
        }
    }

    public BlockData get(Block block) {
        long chunkKey = BlockKey.chunkKey(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        Map<UUID, BlockData> blockMap = dataByChunk.computeIfAbsent(chunkKey, key -> new ConcurrentHashMap<>());
        UUID blockId = BlockKey.of(block);
        return blockMap.computeIfAbsent(blockId, ignored -> new BlockData(block));
    }

    public void loadChunk(Chunk chunk) {
        dataByChunk.put(chunkKey(chunk), loadChunkData(chunk));
    }

    public void saveChunk(Chunk chunk) {
        long chunkKey = chunkKey(chunk);
        persistChunk(chunk, dataByChunk.getOrDefault(chunkKey, Map.of()));
        dataByChunk.remove(chunkKey);
    }

    public void unloadChunk(Chunk chunk) {
        dataByChunk.remove(chunkKey(chunk));
    }

    public void saveAllTracked() {
        for (World world : org.bukkit.Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                saveChunk(chunk);
            }
        }
    }

    public Set<BlockData> getOwnedBlocksInChunk(Chunk chunk) {
        return new HashSet<>(dataByChunk
                .getOrDefault(chunkKey(chunk), Map.of())
                .values()
                .stream()
                .filter(data -> data.ownerId().isPresent())
                .toList());
    }

    private Map<UUID, BlockData> loadChunkData(Chunk chunk) {
        File file = chunkFile(chunk.getWorld(), chunk.getX(), chunk.getZ());
        if (!file.exists()) {
            return new ConcurrentHashMap<>();
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        Map<UUID, BlockData> loaded = new ConcurrentHashMap<>();
        for (String blockKey : configuration.getConfigurationSection("blocks") != null
                ? configuration.getConfigurationSection("blocks").getKeys(false)
                : Set.<String>of()) {
            String path = "blocks." + blockKey;
            UUID worldId = parseUuid(configuration.getString(path + ".world-id"));
            Integer x = readInt(configuration, path + ".x");
            Integer y = readInt(configuration, path + ".y");
            Integer z = readInt(configuration, path + ".z");
            if (worldId == null || x == null || y == null || z == null) {
                continue;
            }
            BlockData data = new BlockData(worldId, x, y, z);
            data.setOwner(parseUuid(configuration.getString(path + ".owner")));
            data.setGrownFromPlayerId(parseUuid(configuration.getString(path + ".grown-from-player")));
            data.setTransformedFromPlayerId(parseUuid(configuration.getString(path + ".transformed-from-player")));
            if (data.ownerId().isEmpty() && data.grownFromPlayerId().isEmpty() && data.transformedFromPlayerId().isEmpty()) {
                continue;
            }
            loaded.put(data.blockId(), data);
        }
        return loaded;
    }

    private void persistChunk(Chunk chunk, Map<UUID, BlockData> blocks) {
        persistChunk(chunk.getWorld(), chunk.getX(), chunk.getZ(), blocks);
    }

    private void persistChunk(World world, int chunkX, int chunkZ, Map<UUID, BlockData> blocks) {
        File file = chunkFile(world, chunkX, chunkZ);
        Map<UUID, BlockData> onlyTagged = blocks.values().stream()
                .filter(data -> data.ownerId().isPresent() || data.grownFromPlayerId().isPresent() || data.transformedFromPlayerId().isPresent())
                .sorted(Comparator.comparingInt(BlockData::x).thenComparingInt(BlockData::y).thenComparingInt(BlockData::z))
                .collect(java.util.stream.Collectors.toMap(BlockData::blockId, data -> data, (a, b) -> a, java.util.LinkedHashMap::new));

        if (onlyTagged.isEmpty()) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        onlyTagged.values().forEach(data -> {
            String base = "blocks." + data.x() + "," + data.y() + "," + data.z();
            configuration.set(base + ".world-id", data.worldId().toString());
            configuration.set(base + ".x", data.x());
            configuration.set(base + ".y", data.y());
            configuration.set(base + ".z", data.z());
            configuration.set(base + ".owner", data.ownerId().map(UUID::toString).orElse(null));
            configuration.set(base + ".grown-from-player", data.grownFromPlayerId().map(UUID::toString).orElse(null));
            configuration.set(base + ".transformed-from-player", data.transformedFromPlayerId().map(UUID::toString).orElse(null));
        });

        try {
            configuration.save(file);
        } catch (IOException e) {
            logger.warning("Failed to save chunk block data for " + chunkX + "," + chunkZ + ": " + e.getMessage());
        }
    }

    private File chunkFile(World world, int chunkX, int chunkZ) {
        return new File(chunksDirectory, chunkX + "x" + 0 + "y" + chunkZ + "z," + world.getUID() + ".yml");
    }

    private long chunkKey(Chunk chunk) {
        return BlockKey.chunkKey(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    private Integer readInt(YamlConfiguration configuration, String path) {
        return configuration.contains(path) ? configuration.getInt(path) : null;
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
