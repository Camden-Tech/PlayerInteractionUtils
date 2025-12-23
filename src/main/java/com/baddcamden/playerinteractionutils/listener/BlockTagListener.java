package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.BlockTagStorage;
import com.baddcamden.playerinteractionutils.BlockDataManager;
import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.PlayerData;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class BlockTagListener implements Listener {
    private final ConfigSettings settings;
    private final BlockTagStorage blockTags;
    private final PlayerDataManager playerDataManager;
    private final BlockDataManager blockDataManager;

    public BlockTagListener(ConfigSettings settings, BlockTagStorage blockTags, PlayerDataManager playerDataManager, BlockDataManager blockDataManager) {
        this.settings = settings;
        this.blockTags = blockTags;
        this.playerDataManager = playerDataManager;
        this.blockDataManager = blockDataManager;
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        if (!settings.blockPlacementTagging()) {
            return;
        }
        Player player = event.getPlayer();
        blockTags.setOwner(event.getBlockPlaced(), player.getUniqueId());
        if (settings.playerCounters()) {
            playerDataManager.get(player.getUniqueId()).increment(PlayerData.CounterType.BLOCKS_PLACED);
        }
    }

    @EventHandler
    public void onBlockFertilize(BlockFertilizeEvent event) {
        if (!settings.blockGrowthTagging()) {
            return;
        }
        handleBlockGrowth(event.getBlock(), event.getBlocks());
    }

    @EventHandler
    public void onStructureGrow(BlockGrowEvent event) {
        if (!settings.blockGrowthTagging()) {
            return;
        }
        handleBlockGrowth(event.getLocation().getBlock(), event.getBlocks());
    }

    @EventHandler
    public void onBlockTransformed(EntityChangeBlockEvent event) {
        if (!settings.blockTransformTagging()) {
            return;
        }
        // Only tag transformations when the original block was owned, keeping the ownership chain intact.
        blockTags.getOwner(event.getBlock()).ifPresent(ownerId -> {
            blockTags.setTransformedFromPlayer(event.getBlock(), ownerId);
            if (settings.playerCounters()) {
                playerDataManager.get(ownerId).increment(PlayerData.CounterType.BLOCK_TRANSFORM_TAGS);
            }
        });
    }

    private void handleBlockGrowth(Block sourceBlock, Collection<BlockState> grownStates) {
        blockTags.getOwner(sourceBlock).ifPresent(ownerId -> grownStates.forEach(state -> {
            tagGrowth(state.getBlock(), ownerId);
            incrementGrowthCounter(ownerId);
        }));
    }

    private void incrementGrowthCounter(UUID ownerId) {
        if (settings.playerCounters()) {
            playerDataManager.get(ownerId).increment(PlayerData.CounterType.BLOCK_GROWTH_TAGS);
        }
    }

    /**
     * Apply a growth tag that records the owner of the source block that produced the new block state.
     * The owner tracked here is the placer of the originating block, not the actor causing the growth.
     */
    private void tagGrowth(Block block, UUID ownerId) {
        blockTags.setGrownFromPlayer(block, ownerId);
        if (!settings.chunkPdcEnabled()) {
            // Ensure the block is tracked for chunk unload saves when chunk PDC is disabled.
            blockDataManager.get(block);
        }
    }
}
