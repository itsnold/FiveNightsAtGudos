package com.gudos.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves writable player-data files outside the application and its bundled resources. Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26). */
/*
 * AppPaths
 *
 * Purpose: Resolves writable player-data paths outside the packaged game.
 *
 * Execution flow: Main calls resolve; SettingsPanel displays the folder; Database, SettingsStore
 * and ShiftLogStore use the three child paths.
 *
 * Design rationale: A JAR contains read-only resources; user saves need a writable
 * external folder. gudo.dataDir supports isolated runs without changing ordinary player data.
 *
 * State: directory is normalized absolute. database() -> gudo.db, settings() ->
 * settings.txt, shiftLog() -> shift-log.txt.
 *
 * Summary: The priority is explicit system property, then LOCALAPPDATA if present, then the
 * home directory fallback. It is based on the available environment value, not an explicit
 * operating-system check.
 * Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
 */
public record AppPaths(Path directory) {
    /*
     * Normalize the supplied directory into an absolute path. resolve is responsible for creating
     * it; direct construction alone does not create the directory.
     * Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     */
    public AppPaths {
        directory = directory.toAbsolutePath().normalize();
    }

    /*
     * Prefer nonblank -Dgudo.dataDir; otherwise use LOCALAPPDATA/FiveNightsAtGudos when that
     * environment variable exists; otherwise use user.home/.five-nights-at-gudos. Create the folder
     * and return normalized paths. The override is useful for safe isolated demos.
     * Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     */
    public static AppPaths resolve() throws IOException {
        // Priority: -Dgudo.dataDir override, Windows LocalAppData, then the user's home folder. | Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
        String override = System.getProperty("gudo.dataDir");
        String local = System.getenv("LOCALAPPDATA");
        Path directory = override != null && !override.isBlank() ? Path.of(override)
                : local != null && !local.isBlank() ? Path.of(local, "FiveNightsAtGudos")
                : Path.of(System.getProperty("user.home"), ".five-nights-at-gudos");
        Files.createDirectories(directory);
        return new AppPaths(directory);
    }

    // SQLite is the source for progress/history; the log is a report and settings are preferences. | Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
    /*
     * Resolve gudo.db beneath the chosen writable directory. This is SQLite storage, not a bundled
     * resource.
     * Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     */
    public Path database() { return directory.resolve("gudo.db"); }
    /*
     * Resolve settings.txt beneath the chosen writable directory. Preferences are editable UTF-8
     * key/value text.
     * Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     */
    public Path settings() { return directory.resolve("settings.txt"); }
    /*
     * Resolve shift-log.txt beneath the chosen writable directory. This is a report, not the
     * authoritative progress file.
     * Reviewer (42-page edition): [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     */
    public Path shiftLog() { return directory.resolve("shift-log.txt"); }
}
