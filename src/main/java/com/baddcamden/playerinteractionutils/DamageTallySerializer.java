package com.baddcamden.playerinteractionutils;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class DamageTallySerializer {
    private static final Duration ENTRY_TTL = Duration.ofHours(6);
    private static final String ENTRY_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = ",";

    private DamageTallySerializer() {
    }

    public record DamageEntry(double totalDamage, Instant updatedAt) {
    }

    static Duration entryTtl() {
        return ENTRY_TTL;
    }

    public static Map<UUID, DamageEntry> read(PersistentDataContainer container, NamespacedKey key, Instant now) {
        String raw = container.get(key, PersistentDataType.STRING);
        return deserialize(raw, now);
    }

    public static void updateDamage(PersistentDataContainer container, NamespacedKey key, UUID playerId, double additionalDamage, Instant now) {
        Map<UUID, DamageEntry> tallies = new HashMap<>(read(container, key, now));
        DamageEntry existing = tallies.get(playerId);
        double updatedDamage = (existing != null ? existing.totalDamage() : 0.0) + additionalDamage;
        tallies.put(playerId, new DamageEntry(updatedDamage, now));

        String serialized = serialize(tallies, now);
        if (serialized.isEmpty()) {
            container.remove(key);
        } else {
            container.set(key, PersistentDataType.STRING, serialized);
        }
    }

    public static Map<UUID, DamageEntry> deserialize(String raw, Instant now) {
        Map<UUID, DamageEntry> tallies = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return tallies;
        }

        String[] entries = raw.split(ENTRY_SEPARATOR);
        for (String entry : entries) {
            if (entry.isBlank()) {
                continue;
            }

            String[] fields = entry.split(FIELD_SEPARATOR);
            if (fields.length != 3) {
                continue;
            }

            try {
                UUID playerId = UUID.fromString(fields[0]);
                double damage = Double.parseDouble(fields[1]);
                Instant updatedAt = Instant.ofEpochMilli(Long.parseLong(fields[2]));

                if (damage <= 0 || isExpired(updatedAt, now)) {
                    continue;
                }

                tallies.put(playerId, new DamageEntry(damage, updatedAt));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entry
            }
        }

        return tallies;
    }

    public static String serialize(Map<UUID, DamageEntry> tallies, Instant now) {
        Map<UUID, DamageEntry> filtered = new LinkedHashMap<>();

        tallies.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().totalDamage() > 0)
                .filter(entry -> !isExpired(entry.getValue().updatedAt(), now))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                .forEach(entry -> filtered.put(entry.getKey(), entry.getValue()));

        return filtered.entrySet().stream()
                .map(entry -> entry.getKey().toString()
                        + FIELD_SEPARATOR
                        + Double.toString(entry.getValue().totalDamage())
                        + FIELD_SEPARATOR
                        + entry.getValue().updatedAt().toEpochMilli())
                .reduce((a, b) -> a + ENTRY_SEPARATOR + b)
                .orElse("");
    }

    private static boolean isExpired(Instant updatedAt, Instant now) {
        return updatedAt.isBefore(now.minus(ENTRY_TTL));
    }
}
