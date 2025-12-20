package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.NonPlayerEntityDataManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Ensures non-player entity metadata is loaded when entities enter memory and saved when they leave
 * whenever entity PDC cannot be relied on for persistence.
 */
public class NonPlayerEntityLifecycleListener implements Listener {
    private final NonPlayerEntityDataManager entityDataManager;
    private final boolean entityPdcEnabled;

    public NonPlayerEntityLifecycleListener(NonPlayerEntityDataManager entityDataManager, boolean entityPdcEnabled) {
        this.entityDataManager = entityDataManager;
        this.entityPdcEnabled = entityPdcEnabled;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (entityPdcEnabled) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        entityDataManager.load(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (entityPdcEnabled) {
            return;
        }
        entityDataManager.save(event.getEntity().getUniqueId());
        entityDataManager.clear(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (entityPdcEnabled) {
            return;
        }
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            entityDataManager.save(entity.getUniqueId());
            entityDataManager.clear(entity.getUniqueId());
        }
    }
}
