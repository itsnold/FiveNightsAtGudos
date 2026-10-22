package com.gudos.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreTest {
    @TempDir Path directory;

    @Test void firstLaunchWritesAnActualTextFile() throws Exception {
        Path file = directory.resolve("settings.txt");
        assertEquals(Settings.DEFAULT, new SettingsStore(file).load());
        assertTrue(Files.readString(file).contains("volume=55"));
    }

    @Test void settingsRoundTripAsUtf8() throws Exception {
        Path file = directory.resolve("settings.txt");
        SettingsStore store = new SettingsStore(file);
        Settings settings = new Settings("Guido ñ", 73, true, true);
        store.save(settings);
        assertEquals(settings, store.load());
        assertTrue(Files.readString(file).contains("Guido ñ"));
    }

    @Test void invalidValuesUseSafeDefaultsAndBounds() throws Exception {
        Path file = directory.resolve("settings.txt");
        Files.writeString(file, "volume=garbage\nplayerName=\nreducedEffects=maybe\nfullscreen=true\n");
        assertEquals(new Settings("Night Guard", 55, false, true), new SettingsStore(file).load());
        Files.writeString(file, "volume=999\nplayerName=Guard\n");
        assertEquals(100, new SettingsStore(file).load().volume());
    }

    @Test void namesCannotInjectLinesIntoReports() {
        Settings settings = new Settings("Guard\nName\r", -5, false, false);
        assertEquals("GuardName", settings.playerName());
        assertEquals(0, settings.volume());
        assertEquals(32, new Settings("a".repeat(100), 50, false, false).playerName().length());
    }
}
