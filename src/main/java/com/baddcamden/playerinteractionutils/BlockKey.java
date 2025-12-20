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

    public static UUID of(Block block) {
        Objects.requireNonNull(block, "block");
        return of(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static UUID of(UUID worldId, int x, int y, int z) {
        Objects.requireNonNull(worldId, "worldId");
        long most = worldId.getMostSignificantBits() ^ (((long) x) << 32) ^ y;
        long least = worldId.getLeastSignificantBits() ^ (((long) z) << 32);
        return new UUID(most, least);
    }

    public static long chunkKey(World world, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world");
        long most = world.getUID().getMostSignificantBits() ^ ((long) chunkX << 32);
        long least = world.getUID().getLeastSignificantBits() ^ chunkZ;
        return most ^ least;
    }
}
