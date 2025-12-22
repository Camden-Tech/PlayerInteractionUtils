package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.NonPlayerEntityData;
import com.baddcamden.playerinteractionutils.NonPlayerEntityDataManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NonPlayerEntityLifecycleListenerTest {

    @TempDir
    File tempDir;

    @Mock
    private NonPlayerEntityDataManager entityDataManager;

    @Mock
    private CreatureSpawnEvent spawnEvent;

    @Mock
    private EntityDeathEvent deathEvent;

    @Mock
    private ChunkUnloadEvent unloadEvent;

    @Mock
    private ChunkLoadEvent loadEvent;

    @Mock
    private Chunk chunk;

    @Mock
    private Entity entity;

    @Mock
    private Player player;

    private NonPlayerEntityLifecycleListener listener;
    private UUID entityId;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        listener = new NonPlayerEntityLifecycleListener(entityDataManager, false);
        entityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
    }

    @Test
    void loadsDataOnCreatureSpawnWhenPdcDisabled() {
        when(spawnEvent.getEntity()).thenReturn(entity);
        when(entity.getUniqueId()).thenReturn(entityId);

        listener.onCreatureSpawn(spawnEvent);

        verify(entityDataManager).load(entityId);
    }

    @Test
    void skipsLoadingOnCreatureSpawnWhenPdcEnabled() {
        NonPlayerEntityLifecycleListener pdcListener = new NonPlayerEntityLifecycleListener(entityDataManager, true);
        when(spawnEvent.getEntity()).thenReturn(entity);
        when(entity.getUniqueId()).thenReturn(entityId);

        pdcListener.onCreatureSpawn(spawnEvent);

        verify(entityDataManager, never()).load(any());
    }

    @Test
    void loadsExistingEntitiesOnChunkLoadWhenPdcDisabled() {
        when(loadEvent.getChunk()).thenReturn(chunk);
        when(chunk.getEntities()).thenReturn(new Entity[]{entity, player});
        when(entity.getUniqueId()).thenReturn(entityId);
        when(player.getUniqueId()).thenReturn(playerId);

        listener.onChunkLoad(loadEvent);

        verify(entityDataManager).load(entityId);
        verify(entityDataManager, never()).load(eq(playerId));
    }

    @Test
    void savesAndClearsOnChunkUnload() {
        when(unloadEvent.getChunk()).thenReturn(chunk);
        when(chunk.getEntities()).thenReturn(new Entity[]{entity, player});
        when(entity.getUniqueId()).thenReturn(entityId);
        when(player.getUniqueId()).thenReturn(playerId);

        listener.onChunkUnload(unloadEvent);

        verify(entityDataManager).save(entityId);
        verify(entityDataManager).clear(entityId);
        verify(entityDataManager, never()).save(eq(playerId));
        verify(entityDataManager, never()).clear(eq(playerId));
    }

    @Test
    void savesAndClearsOnEntityDeath() {
        when(deathEvent.getEntity()).thenReturn(entity);
        when(entity.getUniqueId()).thenReturn(entityId);

        listener.onEntityDeath(deathEvent);

        verify(entityDataManager).save(entityId);
        verify(entityDataManager).clear(entityId);
    }

    @Test
    void restoresMetadataAcrossChunkReload() {
        NonPlayerEntityDataManager persistentManager = new NonPlayerEntityDataManager(tempDir, Logger.getLogger("test"));
        NonPlayerEntityLifecycleListener persistentListener = new NonPlayerEntityLifecycleListener(persistentManager, false);
        UUID spawnOwner = UUID.randomUUID();
        UUID hitter = UUID.randomUUID();
        Instant hitTime = Instant.now();

        NonPlayerEntityData data = persistentManager.get(entityId);
        data.setSpawnOwner(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG, spawnOwner);
        data.recordLastHit(hitter, hitTime);
        data.addDamage(hitter, 4.5, hitTime);

        when(unloadEvent.getChunk()).thenReturn(chunk);
        when(chunk.getEntities()).thenReturn(new Entity[]{entity});
        when(entity.getUniqueId()).thenReturn(entityId);

        persistentListener.onChunkUnload(unloadEvent);

        NonPlayerEntityDataManager reloadedManager = new NonPlayerEntityDataManager(tempDir, Logger.getLogger("test"));
        NonPlayerEntityLifecycleListener reloadedListener = new NonPlayerEntityLifecycleListener(reloadedManager, false);

        when(loadEvent.getChunk()).thenReturn(chunk);
        reloadedListener.onChunkLoad(loadEvent);

        NonPlayerEntityData restored = reloadedManager.get(entityId);
        assertEquals(spawnOwner, restored.spawnOwner(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG).orElseThrow());
        assertEquals(hitter, restored.lastHitBy().orElseThrow());
        assertEquals(hitTime.toEpochMilli(), restored.lastHitAt());
        assertEquals(4.5, restored.damageTallies(Instant.now()).get(hitter).totalDamage());
    }
}
