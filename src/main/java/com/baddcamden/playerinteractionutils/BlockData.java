package com.baddcamden.playerinteractionutils;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal block metadata used when chunk PDC is unavailable. Mirrors the player directory pattern for blocks.
 */
public class BlockData {
    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;
    private UUID ownerId;
    private UUID grownFromPlayerId;
    private UUID transformedFromPlayerId;
    private static final String WORLD_ID_KEY = "world-id";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";

    public BlockData(Block block) {
        this(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public BlockData(UUID worldId, int x, int y, int z) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID blockId() {
        return BlockKey.of(worldId, x, y, z);
    }

    public UUID worldId() {
        return worldId;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Optional<UUID> ownerId() {
        return Optional.ofNullable(ownerId);
    }

    public void setGrownFromPlayerId(UUID ownerId) {
        this.grownFromPlayerId = ownerId;
    }

    public Optional<UUID> grownFromPlayerId() {
        return Optional.ofNullable(grownFromPlayerId);
    }

    public void setTransformedFromPlayerId(UUID ownerId) {
        this.transformedFromPlayerId = ownerId;
    }

    public Optional<UUID> transformedFromPlayerId() {
        return Optional.ofNullable(transformedFromPlayerId);
    }

    public static BlockData load(Block block, File file) throws IOException {
        BlockData data = new BlockData(block);
        if (!file.exists()) {
            return data;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        applyTags(configuration, data);
        return data;
    }

    public static Optional<BlockData> load(File file) throws IOException {
        if (!file.exists()) {
            return Optional.empty();
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        UUID worldId = parseUuid(configuration.getString(WORLD_ID_KEY));
        Integer x = configuration.contains(X_KEY) ? configuration.getInt(X_KEY) : null;
        Integer y = configuration.contains(Y_KEY) ? configuration.getInt(Y_KEY) : null;
        Integer z = configuration.contains(Z_KEY) ? configuration.getInt(Z_KEY) : null;
        if (worldId == null || x == null || y == null || z == null) {
            return Optional.empty();
        }

        BlockData data = new BlockData(worldId, x, y, z);
        applyTags(configuration, data);
        return Optional.of(data);
    }

    public void save(File file) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(WORLD_ID_KEY, worldId.toString());
        configuration.set(X_KEY, x);
        configuration.set(Y_KEY, y);
        configuration.set(Z_KEY, z);
        Map<String, UUID> fields = Map.of(
                "owner", ownerId,
                "grown-from-player", grownFromPlayerId,
                "transformed-from-player", transformedFromPlayerId
        );

        fields.forEach((key, value) -> configuration.set(key, value != null ? value.toString() : null));
        configuration.save(file);
    }

    private static void applyTags(YamlConfiguration configuration, BlockData data) {
        data.ownerId = parseUuid(configuration.getString("owner"));
        data.grownFromPlayerId = parseUuid(configuration.getString("grown-from-player"));
        data.transformedFromPlayerId = parseUuid(configuration.getString("transformed-from-player"));
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
