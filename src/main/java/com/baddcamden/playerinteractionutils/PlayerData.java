package com.baddcamden.playerinteractionutils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayerData {
    public enum CounterType {
        BLOCKS_PLACED,
        EGG_SPAWNS,
        SPAWN_EGG_SPAWNS,
        BREEDING_SPAWNS
    }

    private final UUID playerId;
    private final Map<CounterType, Long> counters = new EnumMap<>(CounterType.class);
    private double damageToNonPlayers;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        for (CounterType type : CounterType.values()) {
            counters.put(type, 0L);
        }
    }

    public UUID playerId() {
        return playerId;
    }

    public long increment(CounterType type) {
        long updated = counters.get(type) + 1;
        counters.put(type, updated);
        return updated;
    }

    public void addDamage(double amount) {
        damageToNonPlayers += amount;
    }

    public Map<CounterType, Long> counters() {
        return counters;
    }

    public double damageToNonPlayers() {
        return damageToNonPlayers;
    }

    public static PlayerData load(UUID playerId, File file) throws IOException {
        if (!file.exists()) {
            return new PlayerData(playerId);
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(playerId);
        for (CounterType type : CounterType.values()) {
            long value = configuration.getLong("counters." + type.name().toLowerCase(), 0L);
            data.counters.put(type, value);
        }
        data.damageToNonPlayers = configuration.getDouble("damage.nonPlayers", 0.0D);
        return data;
    }

    public void save(File file) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection countersSection = configuration.createSection("counters");
        counters.forEach((type, value) -> countersSection.set(type.name().toLowerCase(), value));
        configuration.set("damage.nonPlayers", damageToNonPlayers);
        configuration.save(file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerData that = (PlayerData) o;
        return Double.compare(that.damageToNonPlayers, damageToNonPlayers) == 0 && Objects.equals(playerId, that.playerId) && Objects.equals(counters, that.counters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, counters, damageToNonPlayers);
    }
}
