package com.baddcamden.playerinteractionutils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SpawnContextTracker {
    private static final long EXPIRY_MILLIS = 10_000;
    private static final double MAX_DISTANCE = 4.0;

    private final List<SpawnContext> contexts = new LinkedList<>();

    public void recordSpawnEggUse(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        Material material = event.getItem().getType();
        if (!material.name().endsWith("_SPAWN_EGG")) {
            return;
        }

        Location location = event.getInteractionPoint() != null
                ? event.getInteractionPoint()
                : event.getPlayer().getLocation();
        contexts.add(new SpawnContext(event.getPlayer().getUniqueId(), location, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG, Instant.now().toEpochMilli()));
    }

    public void recordEggThrow(PlayerEggThrowEvent event) {
        Projectile egg = event.getEgg();
        CreatureSpawnEvent.SpawnReason reason = event.isHatching()
                ? CreatureSpawnEvent.SpawnReason.CHICKEN_EGG
                : CreatureSpawnEvent.SpawnReason.EGG;
        contexts.add(new SpawnContext(event.getPlayer().getUniqueId(), egg.getLocation(), reason, Instant.now().toEpochMilli()));
    }

    public Optional<UUID> findPlayer(CreatureSpawnEvent event) {
        purge();
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BREEDING && event.getEntity() instanceof org.bukkit.entity.Breedable breedable) {
            UUID breeder = breedable.getBreedCause();
            if (breeder != null) {
                return Optional.of(breeder);
            }
        }

        for (Iterator<SpawnContext> iterator = contexts.iterator(); iterator.hasNext(); ) {
            SpawnContext context = iterator.next();
            if (context.reason() != event.getSpawnReason()) {
                continue;
            }
            if (!context.location().getWorld().equals(event.getLocation().getWorld())) {
                continue;
            }
            if (context.location().distanceSquared(event.getLocation()) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            iterator.remove();
            return Optional.of(context.playerId());
        }
        return Optional.empty();
    }

    private void purge() {
        long now = Instant.now().toEpochMilli();
        contexts.removeIf(context -> now - context.timestamp() > EXPIRY_MILLIS);
    }

    private record SpawnContext(UUID playerId, Location location, CreatureSpawnEvent.SpawnReason reason, long timestamp) {
    }
}
