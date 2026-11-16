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

/** A real, human-editable UTF-8 .txt settings file, read at startup and written on Apply. Reviewer (42-page edition): [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). */
/*
 * SettingsStore
 *
 * Purpose: Reads and replaces the UTF-8 settings.txt preferences file.
 *
 * Execution flow: load creates defaults on first launch or parses Properties. save writes a
 * temporary neighbor, then replaces the real file.
 *
 * Design rationale: Use explicit true/false parsing, volume fallback and Settings
 * normalization. Atomic replacement reduces truncated settings; filesystems without atomic move use
 * a regular replacement fallback.
 *
 * State: file identifies one path. Keys: playerName, volume, reducedEffects,
 * fullscreen. Malformed Properties syntax returns defaults; IO failures still propagate.
 *
 * Summary: settings.txt stores preferences. It does not unlock nights or restore a live
 * session; SQLite supplies progress.
 * Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
 */
public final class SettingsStore {
    private final Path file;

    /*
     * Retain the settings.txt path. File loading/writing happens only when load/save is called.
     * Reviewer (42-page edition): [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     */
    public SettingsStore(Path file) { this.file = file; }

    // First launch creates defaults. Later launches parse the key/value file and normalize its values. | Reviewer (42-page edition): [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
    /*
     * On first launch save and return DEFAULT. Otherwise read UTF-8 Properties; malformed escapes
     * use DEFAULT, invalid volume text uses 55, booleans accept explicit true/false or fall back.
     * Construct Settings to sanitize name and clamp numeric bounds; ordinary IO errors propagate.
     * Reviewer (42-page edition): [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     */
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

    // Accept explicit true/false values and use the supplied fallback for unrecognized text. | Reviewer (42-page edition): [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
    /*
     * Strip a property, compare true/false ignoring case, and return the supplied fallback for any
     * other text. This prevents an unrecognized preference from silently meaning true.
     * Reviewer (42-page edition): [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     */
    private boolean booleanValue(Properties values, String key, boolean fallback) {
        String value = values.getProperty(key, "").strip();
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    // Save a complete replacement rather than appending another copy of the preferences. | Reviewer (42-page edition): [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
    /*
     * Create the parent folder and write all four keys to a temporary UTF-8 file in that same
     * folder. Move it over settings.txt with ATOMIC_MOVE plus replacement, falling back only when
     * atomic move is unsupported. Finally delete any temporary residue. This is replacement, not
     * append.
     * Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     */
    public void save(Settings settings) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Properties values = new Properties();
        values.setProperty("playerName", settings.playerName());
        values.setProperty("volume", Integer.toString(settings.volume()));
        values.setProperty("reducedEffects", Boolean.toString(settings.reducedEffects()));
        values.setProperty("fullscreen", Boolean.toString(settings.fullscreen()));
        // Write in the same folder, then swap files atomically where the filesystem supports it. | Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
        // The fallback replacement is used only when atomic moves are unavailable. | Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: SettingsStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
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
