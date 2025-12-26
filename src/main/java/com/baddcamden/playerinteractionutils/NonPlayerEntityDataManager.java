package com.baddcamden.playerinteractionutils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Disk-backed storage for non-player entities when {@code PersistentDataContainer} values are not reliably persisted
 * between loads. Data is loaded when an entity enters memory (chunk load/spawn) and saved when it is removed
 * from memory (chunk unload/server shutdown).
 */
public class NonPlayerEntityDataManager {
    private final File entityDirectory;
    private final Logger logger;
    private final Map<UUID, NonPlayerEntityData> dataByEntity = new ConcurrentHashMap<>();

    /**
     * @param dataFolder plugin data folder
     * @param logger     logger used to report storage issues
     */
    public NonPlayerEntityDataManager(File dataFolder, Logger logger) {
        this.entityDirectory = new File(dataFolder, "entities");
        this.logger = logger;
        if (!entityDirectory.exists()) {
            entityDirectory.mkdirs();
        }
    }

    /**
     * Retrieves cached data for the entity or loads it from disk if absent.
     */
    public NonPlayerEntityData get(UUID entityId) {
        return dataByEntity.computeIfAbsent(entityId, this::loadOrCreate);
    }

    /**
     * Loads entity data from disk into the cache, logging on failure.
     */
    public void load(UUID entityId) {
        File file = entityFile(entityId);
        try {
            NonPlayerEntityData data = NonPlayerEntityData.load(entityId, file);
            dataByEntity.put(entityId, data);
        } catch (IOException e) {
            logger.warning("Failed to load non-player entity data for " + entityId + ": " + e.getMessage());
        }
    }

    /**
     * Saves cached data for a specific entity if present.
     */
    public void save(UUID entityId) {
        Optional.ofNullable(dataByEntity.get(entityId)).ifPresent(data -> {
            File file = entityFile(entityId);
            try {
                data.save(file);
            } catch (IOException e) {
                logger.warning("Failed to save non-player entity data for " + entityId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Saves data for the provided collection of entity IDs.
     */
    public void saveAll(Collection<UUID> entities) {
        entities.forEach(this::save);
    }

    /**
     * Saves all tracked entities currently in memory.
     */
    public void saveAllTracked() {
        dataByEntity.keySet().forEach(this::save);
    }

    /**
     * Clears cached data for an entity when it is no longer needed in memory.
     */
    public void clear(UUID entityId) {
        dataByEntity.remove(entityId);
    }

    /**
     * @return file location for the given entity's data
     */
    private File entityFile(UUID entityId) {
        return new File(entityDirectory, entityId.toString() + ".yml");
    }

    /**
     * Attempts to load existing data for an entity, creating a new container if none exists or
     * loading fails.
     */
    private NonPlayerEntityData loadOrCreate(UUID entityId) {
        File file = entityFile(entityId);
        if (file.exists()) {
            try {
                return NonPlayerEntityData.load(entityId, file);
            } catch (IOException e) {
                logger.warning("Failed to load non-player entity data for " + entityId + ": " + e.getMessage());
            }
        }
        return new NonPlayerEntityData(entityId);
    }
}
