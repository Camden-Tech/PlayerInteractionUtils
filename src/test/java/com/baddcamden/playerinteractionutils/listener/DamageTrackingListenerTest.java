package com.baddcamden.playerinteractionutils.listener;

import com.baddcamden.playerinteractionutils.ConfigSettings;
import com.baddcamden.playerinteractionutils.DataKeys;
import com.baddcamden.playerinteractionutils.PlayerDataManager;
import com.baddcamden.playerinteractionutils.WhitelistEvaluator;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DamageTrackingListenerTest {

    @Mock
    private ConfigSettings settings;

    @Mock
    private PlayerDataManager playerDataManager;

    @Mock
    private EntityDamageByEntityEvent event;

    @Mock
    private Player victim;

    @Mock
    private Player damager;

    @Mock
    private PersistentDataContainer dataContainer;

    @Mock
    private WhitelistEvaluator whitelistEvaluator;

    private DataKeys dataKeys;
    private DamageTrackingListener listener;

    @BeforeEach
    void setUp() {
        dataKeys = new DataKeys("playerinteractionutils");
        listener = new DamageTrackingListener(settings, dataKeys, playerDataManager);

        when(settings.damageTracking()).thenReturn(true);
        when(settings.playerCounters()).thenReturn(false);
        when(settings.entityWhitelist()).thenReturn(whitelistEvaluator);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(damager);
        when(victim.getPersistentDataContainer()).thenReturn(dataContainer);
        when(whitelistEvaluator.isAllowed(victim)).thenReturn(true);
        when(damager.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
    }

    @Test
    void tagsVictimWhenWhitelisted() {
        listener.onEntityDamage(event);

        verify(dataContainer).set(eq(dataKeys.lastHitBy), eq(PersistentDataType.STRING), eq(damager.getUniqueId().toString()));
        verify(dataContainer).set(eq(dataKeys.lastHitAt), eq(PersistentDataType.LONG), anyLong());
    }

    @Test
    void skipsTaggingWhenNotWhitelisted() {
        when(whitelistEvaluator.isAllowed(victim)).thenReturn(false);

        listener.onEntityDamage(event);

        verify(dataContainer, never()).set(eq(dataKeys.lastHitBy), eq(PersistentDataType.STRING), eq(damager.getUniqueId().toString()));
    }
}
