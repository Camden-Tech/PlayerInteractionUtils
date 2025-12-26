package com.baddcamden.playerinteractionutils;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;
import java.util.UUID;

/**
 * Deterministic unique identifier for a block location used for disk-backed storage.
 */
public final class BlockKey {
    private BlockKey() {
    }

    /**
     * Builds a deterministic UUID representing a specific block location within a world.
     *
     * @param block block to identify
     * @return unique ID derived from the world UUID and block coordinates
     */
    public static UUID of(Block block) {
        Objects.requireNonNull(block, "block");
        return of(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    /**
     * Builds a deterministic UUID representing a block at the provided world and coordinates.
     *
     * @param worldId world identifier containing the block
     * @param x       block x coordinate
     * @param y       block y coordinate
     * @param z       block z coordinate
     * @return unique ID derived from the inputs
     */
    public static UUID of(UUID worldId, int x, int y, int z) {
        Objects.requireNonNull(worldId, "worldId");
        long most = worldId.getMostSignificantBits() ^ (((long) x) << 32) ^ y;
        long least = worldId.getLeastSignificantBits() ^ (((long) z) << 32);
        return new UUID(most, least);
    }

    /**
     * Produces a long key that identifies a chunk for disk-backed block storage bookkeeping.
     *
     * @param world  world containing the chunk
     * @param chunkX chunk x coordinate
     * @param chunkZ chunk z coordinate
     * @return hashed key combining world ID and chunk coordinates
     */
    public static long chunkKey(World world, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world");
        long most = world.getUID().getMostSignificantBits() ^ ((long) chunkX << 32);
        long least = world.getUID().getLeastSignificantBits() ^ chunkZ;
        return most ^ least;
    }
}
