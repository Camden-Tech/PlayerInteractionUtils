package com.baddcamden.playerinteractionutils;

import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WhitelistEvaluatorTest {

    @Test
    void allowsAllWhenEmpty() {
        WhitelistEvaluator evaluator = new WhitelistEvaluator(List.of());

        assertTrue(evaluator.isAllowed(EntityType.COW));
    }

    @Test
    void matchesTypeByNameOrNamespace() {
        WhitelistEvaluator evaluator = new WhitelistEvaluator(List.of("minecraft:cow", "zombie"));

        assertTrue(evaluator.isAllowed(EntityType.COW));
        assertTrue(evaluator.isAllowed(EntityType.ZOMBIE));
        assertFalse(evaluator.isAllowed(EntityType.SHEEP));
    }

    @Test
    void matchesEntityTags() {
        Tag<EntityType> tag = mock(Tag.class);
        when(tag.isTagged(EntityType.SHEEP)).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getTag(eq(Tag.REGISTRY_ENTITY_TYPES), any(), eq(EntityType.class)))
                    .thenReturn(tag);

            WhitelistEvaluator evaluator = new WhitelistEvaluator(List.of("#minecraft:animals"));

            assertTrue(evaluator.isAllowed(EntityType.SHEEP));
            assertFalse(evaluator.isAllowed(EntityType.CREEPER));
        }
    }
}
