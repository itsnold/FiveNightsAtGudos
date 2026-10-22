package com.gudos.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** A real, human-editable UTF-8 .txt settings file, read at startup and written on Apply. */
public final class SettingsStore {
    private final Path file;

    public SettingsStore(Path file) { this.file = file; }

    public Settings load() throws IOException {
        if (!Files.exists(file)) {
            save(Settings.DEFAULT);
            return Settings.DEFAULT;
        }
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            values.load(reader);
        } catch (IllegalArgumentException malformed) {
            return Settings.DEFAULT;
        }
        int volume;
        try { volume = Integer.parseInt(values.getProperty("volume", "55").strip()); }
        catch (NumberFormatException invalid) { volume = 55; }
        return new Settings(values.getProperty("playerName", "Night Guard"), volume,
                booleanValue(values, "reducedEffects", false), booleanValue(values, "fullscreen", false));
    }

    private boolean booleanValue(Properties values, String key, boolean fallback) {
        String value = values.getProperty(key, "").strip();
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    public void save(Settings settings) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Properties values = new Properties();
        values.setProperty("playerName", settings.playerName());
        values.setProperty("volume", Integer.toString(settings.volume()));
        values.setProperty("reducedEffects", Boolean.toString(settings.reducedEffects()));
        values.setProperty("fullscreen", Boolean.toString(settings.fullscreen()));
        Path temporary = Files.createTempFile(parent, "settings-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, "Five Nights at Gudo's — preferences (UTF-8)");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
