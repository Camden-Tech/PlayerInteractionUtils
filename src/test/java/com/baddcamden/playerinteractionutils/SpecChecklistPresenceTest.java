package com.baddcamden.playerinteractionutils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecChecklistPresenceTest {

    private static final List<String> REQUIRED_LINES = List.of(
            "Block tags",
            "Growth/transform tags",
            "Spawn-tag variants",
            "Last-hit timestamps",
            "Per-player counters",
            "Damage NBT tallies",
            "Config toggles",
            "Whitelist handling"
    );

    @Test
    void specListsRequiredFeatures() throws IOException {
        String spec = Files.readString(Path.of("SPEC.md"));

        for (String line : REQUIRED_LINES) {
            assertTrue(spec.contains(line), () -> "SPEC.md missing checklist entry for: " + line);
        }
    }
}
