package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.DataKeys;
import com.baddcamden.playerinteractionutils.NonPlayerEntityDataManager;
import com.baddcamden.playerinteractionutils.PlayerData;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import com.baddcamden.playerinteractionutils.SpawnContextTracker;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public class EntitySpawnListener implements Listener {
    private final ConfigSettings settings;
    private final DataKeys dataKeys;
    private final NonPlayerEntityDataManager entityDataManager;
    private final PlayerDataManager playerDataManager;
    private final SpawnContextTracker spawnContextTracker;
    private final boolean entityPdcEnabled;

    public EntitySpawnListener(ConfigSettings settings, DataKeys dataKeys, PlayerDataManager playerDataManager, SpawnContextTracker spawnContextTracker, NonPlayerEntityDataManager entityDataManager) {
        this.settings = settings;
        this.dataKeys = dataKeys;
        this.playerDataManager = playerDataManager;
        this.spawnContextTracker = spawnContextTracker;
        this.entityDataManager = entityDataManager;
        this.entityPdcEnabled = settings.entityPdcEnabled();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!settings.entitySpawnTagging()) {
            return;
        }
        spawnContextTracker.recordSpawnEggUse(event);
    }

    @EventHandler
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        if (!settings.entitySpawnTagging()) {
            return;
        }
        spawnContextTracker.recordEggThrow(event);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!settings.entitySpawnTagging()) {
            return;
        }

        Optional<UUID> playerId = spawnContextTracker.findPlayer(event);
        if (playerId.isEmpty()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!settings.entityWhitelist().isAllowed(entity)) {
            return;
        }
        if (entityPdcEnabled) {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            NamespacedKey spawnKey = spawnKeyForReason(event.getSpawnReason());
            if (spawnKey != null) {
                pdc.set(spawnKey, PersistentDataType.STRING, playerId.get().toString());
            }
        } else {
            entityDataManager.get(entity.getUniqueId()).setSpawnOwner(event.getSpawnReason(), playerId.get());
        }

        if (!settings.playerCounters()) {
            return;
        }

        PlayerData.CounterType counterType = counterForReason(event.getSpawnReason());
        if (counterType != null) {
            playerDataManager.get(playerId.get()).increment(counterType);
        }
    }

    private PlayerData.CounterType counterForReason(CreatureSpawnEvent.SpawnReason reason) {
        return switch (reason) {
            case BREEDING -> PlayerData.CounterType.BREEDING_SPAWNS;
            case EGG, CHICKEN_EGG -> PlayerData.CounterType.EGG_SPAWNS;
            case SPAWNER_EGG -> PlayerData.CounterType.SPAWN_EGG_SPAWNS;
            default -> null;
        };
    }

    private NamespacedKey spawnKeyForReason(CreatureSpawnEvent.SpawnReason reason) {
        return switch (reason) {
            case BREEDING -> dataKeys.breedingSpawnPlayer;
            case EGG, CHICKEN_EGG -> dataKeys.eggSpawnPlayer;
            case SPAWNER_EGG -> dataKeys.spawnEggSpawnPlayer;
            default -> null;
        };
    }
}
