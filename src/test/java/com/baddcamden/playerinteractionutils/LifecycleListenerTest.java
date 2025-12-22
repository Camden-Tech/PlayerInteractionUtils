package com.baddcamden.playerinteractionutils;

import com.baddcamden.playerinteractionutils.listener.ChunkLifecycleListener;
import com.baddcamden.playerinteractionutils.listener.NonPlayerEntityLifecycleListener;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LifecycleListenerTest {

    @Mock
    BlockDataManager blockDataManager;

    @Mock
    NonPlayerEntityDataManager entityDataManager;

    @Mock
    Chunk chunk;

    @Mock
    Entity entity;

    @Mock
    Player player;

    @Test
    void chunkLifecycleUsesFallbackWhenChunkPdcDisabled() {
        ChunkLifecycleListener listener = new ChunkLifecycleListener(blockDataManager, false);
        ChunkLoadEvent loadEvent = mock(ChunkLoadEvent.class);
        when(loadEvent.getChunk()).thenReturn(chunk);

        listener.onChunkLoad(loadEvent);
        verify(blockDataManager).loadChunk(chunk);

        ChunkUnloadEvent unloadEvent = mock(ChunkUnloadEvent.class);
        when(unloadEvent.getChunk()).thenReturn(chunk);

        listener.onChunkUnload(unloadEvent);
        verify(blockDataManager).saveChunk(chunk);
    }

    @Test
    void chunkLifecycleSkipsFallbackWhenChunkPdcEnabled() {
        ChunkLifecycleListener listener = new ChunkLifecycleListener(blockDataManager, true);
        ChunkLoadEvent loadEvent = mock(ChunkLoadEvent.class);
        ChunkUnloadEvent unloadEvent = mock(ChunkUnloadEvent.class);

        listener.onChunkLoad(loadEvent);
        listener.onChunkUnload(unloadEvent);

        verifyNoInteractions(blockDataManager);
    }

    @Test
    void entityLifecycleUsesFallbackWhenEntityPdcDisabled() {
        NonPlayerEntityLifecycleListener listener = new NonPlayerEntityLifecycleListener(entityDataManager, false);
        UUID entityId = UUID.randomUUID();
        when(entity.getUniqueId()).thenReturn(entityId);
        when(chunk.getEntities()).thenReturn(new Entity[]{entity, player});

        ChunkLoadEvent loadEvent = mock(ChunkLoadEvent.class);
        when(loadEvent.getChunk()).thenReturn(chunk);
        listener.onChunkLoad(loadEvent);
        verify(entityDataManager).load(entityId);

        CreatureSpawnEvent spawnEvent = mock(CreatureSpawnEvent.class);
        when(spawnEvent.getEntity()).thenReturn(entity);
        listener.onCreatureSpawn(spawnEvent);
        verify(entityDataManager, times(2)).load(entityId);

        EntityDeathEvent deathEvent = mock(EntityDeathEvent.class);
        when(deathEvent.getEntity()).thenReturn(entity);
        listener.onEntityDeath(deathEvent);
        verify(entityDataManager).save(entityId);
        verify(entityDataManager).clear(entityId);

        ChunkUnloadEvent unloadEvent = mock(ChunkUnloadEvent.class);
        when(unloadEvent.getChunk()).thenReturn(chunk);
        listener.onChunkUnload(unloadEvent);
        verify(entityDataManager, times(2)).save(entityId);
        verify(entityDataManager, times(2)).clear(entityId);
    }

    @Test
    void entityLifecycleSkipsFallbackWhenEntityPdcEnabled() {
        NonPlayerEntityLifecycleListener listener = new NonPlayerEntityLifecycleListener(entityDataManager, true);
        ChunkLoadEvent loadEvent = mock(ChunkLoadEvent.class);
        ChunkUnloadEvent unloadEvent = mock(ChunkUnloadEvent.class);
        CreatureSpawnEvent spawnEvent = mock(CreatureSpawnEvent.class);
        EntityDeathEvent deathEvent = mock(EntityDeathEvent.class);

        listener.onChunkLoad(loadEvent);
        listener.onChunkUnload(unloadEvent);
        listener.onCreatureSpawn(spawnEvent);
        listener.onEntityDeath(deathEvent);

        verifyNoInteractions(entityDataManager);
    }
}
