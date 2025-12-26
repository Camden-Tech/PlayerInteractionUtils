package com.baddcamden.playerinteractionutils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages lifecycle of per-player data, handling creation, loading, caching, and saving to disk.
 */
public class PlayerDataManager {
    private final File playerDirectory;
    private final Logger logger;
    private final Map<UUID, PlayerData> dataByPlayer = new ConcurrentHashMap<>();

    /**
     * @param dataFolder root plugin data folder
     * @param logger     logger used to report load/save issues
     */
    public PlayerDataManager(File dataFolder, Logger logger) {
        this.playerDirectory = new File(dataFolder, "players");
        this.logger = logger;
        if (!playerDirectory.exists()) {
            playerDirectory.mkdirs();
        }
    }

    /**
     * Retrieves cached data for a player, creating a new instance if none has been loaded yet.
     */
    public PlayerData get(UUID playerId) {
        return dataByPlayer.computeIfAbsent(playerId, PlayerData::new);
    }

    /**
     * Loads a player's data from disk into the cache, logging a warning on failure.
     */
    public void load(UUID playerId) {
        File file = playerFile(playerId);
        try {
            PlayerData data = PlayerData.load(playerId, file);
            dataByPlayer.put(playerId, data);
        } catch (IOException e) {
            logger.warning("Failed to load data for " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Persists cached player data to disk if present, logging any failures.
     */
    public void save(UUID playerId) {
        Optional.ofNullable(dataByPlayer.get(playerId)).ifPresent(data -> {
            File file = playerFile(playerId);
            try {
                data.save(file);
            } catch (IOException e) {
                logger.warning("Failed to save data for " + playerId + ": " + e.getMessage());
            }
        });
    }

    /**
     * @return the file path used to store the specified player's data
     */
    private File playerFile(UUID playerId) {
        return new File(playerDirectory, playerId.toString() + ".yml");
    }
}
