package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.DataKeys;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import com.baddcamden.playerinteractionutils.SpawnContextTracker;
import com.baddcamden.playerinteractionutils.WhitelistEvaluator;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitySpawnListenerTest {

    @Mock
    private ConfigSettings settings;

    @Mock
    private PlayerDataManager playerDataManager;

    @Mock
    private SpawnContextTracker spawnContextTracker;

    @Mock
    private CreatureSpawnEvent event;

    @Mock
    private LivingEntity entity;

    @Mock
    private PersistentDataContainer dataContainer;

    @Mock
    private WhitelistEvaluator whitelistEvaluator;

    private EntitySpawnListener listener;
    private DataKeys dataKeys;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        dataKeys = new DataKeys("playerinteractionutils");
        listener = new EntitySpawnListener(settings, dataKeys, playerDataManager, spawnContextTracker);
        playerId = UUID.randomUUID();

        when(settings.entitySpawnTagging()).thenReturn(true);
        when(settings.playerCounters()).thenReturn(false);
        when(settings.entityWhitelist()).thenReturn(whitelistEvaluator);
        when(spawnContextTracker.findPlayer(event)).thenReturn(Optional.of(playerId));
        when(event.getEntity()).thenReturn(entity);
        when(entity.getPersistentDataContainer()).thenReturn(dataContainer);
        when(whitelistEvaluator.isAllowed(entity)).thenReturn(true);
    }

    @Test
    void setsOnlyEggKeyForThrownEggSpawn() {
        when(event.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.EGG);

        listener.onCreatureSpawn(event);

        verify(dataContainer).set(eq(dataKeys.eggSpawnPlayer), eq(PersistentDataType.STRING), eq(playerId.toString()));
        verify(dataContainer, never()).set(eq(dataKeys.spawnEggSpawnPlayer), any(), any());
        verify(dataContainer, never()).set(eq(dataKeys.breedingSpawnPlayer), any(), any());
    }

    @Test
    void setsOnlySpawnEggKeyForSpawnEgg() {
        when(event.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);

        listener.onCreatureSpawn(event);

        verify(dataContainer).set(eq(dataKeys.spawnEggSpawnPlayer), eq(PersistentDataType.STRING), eq(playerId.toString()));
        verify(dataContainer, never()).set(eq(dataKeys.eggSpawnPlayer), any(), any());
        verify(dataContainer, never()).set(eq(dataKeys.breedingSpawnPlayer), any(), any());
    }

    @Test
    void setsOnlyBreedingKeyForBreedingSpawn() {
        when(event.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.BREEDING);

        listener.onCreatureSpawn(event);

        verify(dataContainer).set(eq(dataKeys.breedingSpawnPlayer), eq(PersistentDataType.STRING), eq(playerId.toString()));
        verify(dataContainer, never()).set(eq(dataKeys.spawnEggSpawnPlayer), any(), any());
        verify(dataContainer, never()).set(eq(dataKeys.eggSpawnPlayer), any(), any());
    }

    @Test
    void skipsTaggingWhenNotWhitelisted() {
        when(event.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        when(whitelistEvaluator.isAllowed(entity)).thenReturn(false);

        listener.onCreatureSpawn(event);

        verify(dataContainer, never()).set(any(), any(), any());
    }
}
