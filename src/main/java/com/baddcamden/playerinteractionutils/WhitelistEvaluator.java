package com.baddcamden.playerinteractionutils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Evaluates whether an entity type is permitted based on a list of configured entries that can
 * reference explicit types or tags (prefixed with '#').
 */
public class WhitelistEvaluator {
    private final List<String> entries;

    /**
     * @param entries raw whitelist entries provided by configuration
     */
    public WhitelistEvaluator(List<String> entries) {
        this.entries = Objects.requireNonNull(entries, "entries");
    }

    /**
     * Determines whether the given entity instance is allowed by delegating to its {@link EntityType}.
     *
     * @param entity entity to evaluate
     * @return {@code true} if the entity type matches an entry or the whitelist is empty
     */
    public boolean isAllowed(Entity entity) {
        return isAllowed(entity.getType());
    }

    /**
     * Determines whether the given entity type is allowed by evaluating against the configured
     * list of entries and Bukkit tags.
     *
     * @param entityType entity type to evaluate
     * @return {@code true} when the type or a tag entry matches, or when the whitelist is empty
     */
    public boolean isAllowed(EntityType entityType) {
        if (entries.isEmpty()) {
            return true;
        }

        for (String rawEntry : entries) {
            if (rawEntry == null) {
                continue;
            }
            String entry = rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }

            if (entry.startsWith("#")) {
                String tagName = entry.substring(1);
                Tag<EntityType> tag = resolveTag(tagName);
                if (tag != null && tag.isTagged(entityType)) {
                    return true;
                }
            } else if (matchesType(entry, entityType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether a whitelist entry matches an entity type by namespace, key, or enum name.
     */
    private boolean matchesType(String entry, EntityType type) {
        String normalizedEntry = entry.toLowerCase(Locale.ROOT);
        String namespaced = type.getKey().toString().toLowerCase(Locale.ROOT);
        if (namespaced.equals(normalizedEntry)) {
            return true;
        }

        String keyOnly = type.getKey().getKey().toLowerCase(Locale.ROOT);
        return keyOnly.equals(normalizedEntry) || type.name().toLowerCase(Locale.ROOT).equals(normalizedEntry);
    }

    /**
     * Resolves a Bukkit tag based on the provided entry, falling back to the Minecraft namespace
     * when no namespace is provided.
     */
    private Tag<EntityType> resolveTag(String entry) {
        NamespacedKey key = namespacedKey(entry);
        if (key == null) {
            return null;
        }
        return Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, key, EntityType.class);
    }

    /**
     * Parses a string into a {@link NamespacedKey}, accepting both fully qualified and shorthand
     * Minecraft keys.
     */
    private NamespacedKey namespacedKey(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        NamespacedKey parsed = NamespacedKey.fromString(value);
        if (parsed != null) {
            return parsed;
        }

        return NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT));
    }
}
