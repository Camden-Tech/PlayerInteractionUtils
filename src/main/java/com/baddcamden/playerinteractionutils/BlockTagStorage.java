package com.baddcamden.playerinteractionutils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Provides read/write helpers for block ownership tags, delegating to chunk PDC when available
 * and falling back to disk-backed storage otherwise.
 */
public class BlockTagStorage {
    private final DataKeys keys;
    private final boolean chunkPdcEnabled;
    private final BlockDataManager blockDataManager;

    /**
     * @param keys             persistent data keys used to store ownership values
     * @param chunkPdcEnabled  whether chunk PDC can be relied on for persistence
     * @param blockDataManager fallback manager for disk-backed storage when PDC is unavailable
     */
    public BlockTagStorage(DataKeys keys, boolean chunkPdcEnabled, BlockDataManager blockDataManager) {
        this.keys = keys;
        this.chunkPdcEnabled = chunkPdcEnabled;
        this.blockDataManager = blockDataManager;
    }

    /**
     * Records the owner of a placed block either in chunk PDC or in the disk-backed manager.
     */
    public void setOwner(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockOwner, ownerId.toString());
        } else if (blockDataManager != null) {
            blockDataManager.get(block).setOwner(ownerId);
        }
    }

    /**
     * Retrieves the stored owner for the given block, if any.
     */
    public Optional<UUID> getOwner(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockOwner).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).ownerId() : Optional.empty();
    }

    /**
     * Retrieves the player who owned the source block that produced this grown block, if recorded.
     */
    public Optional<UUID> getGrownFromPlayer(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockGrownFromPlayer).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).grownFromPlayerId() : Optional.empty();
    }

    /**
     * Retrieves the player who owned the source block that transformed into this block, if recorded.
     */
    public Optional<UUID> getTransformedFromPlayer(Block block) {
        if (chunkPdcEnabled) {
            return getBlockValue(block, keys.blockTransformedFromPlayer).map(UUID::fromString);
        }
        return blockDataManager != null ? blockDataManager.get(block).transformedFromPlayerId() : Optional.empty();
    }

    /**
     * Tag a block that is the product of growth from a player-owned source block.
     * The stored value reflects the original owner, not necessarily the actor that caused the growth.
     */
    public void setGrownFromPlayer(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockGrownFromPlayer, ownerId.toString());
        } else if (blockDataManager != null) {
            blockDataManager.get(block).setGrownFromPlayerId(ownerId);
        }
    }

    /**
     * Tag a block that is the product of a transformation of a player-owned block.
     * The stored value reflects the original owner, not necessarily the actor that triggered the change.
     */
    public void setTransformedFromPlayer(Block block, UUID ownerId) {
        if (chunkPdcEnabled) {
            setBlockValue(block, keys.blockTransformedFromPlayer, ownerId.toString());
        } else if (blockDataManager != null) {
            blockDataManager.get(block).setTransformedFromPlayerId(ownerId);
        }
    }

    /**
     * Returns all player-placed blocks in a specific chunk.
     */
    public Set<Block> getPlayerPlacedBlocks(Chunk chunk) {
        return chunkPdcEnabled ? getOwnedBlocksFromChunkPdc(chunk) : getOwnedBlocksFromDisk(chunk);
    }

    /**
     * Returns all player-placed blocks from a chunk coordinate in the provided world.
     */
    public Set<Block> getPlayerPlacedBlocks(World world, int chunkX, int chunkZ) {
        return getPlayerPlacedBlocks(world.getChunkAt(chunkX, chunkZ));
    }

    /**
     * Returns all player-placed blocks within a spherical radius around a location.
     */
    public Set<Block> getPlayerPlacedBlocks(Location center, double radius) {
        double clampedRadius = Math.max(0, radius);
        Location min = center.clone().add(-clampedRadius, -clampedRadius, -clampedRadius);
        Location max = center.clone().add(clampedRadius, clampedRadius, clampedRadius);
        BoundingBox bounds = BoundingBox.of(min, max);
        return getPlayerPlacedBlocks(bounds, center, clampedRadius * clampedRadius);
    }

    /**
     * Returns all player-placed blocks within independent axis radii around a location.
     */
    public Set<Block> getPlayerPlacedBlocks(Location center, double radiusX, double radiusY, double radiusZ) {
        if (center.getWorld() == null) {
            return Set.of();
        }
        Location min = center.clone().add(-radiusX, -radiusY, -radiusZ);
        Location max = center.clone().add(radiusX, radiusY, radiusZ);
        return getPlayerPlacedBlocks(BoundingBox.of(min, max), center.getWorld());
    }

    /**
     * Returns all player-placed blocks in a bounding box.
     */
    public Set<Block> getPlayerPlacedBlocks(BoundingBox bounds, World world) {
        return collectPlayerPlacedBlocksInBounds(bounds, world);
    }

    /**
     * Returns all player-placed blocks in world-space bounds.
     */
    public Set<Block> getPlayerPlacedBlocks(World world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int blockMinX = (int) Math.floor(Math.min(minX, maxX));
        int blockMinY = (int) Math.floor(Math.min(minY, maxY));
        int blockMinZ = (int) Math.floor(Math.min(minZ, maxZ));
        int blockMaxX = (int) Math.floor(Math.max(minX, maxX));
        int blockMaxY = (int) Math.floor(Math.max(minY, maxY));
        int blockMaxZ = (int) Math.floor(Math.max(minZ, maxZ));
        BoundingBox bounds = new BoundingBox(blockMinX, blockMinY, blockMinZ, blockMaxX + 1, blockMaxY + 1, blockMaxZ + 1);
        return collectPlayerPlacedBlocksInBounds(bounds, world);
    }

    /**
     * Returns all player-placed blocks in a bound relative to the player's current location.
     */
    public Set<Block> getPlayerPlacedBlocks(Player player, int minXOffset, int minYOffset, int minZOffset, int maxXOffset, int maxYOffset, int maxZOffset) {
        Location origin = player.getLocation();
        if (origin.getWorld() == null) {
            return Set.of();
        }
        return getPlayerPlacedBlocks(
                origin.getWorld(),
                origin.getX() + minXOffset,
                origin.getY() + minYOffset,
                origin.getZ() + minZOffset,
                origin.getX() + maxXOffset,
                origin.getY() + maxYOffset,
                origin.getZ() + maxZOffset
        );
    }

    /**
     * Returns all player-placed blocks in a symmetric bound centered on the player.
     */
    public Set<Block> getPlayerPlacedBlocks(Player player, int xzRadius, int yRadius) {
        return getPlayerPlacedBlocks(player, -xzRadius, -yRadius, -xzRadius, xzRadius, yRadius, xzRadius);
    }

    /**
     * Returns all player-placed blocks in a spherical radius around the player.
     */
    public Set<Block> getPlayerPlacedBlocks(Player player, double radius) {
        return getPlayerPlacedBlocks(player.getLocation(), radius);
    }

    /**
     * Writes a string value to the chunk PDC under a composite key that includes block coordinates.
     */
    private void setBlockValue(Block block, org.bukkit.NamespacedKey key, String value) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        container.set(compositeKey.key(), PersistentDataType.STRING, value);
    }

    /**
     * Reads a string value from the chunk PDC using the composite key for the given block.
     */
    private Optional<String> getBlockValue(Block block, org.bukkit.NamespacedKey key) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        NamespacedKeyWithLocation compositeKey = NamespacedKeyWithLocation.of(key, block.getLocation());
        return Optional.ofNullable(container.get(compositeKey.key(), PersistentDataType.STRING));
    }

    private Set<Block> getPlayerPlacedBlocks(BoundingBox bounds, Location radialOrigin, double radialDistanceSquared) {
        World world = radialOrigin.getWorld();
        if (world == null) {
            return Set.of();
        }

        Set<Block> bounded = collectPlayerPlacedBlocksInBounds(bounds, world);
        if (radialDistanceSquared <= 0) {
            return bounded;
        }

        Set<Block> radial = new HashSet<>();
        bounded.forEach(block -> {
            double dx = block.getX() - radialOrigin.getX();
            double dy = block.getY() - radialOrigin.getY();
            double dz = block.getZ() - radialOrigin.getZ();
            if (dx * dx + dy * dy + dz * dz <= radialDistanceSquared) {
                radial.add(block);
            }
        });
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
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                getPlayerPlacedBlocks(chunk).stream()
                        .filter(block -> bounds.contains(block.getX(), block.getY(), block.getZ()))
                        .forEach(matches::add);
            }
        }
        return matches;
    }

    private Set<Block> getOwnedBlocksFromChunkPdc(Chunk chunk) {
        Set<Block> ownedBlocks = new HashSet<>();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        String keyPrefix = keys.blockOwner.getKey() + "_";

        container.getKeys().forEach(key -> {
            if (!key.getNamespace().equals(keys.blockOwner.getNamespace()) || !key.getKey().startsWith(keyPrefix)) {
                return;
            }
            Optional<Coordinates> coordinates = Coordinates.parse(key.getKey().substring(keyPrefix.length()));
            if (coordinates.isEmpty()) {
                return;
            }
            String rawOwner = container.get(key, PersistentDataType.STRING);
            if (rawOwner == null || rawOwner.isBlank()) {
                return;
            }
            ownedBlocks.add(chunk.getWorld().getBlockAt(coordinates.get().x, coordinates.get().y, coordinates.get().z));
        });

        return ownedBlocks;
    }

    private Set<Block> getOwnedBlocksFromDisk(Chunk chunk) {
        if (blockDataManager == null) {
            return Set.of();
        }
        Set<Block> ownedBlocks = new HashSet<>();
        blockDataManager.getOwnedBlocksInChunk(chunk).forEach(data -> {
            World world = Bukkit.getWorld(data.worldId());
            if (world != null) {
                ownedBlocks.add(world.getBlockAt(data.x(), data.y(), data.z()));
            }
        });
        return ownedBlocks;
    }

    private record Coordinates(int x, int y, int z) {
        private static Optional<Coordinates> parse(String raw) {
            String[] split = raw.split("_");
            if (split.length != 3) {
                return Optional.empty();
            }
            try {
                return Optional.of(new Coordinates(
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2])
                ));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
    }

    /**
     * Provides a unique {@link org.bukkit.NamespacedKey} per block coordinate to avoid collisions
     * when storing values in a chunk's persistent data container.
     */
    private record NamespacedKeyWithLocation(org.bukkit.NamespacedKey key) {
        /**
         * Builds a composite key by appending block coordinates to the provided root key.
         */
        static NamespacedKeyWithLocation of(org.bukkit.NamespacedKey root, Location location) {
            String suffix = location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
            return new NamespacedKeyWithLocation(new org.bukkit.NamespacedKey(root.getNamespace(), root.getKey() + "_" + suffix));
        }

        /**
         * @return the fully qualified key used for PDC access
         */
        public org.bukkit.NamespacedKey key() {
            return key;
        }
    }
}
