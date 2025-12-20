package com.baddcamden.playerinteractionutils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight, file-backed container for non-player entity metadata when a {@link org.bukkit.persistence.PersistentDataContainer}
 * cannot be used (for example, on platforms that do not persist non-player PDC data between loads).
 *
 * Data is read on entity load and saved when the entity leaves memory, mirroring the pattern used for {@link PlayerData}.
 */
public class NonPlayerEntityData {
    private final UUID entityId;
    private final Map<CreatureSpawnEvent.SpawnReason, UUID> spawnOwners = new EnumMap<>(CreatureSpawnEvent.SpawnReason.class);
    private final Map<UUID, DamageTallySerializer.DamageEntry> damageTallies = new java.util.HashMap<>();
    private UUID lastHitBy;
    private long lastHitAt;

    public NonPlayerEntityData(UUID entityId) {
        this.entityId = Objects.requireNonNull(entityId, "entityId");
    }

    public UUID entityId() {
        return entityId;
    }

    public void setSpawnOwner(CreatureSpawnEvent.SpawnReason reason, UUID ownerId) {
        if (reason == null || ownerId == null) {
            return;
        }
        spawnOwners.put(reason, ownerId);
    }

    public Optional<UUID> spawnOwner(CreatureSpawnEvent.SpawnReason reason) {
        return Optional.ofNullable(spawnOwners.get(reason));
    }

    public void recordLastHit(UUID playerId, Instant when) {
        if (playerId == null || when == null) {
            return;
        }
        lastHitBy = playerId;
        lastHitAt = when.toEpochMilli();
    }

    public Optional<UUID> lastHitBy() {
        return Optional.ofNullable(lastHitBy);
    }

    public long lastHitAt() {
        return lastHitAt;
    }

    public void addDamage(UUID playerId, double amount, Instant now) {
        if (playerId == null || now == null || amount <= 0) {
            return;
        }
        purgeDamage(now);
        DamageTallySerializer.DamageEntry existing = damageTallies.get(playerId);
        double updatedDamage = (existing != null ? existing.totalDamage() : 0.0) + amount;
        damageTallies.put(playerId, new DamageTallySerializer.DamageEntry(updatedDamage, now));
    }

    public Map<UUID, DamageTallySerializer.DamageEntry> damageTallies(Instant now) {
        purgeDamage(now);
        return Map.copyOf(damageTallies);
    }

    public static NonPlayerEntityData load(UUID entityId, File file) throws IOException {
        NonPlayerEntityData data = new NonPlayerEntityData(entityId);
        if (!file.exists()) {
            return data;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection spawnsSection = configuration.getConfigurationSection("spawns");
        if (spawnsSection != null) {
            for (String key : spawnsSection.getKeys(false)) {
                CreatureSpawnEvent.SpawnReason reason = parseReason(key);
                UUID owner = parseUuid(spawnsSection.getString(key));
                if (reason != null && owner != null) {
                    data.setSpawnOwner(reason, owner);
                }
            }
        }

        data.lastHitBy = parseUuid(configuration.getString("last-hit.by"));
        data.lastHitAt = configuration.getLong("last-hit.at", 0L);

        String damageRaw = configuration.getString("damage.by-player");
        Map<UUID, DamageTallySerializer.DamageEntry> parsedDamage = DamageTallySerializer.deserialize(damageRaw, Instant.now());
        data.damageTallies.putAll(parsedDamage);

        return data;
    }

    public void save(File file) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection spawnsSection = configuration.createSection("spawns");
        spawnOwners.forEach((reason, owner) -> spawnsSection.set(reason.name().toLowerCase(), owner.toString()));

        configuration.set("last-hit.by", lastHitBy != null ? lastHitBy.toString() : null);
        configuration.set("last-hit.at", lastHitAt > 0 ? lastHitAt : null);

        String serializedDamage = DamageTallySerializer.serialize(damageTallies, Instant.now());
        configuration.set("damage.by-player", serializedDamage);
        configuration.save(file);
    }

    private void purgeDamage(Instant now) {
        if (now == null) {
            return;
        }
        damageTallies.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().updatedAt().isBefore(now.minus(DamageTallySerializer.entryTtl())));
    }

    private static CreatureSpawnEvent.SpawnReason parseReason(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return CreatureSpawnEvent.SpawnReason.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
