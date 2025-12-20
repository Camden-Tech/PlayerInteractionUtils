package com.baddcamden.playerinteractionutils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerDataManagerTest {

    @Test
    void savesAndLoadsPlayerData(@TempDir File tempDir) {
        Logger logger = Logger.getLogger("PlayerDataManagerTest");
        PlayerDataManager manager = new PlayerDataManager(tempDir, logger);
        UUID playerId = UUID.randomUUID();

        PlayerData data = manager.get(playerId);
        data.increment(PlayerData.CounterType.BLOCKS_PLACED);
        data.addDamage(5.5);
        manager.save(playerId);

        PlayerDataManager reloadedManager = new PlayerDataManager(tempDir, logger);
        reloadedManager.load(playerId);
        PlayerData loaded = reloadedManager.get(playerId);

        assertNotNull(loaded);
        assertEquals(1L, loaded.counters().get(PlayerData.CounterType.BLOCKS_PLACED));
        assertEquals(5.5, loaded.damageToNonPlayers());
    }
}
