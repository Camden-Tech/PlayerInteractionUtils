package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.DataKeys;
import com.baddcamden.playerinteractionutils.DamageTallySerializer;
import com.baddcamden.playerinteractionutils.PlayerData;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.util.UUID;

public class DamageTrackingListener implements Listener {
    private final ConfigSettings settings;
    private final DataKeys dataKeys;
    private final PlayerDataManager playerDataManager;

    public DamageTrackingListener(ConfigSettings settings, DataKeys dataKeys, PlayerDataManager playerDataManager) {
        this.settings = settings;
        this.dataKeys = dataKeys;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        Player player = resolvePlayer(event.getDamager());
        if (player == null) {
            return;
        }

        if (!settings.entityWhitelist().isAllowed(victim)) {
            return;
        }

        boolean countersEnabled = settings.playerCounters();
        if (settings.damageTracking()) {
            Instant now = Instant.now();
            PersistentDataContainer pdc = victim.getPersistentDataContainer();
            pdc.set(dataKeys.lastHitBy, PersistentDataType.STRING, player.getUniqueId().toString());
            pdc.set(dataKeys.lastHitAt, PersistentDataType.LONG, now.toEpochMilli());
            if (countersEnabled) {
                playerDataManager.get(player.getUniqueId()).increment(PlayerData.CounterType.LAST_HIT_UPDATES);
            }

            if (!(victim instanceof Player)) {
                DamageTallySerializer.updateDamage(
                        pdc,
                        dataKeys.damageByPlayer,
                        player.getUniqueId(),
                        event.getFinalDamage(),
                        now);
                if (countersEnabled) {
                    playerDataManager.get(player.getUniqueId()).increment(PlayerData.CounterType.DAMAGE_RECORDS);
                }
            }
        }

        if (countersEnabled && !(victim instanceof Player)) {
            playerDataManager.get(player.getUniqueId()).addDamage(event.getFinalDamage());
        }
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
