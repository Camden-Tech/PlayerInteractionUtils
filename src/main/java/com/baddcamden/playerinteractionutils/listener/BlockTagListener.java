package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.BlockTagStorage;
import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.PlayerData;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.StructureGrowEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.entity.Player;

public class BlockTagListener implements Listener {
    private final ConfigSettings settings;
    private final BlockTagStorage blockTags;
    private final PlayerDataManager playerDataManager;

    public BlockTagListener(ConfigSettings settings, BlockTagStorage blockTags, PlayerDataManager playerDataManager) {
        this.settings = settings;
        this.blockTags = blockTags;
        this.playerDataManager = playerDataManager;
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
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        event.getBlocks().forEach(state -> tagGrowth(state.getBlock(), player));
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        if (!settings.blockGrowthTagging()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        event.getBlocks().forEach(state -> tagGrowth(state.getBlock(), player));
    }

    @EventHandler
    public void onBlockTransformed(EntityChangeBlockEvent event) {
        if (!settings.blockTransformTagging()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        blockTags.setTransformedBy(event.getBlock(), player.getUniqueId());
    }

    private void tagGrowth(Block block, Player player) {
        blockTags.setGrownBy(block, player.getUniqueId());
    }
}
