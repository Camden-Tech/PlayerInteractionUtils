package com.baddcamden.playerinteractionutils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlayerDataManager {
    private final File playerDirectory;
    private final Logger logger;
    private final Map<UUID, PlayerData> dataByPlayer = new ConcurrentHashMap<>();

    public PlayerDataManager(File dataFolder, Logger logger) {
        this.playerDirectory = new File(dataFolder, "players");
        this.logger = logger;
        if (!playerDirectory.exists()) {
            playerDirectory.mkdirs();
        }
    }

    public PlayerData get(UUID playerId) {
        return dataByPlayer.computeIfAbsent(playerId, PlayerData::new);
    }

    public void load(UUID playerId) {
        File file = playerFile(playerId);
        try {
            PlayerData data = PlayerData.load(playerId, file);
            dataByPlayer.put(playerId, data);
        } catch (IOException e) {
            logger.warning("Failed to load data for " + playerId + ": " + e.getMessage());
        }
    }

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

    public void saveAll(Collection<UUID> players) {
        players.forEach(this::save);
    }

    private File playerFile(UUID playerId) {
        return new File(playerDirectory, playerId.toString() + ".yml");
    }
}
