package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.BlockTagStorage;
import com.baddcamden.playerinteractionutils.BlockDataManager;
import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.PlayerData;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BlockTagListener implements Listener {
    private final ConfigSettings settings;
    private final BlockTagStorage blockTags;
    private final PlayerDataManager playerDataManager;
    private final BlockDataManager blockDataManager;
    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };
    private static final Map<Material, EnumSet<Material>> BELOW_GROWTH_SUPPORT = new EnumMap<>(Material.class);
    private static final Map<Material, EnumSet<Material>> ABOVE_GROWTH_SUPPORT = new EnumMap<>(Material.class);

    static {
        BELOW_GROWTH_SUPPORT.put(Material.SUGAR_CANE, EnumSet.of(Material.SUGAR_CANE));
        BELOW_GROWTH_SUPPORT.put(Material.CACTUS, EnumSet.of(Material.CACTUS));
        BELOW_GROWTH_SUPPORT.put(Material.BAMBOO, EnumSet.of(Material.BAMBOO));
        BELOW_GROWTH_SUPPORT.put(Material.KELP, EnumSet.of(Material.KELP, Material.KELP_PLANT));
        BELOW_GROWTH_SUPPORT.put(Material.KELP_PLANT, EnumSet.of(Material.KELP, Material.KELP_PLANT));
        BELOW_GROWTH_SUPPORT.put(Material.TWISTING_VINES, EnumSet.of(Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT));
        BELOW_GROWTH_SUPPORT.put(Material.TWISTING_VINES_PLANT, EnumSet.of(Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT));
        BELOW_GROWTH_SUPPORT.put(Material.CHORUS_PLANT, EnumSet.of(Material.CHORUS_PLANT, Material.CHORUS_FLOWER));

        ABOVE_GROWTH_SUPPORT.put(Material.WEEPING_VINES, EnumSet.of(Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT));
        ABOVE_GROWTH_SUPPORT.put(Material.WEEPING_VINES_PLANT, EnumSet.of(Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT));
        ABOVE_GROWTH_SUPPORT.put(Material.CAVE_VINES, EnumSet.of(Material.CAVE_VINES, Material.CAVE_VINES_PLANT));
        ABOVE_GROWTH_SUPPORT.put(Material.CAVE_VINES_PLANT, EnumSet.of(Material.CAVE_VINES, Material.CAVE_VINES_PLANT));
    }

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
        handleBlockGrowth(event.getBlock(), List.of(event.getNewState()));
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!settings.blockGrowthTagging()) {
            return;
        }
        handleBlockGrowth(event.getSource(), List.of(event.getNewState()));
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
        grownStates.forEach(state -> resolveGrowthOwner(sourceBlock, state).ifPresent(ownerId -> {
            tagGrowth(state.getBlock(), ownerId);
            incrementGrowthCounter(ownerId);
        }));
    }

    private Optional<UUID> resolveGrowthOwner(Block sourceBlock, BlockState grownState) {
        Optional<UUID> directOwner = blockTags.getOwner(sourceBlock);
        if (directOwner.isPresent()) {
            return directOwner;
        }

        Block grownBlock = grownState.getBlock();
        Optional<UUID> grownBlockOwner = blockTags.getOwner(grownBlock);
        if (grownBlockOwner.isPresent()) {
            return grownBlockOwner;
        }

        return inferGrowthSourceBlock(grownBlock, grownState)
                .flatMap(blockTags::getOwner);
    }

    private Optional<Block> inferGrowthSourceBlock(Block grownBlock, BlockState grownState) {
        if (grownBlock.getType() != Material.AIR) {
            // Growth that ages an existing block keeps the ownership with the same block.
            return Optional.of(grownBlock);
        }

        Material grownType = grownState.getType();
        Block below = grownBlock.getRelative(BlockFace.DOWN);
        if (isSupportedGrowth(below.getType(), BELOW_GROWTH_SUPPORT.get(grownType))) {
            return Optional.of(below);
        }

        Block above = grownBlock.getRelative(BlockFace.UP);
        if (isSupportedGrowth(above.getType(), ABOVE_GROWTH_SUPPORT.get(grownType))) {
            return Optional.of(above);
        }

        for (BlockFace face : HORIZONTAL_FACES) {
            Block neighbor = grownBlock.getRelative(face);
            if (neighbor.getType() == grownType) {
                return Optional.of(neighbor);
            }
        }

        // As a last resort, fall back to the block beneath even if it is a different type.
        if (blockTags.getOwner(below).isPresent()) {
            return Optional.of(below);
        }

        return Optional.empty();
    }

    private boolean isSupportedGrowth(Material candidate, EnumSet<Material> validSources) {
        return validSources != null && validSources.contains(candidate);
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
