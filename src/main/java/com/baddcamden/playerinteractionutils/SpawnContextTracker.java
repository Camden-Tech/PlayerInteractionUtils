package com.baddcamden.playerinteractionutils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Captures short-lived player interactions that may result in entity spawns so the spawn can be
 * attributed back to the player.
 */
public class SpawnContextTracker {
    private static final long EXPIRY_MILLIS = 10_000;
    private static final double MAX_DISTANCE = 4.0;

    private final List<SpawnContext> contexts = new LinkedList<>();

    /**
     * Records the use of a spawn egg, storing the player and the click location to match against a
     * subsequent spawn event.
     */
    public void recordSpawnEggUse(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        Material material = event.getItem().getType();
        if (!material.name().endsWith("_SPAWN_EGG")) {
            return;
        }

        Location location = event.getClickedBlock() != null
                ? event.getClickedBlock().getLocation()
                : event.getPlayer().getLocation();
        contexts.add(new SpawnContext(event.getPlayer().getUniqueId(), location, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG, Instant.now().toEpochMilli()));
    }

    /**
     * Records the throw of a regular egg, which may result in a spawned entity.
     */
    public void recordEggThrow(PlayerEggThrowEvent event) {
        Projectile egg = event.getEgg();
        CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.EGG;
        contexts.add(new SpawnContext(event.getPlayer().getUniqueId(), egg.getLocation(), reason, Instant.now().toEpochMilli()));
    }

    /**
     * Attempts to resolve the player responsible for a spawn event based on recent interactions
     * and proximity. Removes consumed contexts to avoid duplicate matches.
     */
    public Optional<UUID> findPlayer(CreatureSpawnEvent event) {
        purge();
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BREEDING && event.getEntity() instanceof org.bukkit.entity.Breedable breedable) {
            UUID breeder = breedable.getUniqueId();
            return Optional.of(breeder);

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

    /**
     * Discards stale spawn contexts so only recent player actions are considered.
     */
    private void purge() {
        long now = Instant.now().toEpochMilli();
        contexts.removeIf(context -> now - context.timestamp() > EXPIRY_MILLIS);
    }

    /**
     * Captures player attribution for an entity spawn along with the location and timestamp.
     */
    private record SpawnContext(UUID playerId, Location location, CreatureSpawnEvent.SpawnReason reason, long timestamp) {
    }
}
