package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.DataKeys;
import com.baddcamden.playerinteractionutils.PlayerData;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import com.baddcamden.playerinteractionutils.SpawnContextTracker;
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
    private final PlayerDataManager playerDataManager;
    private final SpawnContextTracker spawnContextTracker;

    public EntitySpawnListener(ConfigSettings settings, DataKeys dataKeys, PlayerDataManager playerDataManager, SpawnContextTracker spawnContextTracker) {
        this.settings = settings;
        this.dataKeys = dataKeys;
        this.playerDataManager = playerDataManager;
        this.spawnContextTracker = spawnContextTracker;
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
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(dataKeys.entitySpawnCause, PersistentDataType.STRING, event.getSpawnReason().name());
        pdc.set(dataKeys.entitySpawnPlayer, PersistentDataType.STRING, playerId.get().toString());

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
}
