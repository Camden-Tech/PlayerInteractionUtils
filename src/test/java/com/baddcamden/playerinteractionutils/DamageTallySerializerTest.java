package com.baddcamden.playerinteractionutils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageTallySerializerTest {

    @Test
    void roundTripsDamageTallies() {
        Instant now = Instant.parse("2024-06-01T12:00:00Z");
        UUID first = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID second = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        Map<UUID, DamageTallySerializer.DamageEntry> tallies = new HashMap<>();
        tallies.put(first, new DamageTallySerializer.DamageEntry(5.5, now.minusSeconds(10)));
        tallies.put(second, new DamageTallySerializer.DamageEntry(3.25, now.minusSeconds(30)));

        String serialized = DamageTallySerializer.serialize(tallies, now);
        Map<UUID, DamageTallySerializer.DamageEntry> deserialized = DamageTallySerializer.deserialize(serialized, now);

        assertEquals(tallies.keySet(), deserialized.keySet());
        assertEquals(5.5, deserialized.get(first).totalDamage());
        assertEquals(3.25, deserialized.get(second).totalDamage());
        assertEquals(tallies.get(first).updatedAt(), deserialized.get(first).updatedAt());
        assertEquals(tallies.get(second).updatedAt(), deserialized.get(second).updatedAt());
    }

    @Test
    void prunesZeroAndExpiredEntries() {
        Instant now = Instant.parse("2024-06-01T12:00:00Z");
        UUID expired = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
        UUID zero = UUID.fromString("fedcba98-7654-3210-ffff-eeeeeeeeeeee");
        UUID valid = UUID.fromString("99999999-8888-7777-6666-555555555555");

        Map<UUID, DamageTallySerializer.DamageEntry> tallies = new HashMap<>();
        tallies.put(expired, new DamageTallySerializer.DamageEntry(10.0, now.minus(DamageTallySerializer.entryTtl()).minusSeconds(1)));
        tallies.put(zero, new DamageTallySerializer.DamageEntry(0.0, now.minusSeconds(5)));
        tallies.put(valid, new DamageTallySerializer.DamageEntry(2.5, now.minusSeconds(15)));

        String serialized = DamageTallySerializer.serialize(tallies, now);
        Map<UUID, DamageTallySerializer.DamageEntry> deserialized = DamageTallySerializer.deserialize(serialized, now);

        assertEquals(Set.of(valid), deserialized.keySet());
        assertTrue(serialized.contains(valid.toString()));
    }
}
