package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.BlockDataManager;
import com.baddcamden.playerinteractionutils.BlockTagStorage;
import com.baddcamden.playerinteractionutils.DataKeys;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkLifecycleListenerTest {

    @TempDir
    File tempDir;

    @Mock
    private Chunk chunk;

    @Mock
    private Block block;

    @Mock
    private World world;

    private final DataKeys dataKeys = new DataKeys("playerinteractionutils");
    private BlockDataManager blockDataManager;
    private BlockTagStorage blockTagStorage;
    private ChunkLifecycleListener listener;
    private UUID worldId;

    @BeforeEach
    void setUp() {
        worldId = UUID.randomUUID();
        blockDataManager = new BlockDataManager(tempDir, Logger.getLogger("test"));
        blockTagStorage = new BlockTagStorage(dataKeys, false, blockDataManager);
        listener = new ChunkLifecycleListener(blockDataManager, false);

        when(world.getUID()).thenReturn(worldId);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(2);
        when(chunk.getZ()).thenReturn(3);
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(32);
        when(block.getY()).thenReturn(70);
        when(block.getZ()).thenReturn(48);
        when(block.getChunk()).thenReturn(chunk);
    }

    @Test
    void restoresBlockTagsOnChunkLoadWhenPdcDisabled(@Mock ChunkUnloadEvent unloadEvent, @Mock ChunkLoadEvent loadEvent) {
        UUID ownerId = UUID.randomUUID();
        UUID grownId = UUID.randomUUID();
        UUID transformedId = UUID.randomUUID();

        blockTagStorage.setOwner(block, ownerId);
        blockTagStorage.setGrownFromPlayer(block, grownId);
        blockTagStorage.setTransformedFromPlayer(block, transformedId);

        when(unloadEvent.getChunk()).thenReturn(chunk);
        listener.onChunkUnload(unloadEvent);

        BlockDataManager reloadedManager = new BlockDataManager(tempDir, Logger.getLogger("test"));
        ChunkLifecycleListener reloadedListener = new ChunkLifecycleListener(reloadedManager, false);
        BlockTagStorage reloadedStorage = new BlockTagStorage(dataKeys, false, reloadedManager);

        when(loadEvent.getChunk()).thenReturn(chunk);
        reloadedListener.onChunkLoad(loadEvent);

        assertEquals(ownerId, reloadedStorage.getOwner(block).orElseThrow());
        assertEquals(grownId, reloadedStorage.getGrownFromPlayer(block).orElseThrow());
        assertEquals(transformedId, reloadedStorage.getTransformedFromPlayer(block).orElseThrow());
    }
}
