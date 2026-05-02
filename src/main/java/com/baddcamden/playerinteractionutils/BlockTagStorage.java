package com.baddcamden.playerinteractionutils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BlockTagStorage {
    private final JavaPlugin plugin;
    private final DataKeys keys;
    private final boolean chunkPdcEnabled;
    private final BlockDataManager blockDataManager;

    public BlockTagStorage(JavaPlugin plugin, DataKeys keys, boolean chunkPdcEnabled, BlockDataManager blockDataManager) {
        this.plugin = plugin;
        this.keys = keys;
        this.chunkPdcEnabled = chunkPdcEnabled;
        this.blockDataManager = blockDataManager;
    }

    public void setOwner(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockOwner, ownerId != null ? ownerId.toString() : null);
        }
        if (blockDataManager != null) {
            blockDataManager.get(block).setOwner(ownerId);
        }
    }

    public Optional<UUID> getOwner(Block block) {
        verifyAndCorrectBlock(block);
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockOwner).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).ownerId() : Optional.empty();
    }

    public Optional<UUID> getGrownFromPlayer(Block block) {
        verifyAndCorrectBlock(block);
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockGrownFromPlayer).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).grownFromPlayerId() : Optional.empty();
    }

    public Optional<UUID> getTransformedFromPlayer(Block block) {
        verifyAndCorrectBlock(block);
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockTransformedFromPlayer).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).transformedFromPlayerId() : Optional.empty();
    }

    public void setGrownFromPlayer(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockGrownFromPlayer, ownerId != null ? ownerId.toString() : null);
        }
        if (blockDataManager != null) {
            blockDataManager.get(block).setGrownFromPlayerId(ownerId);
        }
    }

    public void setTransformedFromPlayer(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockTransformedFromPlayer, ownerId != null ? ownerId.toString() : null);
        }
        if (blockDataManager != null) {
            blockDataManager.get(block).setTransformedFromPlayerId(ownerId);
        }
    }

    public void clearTags(Block block) {
        setOwner(block, null);
        setGrownFromPlayer(block, null);
        setTransformedFromPlayer(block, null);
    }

    public Set<Block> getPlayerPlacedBlocks(Chunk chunk) {
        return chunkPdcEnabled ? getOwnedBlocksFromChunkPdc(chunk) : getOwnedBlocksFromDisk(chunk);
    }

    public Set<Block> getPlayerPlacedBlocksByManualScan(BoundingBox bounds, World world) {
        Set<Block> matches = new HashSet<>();
        int minX = (int) Math.floor(bounds.getMinX());
        int maxX = (int) Math.floor(bounds.getMaxX());
        int minY = (int) Math.floor(bounds.getMinY());
        int maxY = (int) Math.floor(bounds.getMaxY());
        int minZ = (int) Math.floor(bounds.getMinZ());
        int maxZ = (int) Math.floor(bounds.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (rawOwner(block).isPresent()) {
                        matches.add(block);
                        verifyAndCorrectBlock(block);
                    }
                }
            }
        }
        return matches;
    }

    public Set<Block> getPlayerPlacedBlocks(World world, int chunkX, int chunkZ) { return getPlayerPlacedBlocks(world.getChunkAt(chunkX, chunkZ)); }
    public Set<Block> getPlayerPlacedBlocks(Location center, double radius) { double r=Math.max(0,radius); return getPlayerPlacedBlocks(BoundingBox.of(center.clone().add(-r,-r,-r),center.clone().add(r,r,r)),center,r*r); }
    public Set<Block> getPlayerPlacedBlocks(Location center, double radiusX, double radiusY, double radiusZ) { if (center.getWorld()==null) return Set.of(); return getPlayerPlacedBlocks(BoundingBox.of(center.clone().add(-radiusX,-radiusY,-radiusZ),center.clone().add(radiusX,radiusY,radiusZ)),center.getWorld()); }
    public Set<Block> getPlayerPlacedBlocks(BoundingBox bounds, World world) { Set<Block> result = collectPlayerPlacedBlocksInBounds(bounds, world); validateBoundsAsync(bounds, world); return result; }
    public Set<Block> getPlayerPlacedBlocks(World world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) { BoundingBox b = new BoundingBox(Math.floor(minX),Math.floor(minY),Math.floor(minZ),Math.floor(maxX)+1,Math.floor(maxY)+1,Math.floor(maxZ)+1); Set<Block> r=collectPlayerPlacedBlocksInBounds(b,world); validateBoundsAsync(b,world); return r; }
    public Set<Block> getPlayerPlacedBlocks(Player player, int minXOffset, int minYOffset, int minZOffset, int maxXOffset, int maxYOffset, int maxZOffset) { Location o=player.getLocation(); if (o.getWorld()==null) return Set.of(); return getPlayerPlacedBlocks(o.getWorld(),o.getX()+minXOffset,o.getY()+minYOffset,o.getZ()+minZOffset,o.getX()+maxXOffset,o.getY()+maxYOffset,o.getZ()+maxZOffset); }
    public Set<Block> getPlayerPlacedBlocks(Player player, int xzRadius, int yRadius) { return getPlayerPlacedBlocks(player,-xzRadius,-yRadius,-xzRadius,xzRadius,yRadius,xzRadius); }
    public Set<Block> getPlayerPlacedBlocks(Player player, double radius) { return getPlayerPlacedBlocks(player.getLocation(),radius); }

    private Optional<UUID> rawOwner(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockOwner).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).ownerId() : Optional.empty();
    }

    private void verifyAndCorrectBlock(Block block) {
        if (chunkPdcEnabled) {
            // source of truth is PDC in this mode; no external config to reconcile
            return;
        }
        if (blockDataManager == null) {
            return;
        }
        BlockData data = blockDataManager.get(block);
        if (block.getType().isAir()) {
            data.setOwner(null);
            data.setGrownFromPlayerId(null);
            data.setTransformedFromPlayerId(null);
        }
    }

    private void validateBoundsAsync(BoundingBox bounds, World world) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Set<Block> hashed = collectPlayerPlacedBlocksInBounds(bounds, world);
                    Set<Block> manual = getPlayerPlacedBlocksByManualScan(bounds, world);
                    hashed.forEach(this::verifyAndCorrectBlock);
                    manual.forEach(this::verifyAndCorrectBlock);
                })
        );
    }

    private void setBlockValue(Block block, org.bukkit.NamespacedKey key, String value) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        if (value == null || value.isBlank()) {
            container.remove(compositeKey.key());
            return;
        }
        container.set(compositeKey.key(), PersistentDataType.STRING, value);
    }

    private Optional<String> getBlockValue(Block block, org.bukkit.NamespacedKey key) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        return Optional.ofNullable(container.get(NamespacedKeyWithLocation.of(key, block.getLocation()).key(), PersistentDataType.STRING));
    }

    private Set<Block> getPlayerPlacedBlocks(BoundingBox bounds, Location radialOrigin, double radialDistanceSquared) {
        World world = radialOrigin.getWorld(); if (world == null) return Set.of();
        Set<Block> bounded = getPlayerPlacedBlocks(bounds, world);
        if (radialDistanceSquared <= 0) return bounded;
        Set<Block> radial = new HashSet<>();
        bounded.forEach(block -> { double dx=block.getX()-radialOrigin.getX(),dy=block.getY()-radialOrigin.getY(),dz=block.getZ()-radialOrigin.getZ(); if (dx*dx+dy*dy+dz*dz<=radialDistanceSquared) radial.add(block); });
        return radial;
    }

    private Set<Block> collectPlayerPlacedBlocksInBounds(BoundingBox bounds, World world) {
        int minChunkX = Math.floorDiv((int) Math.floor(bounds.getMinX()), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(bounds.getMaxX()), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(bounds.getMinZ()), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(bounds.getMaxZ()), 16);
        Set<Block> matches = new HashSet<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                getPlayerPlacedBlocks(world.getChunkAt(chunkX, chunkZ)).stream().filter(block -> bounds.contains(block.getX(), block.getY(), block.getZ())).forEach(matches::add);
            }
        }
        return matches;
    }

    private Set<Block> getOwnedBlocksFromChunkPdc(Chunk chunk) {
        Set<Block> ownedBlocks = new HashSet<>();
        String keyPrefix = keys.blockOwner.getKey() + "_";
        chunk.getPersistentDataContainer().getKeys().forEach(key -> {
            if (!key.getNamespace().equals(keys.blockOwner.getNamespace()) || !key.getKey().startsWith(keyPrefix)) return;
            Optional<Coordinates> coordinates = Coordinates.parse(key.getKey().substring(keyPrefix.length()));
            if (coordinates.isEmpty()) return;
            String rawOwner = chunk.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (rawOwner == null || rawOwner.isBlank()) return;
            ownedBlocks.add(chunk.getWorld().getBlockAt(coordinates.get().x, coordinates.get().y, coordinates.get().z));
        });
        return ownedBlocks;
    }

    private Set<Block> getOwnedBlocksFromDisk(Chunk chunk) {
        if (blockDataManager == null) return Set.of();
        Set<Block> ownedBlocks = new HashSet<>();
        blockDataManager.getOwnedBlocksInChunk(chunk).forEach(data -> {
            World world = Bukkit.getWorld(data.worldId());
            if (world != null) ownedBlocks.add(world.getBlockAt(data.x(), data.y(), data.z()));
        });
        return ownedBlocks;
    }

    private record Coordinates(int x, int y, int z) {
        private static Optional<Coordinates> parse(String raw) {
            String[] split = raw.split("_"); if (split.length != 3) return Optional.empty();
            try { return Optional.of(new Coordinates(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]))); }
            catch (NumberFormatException ignored) { return Optional.empty(); }
        }
    }

    private record NamespacedKeyWithLocation(org.bukkit.NamespacedKey key) {
        static NamespacedKeyWithLocation of(org.bukkit.NamespacedKey root, Location location) {
            return new NamespacedKeyWithLocation(new org.bukkit.NamespacedKey(root.getNamespace(), root.getKey() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ()));
        }
    }
}
