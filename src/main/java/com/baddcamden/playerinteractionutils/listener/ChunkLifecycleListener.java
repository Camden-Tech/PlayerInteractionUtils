package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.BlockDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Saves block metadata when a chunk unloads if chunk PDC cannot be relied on.
 */
public class ChunkLifecycleListener implements Listener {
    private final BlockDataManager blockDataManager;
    private final boolean chunkPdcEnabled;

    public ChunkLifecycleListener(BlockDataManager blockDataManager, boolean chunkPdcEnabled) {
        this.blockDataManager = blockDataManager;
        this.chunkPdcEnabled = chunkPdcEnabled;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (chunkPdcEnabled) {
            return;
        }
        blockDataManager.loadChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (chunkPdcEnabled) {
            return;
        }
        blockDataManager.saveChunk(event.getChunk());
    }
}
