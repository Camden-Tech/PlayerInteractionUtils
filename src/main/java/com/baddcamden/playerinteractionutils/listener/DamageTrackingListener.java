package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.DataKeys;
import com.baddcamden.playerinteractionutils.DamageTallySerializer;
import com.baddcamden.playerinteractionutils.NonPlayerEntityData;
import com.baddcamden.playerinteractionutils.NonPlayerEntityDataManager;
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
    private final NonPlayerEntityDataManager entityDataManager;
    private final boolean entityPdcEnabled;

    public DamageTrackingListener(ConfigSettings settings, DataKeys dataKeys, PlayerDataManager playerDataManager, NonPlayerEntityDataManager entityDataManager) {
        this.settings = settings;
        this.dataKeys = dataKeys;
        this.playerDataManager = playerDataManager;
        this.entityDataManager = entityDataManager;
        this.entityPdcEnabled = settings.entityPdcEnabled();
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
            trackLastHit(victim, player.getUniqueId(), now);
            trackDamage(victim, player.getUniqueId(), event.getFinalDamage(), now, countersEnabled);
            if (countersEnabled) {
                playerDataManager.get(player.getUniqueId()).increment(PlayerData.CounterType.LAST_HIT_UPDATES);
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

    private void trackLastHit(LivingEntity victim, UUID playerId, Instant now) {
        if (entityPdcEnabled || victim instanceof Player) {
            PersistentDataContainer pdc = victim.getPersistentDataContainer();
            pdc.set(dataKeys.lastHitBy, PersistentDataType.STRING, playerId.toString());
            pdc.set(dataKeys.lastHitAt, PersistentDataType.LONG, now.toEpochMilli());
            return;
        }

        NonPlayerEntityData data = entityDataManager.get(victim.getUniqueId());
        data.recordLastHit(playerId, now);
    }

    private void trackDamage(LivingEntity victim, UUID playerId, double amount, Instant now, boolean countersEnabled) {
        if (victim instanceof Player) {
            return;
        }

        if (entityPdcEnabled) {
            PersistentDataContainer pdc = victim.getPersistentDataContainer();
            DamageTallySerializer.updateDamage(
                    pdc,
                    dataKeys.damageByPlayer,
                    playerId,
                    amount,
                    now);
        } else {
            NonPlayerEntityData data = entityDataManager.get(victim.getUniqueId());
            data.addDamage(playerId, amount, now);
        }

        if (countersEnabled) {
            playerDataManager.get(playerId).increment(PlayerData.CounterType.DAMAGE_RECORDS);
        }
    }
}
