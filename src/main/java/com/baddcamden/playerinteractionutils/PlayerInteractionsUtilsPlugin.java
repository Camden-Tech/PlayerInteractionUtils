package com.baddcamden.playerinteractionutils;

import com.baddcamden.playerinteractionutils.listener.BlockTagListener;
import com.baddcamden.playerinteractionutils.listener.DamageTrackingListener;
import com.baddcamden.playerinteractionutils.listener.EntitySpawnListener;
import com.baddcamden.playerinteractionutils.listener.ChunkLifecycleListener;
import com.baddcamden.playerinteractionutils.listener.NonPlayerEntityLifecycleListener;
import com.baddcamden.playerinteractionutils.listener.PlayerLifecycleListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Collectors;

public class PlayerInteractionsUtilsPlugin extends JavaPlugin {
    private ConfigSettings settings;
    private PlayerDataManager playerDataManager;
    private NonPlayerEntityDataManager entityDataManager;
    private BlockDataManager blockDataManager;
    private DataKeys dataKeys;
    private SpawnContextTracker spawnContextTracker;
    private BlockTagStorage blockTagStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfiguration();
        dataKeys = new DataKeys(this);
        playerDataManager = new PlayerDataManager(getDataFolder(), getLogger());
        entityDataManager = new NonPlayerEntityDataManager(getDataFolder(), getLogger());
        blockDataManager = new BlockDataManager(getDataFolder(), getLogger());
        spawnContextTracker = new SpawnContextTracker();
        blockTagStorage = new BlockTagStorage(dataKeys, settings.chunkPdcEnabled(), blockDataManager);

        registerListeners();

        Bukkit.getOnlinePlayers().forEach(player -> playerDataManager.load(player.getUniqueId()));
        getLogger().info("PlayerInteractionsUtils enabled with features: " + enabledFeaturesDescription());
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(player -> playerDataManager.save(player.getUniqueId()));
        if (!settings.entityPdcEnabled()) {
            entityDataManager.saveAllTracked();
        }
        if (!settings.chunkPdcEnabled()) {
            blockDataManager.saveAllTracked();
        }
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerLifecycleListener(playerDataManager), this);
        pluginManager.registerEvents(new BlockTagListener(settings, blockTagStorage, playerDataManager, blockDataManager), this);
        pluginManager.registerEvents(new EntitySpawnListener(settings, dataKeys, playerDataManager, spawnContextTracker, entityDataManager), this);
        pluginManager.registerEvents(new DamageTrackingListener(settings, dataKeys, playerDataManager, entityDataManager), this);
        if (!settings.entityPdcEnabled()) {
            pluginManager.registerEvents(new NonPlayerEntityLifecycleListener(entityDataManager, false), this);
        }
        if (!settings.chunkPdcEnabled()) {
            pluginManager.registerEvents(new ChunkLifecycleListener(blockDataManager, false), this);
        }
    }

    public void reloadConfiguration() {
        reloadConfig();
        FileConfiguration config = getConfig();
        settings = ConfigSettings.fromConfiguration(config);
    }

    private String enabledFeaturesDescription() {
        return java.util.stream.Stream.of(
                flag("blocks", settings.blockPlacementTagging() || settings.blockGrowthTagging() || settings.blockTransformTagging()),
                flag("entity-spawns", settings.entitySpawnTagging()),
                flag("damage", settings.damageTracking()),
                flag("counters", settings.playerCounters())
        ).filter(s -> !s.isEmpty()).collect(Collectors.joining(", "));
    }

    private String flag(String name, boolean enabled) {
        return enabled ? name : "";
    }
}
