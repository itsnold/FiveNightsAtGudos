package com.gudos.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves writable player-data files outside the application and its bundled resources. [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26). Related: [Main.java:72 (launch)](../Main.java#L72) (resolves paths at boot); [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path). */
/*
 * AppPaths
 *
 * Related: [Main.java:72 (launch)](../Main.java#L72) (resolves paths at boot);
 * [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path);
 * [persistence/Database.java:57 (Database)](Database.java#L57) (uses gudo.db path);
 * [persistence/ShiftLogStore.java:49 (ShiftLogStore)](ShiftLogStore.java#L49) (uses shift-log.txt path).
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
 * [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
 */
public record AppPaths(Path directory) {
    /*
     * Normalize the supplied directory into an absolute path. resolve is responsible for creating
     * it; direct construction alone does not create the directory.
     * [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     * Related: [Main.java:72 (launch)](../Main.java#L72) (resolves paths at boot);
     * [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path);
     * [persistence/Database.java:57 (Database)](Database.java#L57) (uses gudo.db path).
     */
    public AppPaths {
        directory = directory.toAbsolutePath().normalize();
    }

    /*
     * Prefer nonblank -Dgudo.dataDir; otherwise use LOCALAPPDATA/FiveNightsAtGudos when that
     * environment variable exists; otherwise use user.home/.five-nights-at-gudos. Create the folder
     * and return normalized paths. The override is useful for safe isolated demos.
     * [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     * Related: [Main.java:109 (resolve call)](../Main.java#L109) (caller of resolve);
     * [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path);
     * [persistence/Database.java:57 (Database)](Database.java#L57) (uses gudo.db path).
     */
    public static AppPaths resolve() throws IOException {
        // Priority: -Dgudo.dataDir override, Windows LocalAppData, then the user's home folder. | [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26). | Related: [Main.java:109 (resolve call)](../Main.java#L109) (caller of resolve); [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path).
        String override = System.getProperty("gudo.dataDir");
        String local = System.getenv("LOCALAPPDATA");
        Path directory = override != null && !override.isBlank() ? Path.of(override)
                : local != null && !local.isBlank() ? Path.of(local, "FiveNightsAtGudos")
                : Path.of(System.getProperty("user.home"), ".five-nights-at-gudos");
        Files.createDirectories(directory);
        return new AppPaths(directory);
    }

    // SQLite is the source for progress/history; the log is a report and settings are preferences. | [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26). | Related: [Main.java:112 (database call)](../Main.java#L112) (caller of database); [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path).
    /*
     * Resolve gudo.db beneath the chosen writable directory. This is SQLite storage, not a bundled
     * resource.
     * [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     * Related: [Main.java:112 (database call)](../Main.java#L112) (caller of database);
     * [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path);
     * [persistence/Database.java:57 (Database)](Database.java#L57) (uses gudo.db path).
     */
    public Path database() { return directory.resolve("gudo.db"); }
    /*
     * Resolve settings.txt beneath the chosen writable directory. Preferences are editable UTF-8
     * key/value text.
     * [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     * Related: [Application.java:640 (settings call)](../Application.java#L640) (caller of settings);
     * [Main.java:110 (settings call)](../Main.java#L110) (caller of settings);
     * [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path).
     */
    public Path settings() { return directory.resolve("settings.txt"); }
    /*
     * Resolve shift-log.txt beneath the chosen writable directory. This is a report, not the
     * authoritative progress file.
     * [p. 26: AppPaths](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     * Related: [Application.java:119 (shiftLog call)](../Application.java#L119) (caller of shiftLog);
     * [Main.java:72 (launch)](../Main.java#L72) (resolves paths at boot);
     * [persistence/SettingsStore.java:48 (SettingsStore)](SettingsStore.java#L48) (uses settings.txt path).
     */
    public Path shiftLog() { return directory.resolve("shift-log.txt"); }
}
