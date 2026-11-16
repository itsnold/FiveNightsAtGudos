package com.gudos.persistence;

import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** SQLite via JDBC. Connections are short-lived and all calls belong on the IO executor. [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (coordinates save/retry on the IO worker); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (saved attempt values). */
/*
 * Database
 *
 * Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (coordinates save/retry on the IO worker);
 * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (saved attempt values);
 * [persistence/Progress.java:27 (Progress)](Progress.java#L27) (loaded unlock/count summary);
 * [ui/RecordsPanel.java:55 (RecordsPanel)](../ui/RecordsPanel.java#L55) (shows loaded history).
 *
 * Purpose: Persists local profiles, unlock summaries and attempt history using SQLite/JDBC.
 *
 * Execution flow: Constructor configures/migrates schema; loadProgress selects/creates a profile;
 * saveRun inserts an attempt and updates unlocks transactionally; loadRuns returns latest rows.
 *
 * Design rationale: Prepared statements bind player values. Foreign keys and
 * CHECK/UNIQUE constraints protect relations and ranges. A UUID primary key supports idempotent
 * retries; MAX preserves unlocks when replaying earlier nights.
 *
 * State: url points at one file. Schema user_version=5; profiles 1-to-1 progress and
 * 1-to-many night_runs. Index runs_by_profile(profile_id,ended_at DESC). busy_timeout=5000;
 * foreign_keys=ON per connection; WAL requested at initialization.
 *
 * Summary: The attempt insert and unlock update share one SQLite transaction. The text report
 * is a second operation outside that transaction. A failed text append may follow a committed
 * database save.
 * [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
 */
public final class Database {
    private final String url;

    /*
     * Build the JDBC file URL, request WAL, inspect user_version and reject newer-than-five data.
     * Ensure profiles/progress/night_runs exist; rebuild legacy outcome-constrained tables in
     * transactions for supported versions, preserve existing rows/indexes, leave legacy completion
     * flags untouched and set user_version=5. Each connection enables foreign keys and lock timeout.
     * [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup);
     * [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location);
     * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (stored attempt fields).
     */
    public Database(Path file) throws SQLException {
        // SQLite is an embedded database file; there is no separate database server to connect to. | [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
        url = "jdbc:sqlite:" + file.toAbsolutePath();
        try (Connection connection = connect(); Statement sql = connection.createStatement()) {
            // Write-ahead logging lets SQLite coordinate reads with committed writes. | [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            sql.execute("PRAGMA journal_mode=WAL");
            int schemaVersion;
            try (ResultSet version = sql.executeQuery("PRAGMA user_version")) {
                schemaVersion = version.next() ? version.getInt(1) : 0;
                if (schemaVersion > 5) throw new SQLException("This database belongs to a newer game version.");
            }
            // profiles identifies a player by a unique, case-insensitive local name. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            sql.execute("""
                    CREATE TABLE IF NOT EXISTS profiles (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        created_at TEXT NOT NULL
                    )
                    """);
            // One progress row per profile holds unlock state, not individual attempt history. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            sql.execute("""
                    CREATE TABLE IF NOT EXISTS progress (
                        profile_id INTEGER PRIMARY KEY REFERENCES profiles(id),
                        next_night INTEGER NOT NULL DEFAULT 1 CHECK(next_night BETWEEN 1 AND 5),
                        highest_completed INTEGER NOT NULL DEFAULT 0 CHECK(highest_completed BETWEEN 0 AND 5),
                        updated_at TEXT NOT NULL
                    )
                    """);
            sql.execute(runsTable("night_runs"));
            // Version 2 adds CAUGHT as an allowed outcome. Rebuild/copy the old constrained table | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            // inside a transaction so a migration failure can restore the previous schema/data. | [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            if (schemaVersion == 1) {
                connection.setAutoCommit(false);
                try {
                    sql.execute(runsTable("night_runs_v2"));
                    sql.execute("INSERT INTO night_runs_v2 SELECT * FROM night_runs");
                    sql.execute("DROP TABLE night_runs");
                    sql.execute("ALTER TABLE night_runs_v2 RENAME TO night_runs");
                    sql.execute("CREATE INDEX runs_by_profile ON night_runs(profile_id, ended_at DESC)");
                    sql.execute("PRAGMA user_version=2");
                    connection.commit();
                } catch (SQLException error) {
                    connection.rollback();
                    throw error;
                } finally { connection.setAutoCommit(true); }
            }
            // Match the records screen's lookup: one profile, most recent end time first. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            sql.execute("CREATE INDEX IF NOT EXISTS runs_by_profile ON night_runs(profile_id, ended_at DESC)");
            if (schemaVersion > 0 && schemaVersion < 3) {
                connection.setAutoCommit(false);
                try {
                    sql.execute(runsTable("night_runs_v3"));
                    sql.execute("INSERT INTO night_runs_v3 SELECT * FROM night_runs");
                    sql.execute("DROP TABLE night_runs");
                    sql.execute("ALTER TABLE night_runs_v3 RENAME TO night_runs");
                    sql.execute("CREATE INDEX runs_by_profile ON night_runs(profile_id, ended_at DESC)");
                    connection.commit();
                } catch (SQLException error) {
                    connection.rollback();
                    throw error;
                } finally { connection.setAutoCommit(true); }
            }
            if (schemaVersion == 3) {
                connection.setAutoCommit(false);
                try {
                    sql.execute(runsTable("night_runs_v4"));
                    sql.execute("INSERT INTO night_runs_v4 SELECT * FROM night_runs");
                    sql.execute("DROP TABLE night_runs");
                    sql.execute("ALTER TABLE night_runs_v4 RENAME TO night_runs");
                    sql.execute("CREATE INDEX runs_by_profile ON night_runs(profile_id, ended_at DESC)");
                    connection.commit();
                } catch (SQLException error) { connection.rollback(); throw error; }
                finally { connection.setAutoCommit(true); }
            }
            // Legacy completion tables, if present, are left intact but no longer restrict play. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            sql.execute("PRAGMA user_version=5");
        }
    }

    // Each night_runs row is a RunRecord linked to profiles.id through profile_id. | [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
    // The table argument is an internal schema name, never supplied by a player. | [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
    /*
     * Return the internal CREATE TABLE statement with UUID primary key, profile foreign key, range
     * checks and all six allowed Outcome names. The formatted table name is a fixed internal
     * migration identifier, not player input.
     * [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup);
     * [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location);
     * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (stored attempt fields).
     */
    private static String runsTable(String table) {
        return """
                    CREATE TABLE IF NOT EXISTS %s (
                        id TEXT PRIMARY KEY,
                        profile_id INTEGER NOT NULL REFERENCES profiles(id),
                        night INTEGER NOT NULL CHECK(night BETWEEN 1 AND 5),
                        elapsed_seconds REAL NOT NULL CHECK(elapsed_seconds >= 0),
                        power_remaining REAL NOT NULL CHECK(power_remaining BETWEEN 0 AND 100),
                        outcome TEXT NOT NULL CHECK(outcome IN ('COMPLETED','POWER_OUT','ABANDONED','CAUGHT','MINIGAME_FAILED','MINIGAME_COMPLETED')),
                        started_at TEXT NOT NULL,
                        ended_at TEXT NOT NULL
                    )
                    """.formatted(table);
    }

    // Each operation opens/closes its own connection. SQLite needs foreign keys enabled per connection. | [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
    /*
     * Open one JDBC SQLite connection; enable foreign_keys and a five-second busy timeout for that
     * connection. If configuration fails, close before rethrowing. Callers close the connection with
     * try-with-resources.
     * [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup);
     * [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location);
     * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (stored attempt fields).
     */
    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            // Wait up to five seconds for a lock instead of immediately failing a busy write. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (opens the database at startup); [persistence/AppPaths.java:74 (database)](AppPaths.java#L74) (database file location).
            statement.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    // Select or create the local profile, then ensure it has a default progress row. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (initial summary caller); [Application.java:621 (saveSettings)](../Application.java#L621) (local-profile selection caller).
    // SQL placeholders bind values separately from SQL syntax, including names with quotes. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (initial summary caller); [Application.java:621 (saveSettings)](../Application.java#L621) (local-profile selection caller).
    /*
     * Bind the name/time into INSERT ... ON CONFLICT(name) DO NOTHING, select the case-insensitive
     * profile ID, and ensure a default progress row exists. Return the relational ID; player names
     * never become executable SQL syntax.
     * [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Main.java:72 (launch)](../Main.java#L72) (initial summary caller);
     * [Application.java:621 (saveSettings)](../Application.java#L621) (local-profile selection caller);
     * [persistence/Progress.java:27 (Progress)](Progress.java#L27) (query result value).
     */
    private long profile(Connection connection, String name) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO profiles(name,created_at) VALUES(?,?) ON CONFLICT(name) DO NOTHING")) {
            insert.setString(1, name);
            insert.setString(2, Instant.now().toString());
            insert.executeUpdate();
        }
        long id;
        try (PreparedStatement select = connection.prepareStatement("SELECT id FROM profiles WHERE name=?")) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                if (!result.next()) throw new SQLException("Could not load player profile");
                id = result.getLong(1);
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO progress(profile_id,updated_at) VALUES(?,?) ON CONFLICT(profile_id) DO NOTHING")) {
            insert.setLong(1, id);
            insert.setString(2, Instant.now().toString());
            insert.executeUpdate();
        }
        return id;
    }

    // Unlock fields come from progress; attempt/completion counts are calculated from night_runs. | [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (initial summary caller); [Application.java:621 (saveSettings)](../Application.java#L621) (local-profile selection caller).
    // Loading a name can create its profile if this is the first time it is used. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Main.java:72 (launch)](../Main.java#L72) (initial summary caller); [Application.java:621 (saveSettings)](../Application.java#L621) (local-profile selection caller).
    /*
     * Open a connection, select/create the profile, and read next_night/highest_completed plus COUNT
     * subqueries for all and normally completed runs. Return one Progress value. This read may
     * create a new profile/progress row.
     * [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Main.java:72 (launch)](../Main.java#L72) (initial summary caller);
     * [Application.java:621 (saveSettings)](../Application.java#L621) (local-profile selection caller);
     * [persistence/Progress.java:27 (Progress)](Progress.java#L27) (query result value).
     */
    public Progress loadProgress(String name) throws SQLException {
        try (Connection connection = connect()) {
            long id = profile(connection, name);
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT next_night, highest_completed,
                        (SELECT COUNT(*) FROM night_runs WHERE profile_id=?) AS total,
                        (SELECT COUNT(*) FROM night_runs WHERE profile_id=? AND outcome='COMPLETED') AS completed
                    FROM progress WHERE profile_id=?
                    """)) {
                query.setLong(1, id);
                query.setLong(2, id);
                query.setLong(3, id);
                try (ResultSet result = query.executeQuery()) {
                    if (!result.next()) throw new SQLException("Missing progress");
                    return new Progress(result.getInt(1), result.getInt(2), result.getInt(3), result.getInt(4));
                }
            }
        }
    }

    // Atomically save an attempt and its unlock update. The separate text report is not in this transaction. | [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry caller); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (UUID and ending values).
    /*
     * Get/create the profile, then disable auto-commit. Insert the stable run UUID with ON
     * CONFLICT(id) DO NOTHING. Only a newly inserted COMPLETED result updates progress using MAX and
     * a five-night cap. Commit attempt plus unlock update together; rollback on SQLException, close
     * connection, then reload Progress. The earlier profile setup and later text append are outside
     * this transaction.
     * [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry caller);
     * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (UUID and ending values);
     * [persistence/ShiftLogStore.java:61 (append)](ShiftLogStore.java#L61) (separate report write afterward).
     */
    public Progress saveRun(String name, RunRecord run) throws SQLException {
        try (Connection connection = connect()) {
            long profileId = profile(connection, name);
            connection.setAutoCommit(false);
            try {
                // A UUID primary key makes retries idempotent: the same run is inserted at most once. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry caller); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (UUID and ending values).
                int inserted;
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO night_runs(id,profile_id,night,elapsed_seconds,power_remaining,outcome,started_at,ended_at)
                        VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING
                        """)) {
                    insert.setString(1, run.id());
                    insert.setLong(2, profileId);
                    insert.setInt(3, run.night());
                    insert.setDouble(4, run.elapsedSeconds());
                    insert.setDouble(5, run.powerRemaining());
                    insert.setString(6, run.outcome().name());
                    insert.setString(7, run.startedAt().toString());
                    insert.setString(8, run.endedAt().toString());
                    inserted = insert.executeUpdate();
                }
                // Only normal shift survival advances progress. MINIGAME_COMPLETED is recorded | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry caller); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (UUID and ending values).
                // as an attempt but deliberately does not unlock the next night. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry caller); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (UUID and ending values).
                // MAX preserves unlocks on earlier-night replays. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry caller); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (UUID and ending values).
                if (inserted > 0 && run.outcome() == Outcome.COMPLETED) {
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE progress SET highest_completed=MAX(highest_completed,?),
                                next_night=MAX(next_night,?), updated_at=? WHERE profile_id=?
                            """)) {
                        update.setInt(1, run.night());
                        update.setInt(2, Math.min(5, run.night() + 1));
                        update.setString(3, run.endedAt().toString());
                        update.setLong(4, profileId);
                        update.executeUpdate();
                    }
                }
                // Both attempt and progress changes become permanent together. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry caller); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (UUID and ending values).
                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        }
        return loadProgress(name);
    }

    // Return the newest 200 attempts for this player; older rows remain stored in SQLite. | [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:319 (page)](../Application.java#L319) (background query caller); [ui/RecordsPanel.java:55 (RecordsPanel)](../ui/RecordsPanel.java#L55) (displays returned rows).
    /*
     * Bind this profile ID into a SELECT ordered by ended_at DESC LIMIT 200. Reconstruct typed
     * RunRecord/Outcome/Instant values and return an unmodifiable list. Older rows stay stored and
     * still contribute to progress counts.
     * [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:319 (page)](../Application.java#L319) (background query caller);
     * [ui/RecordsPanel.java:55 (RecordsPanel)](../ui/RecordsPanel.java#L55) (displays returned rows);
     * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (reconstructed row value).
     */
    public List<RunRecord> loadRuns(String name) throws SQLException {
        List<RunRecord> runs = new ArrayList<>();
        try (Connection connection = connect()) {
            long id = profile(connection, name);
            try (PreparedStatement query = connection.prepareStatement("SELECT * FROM night_runs WHERE profile_id=? ORDER BY ended_at DESC LIMIT 200")) {
                query.setLong(1, id);
                try (ResultSet row = query.executeQuery()) {
                    while (row.next()) {
                        // Convert SQL columns back into the game's immutable result representation. | [p. 32: Database](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:319 (page)](../Application.java#L319) (background query caller); [ui/RecordsPanel.java:55 (RecordsPanel)](../ui/RecordsPanel.java#L55) (displays returned rows).
                        runs.add(new RunRecord(row.getString("id"), row.getInt("night"), row.getDouble("elapsed_seconds"),
                                row.getDouble("power_remaining"), Outcome.valueOf(row.getString("outcome")),
                                Instant.parse(row.getString("started_at")), Instant.parse(row.getString("ended_at"))));
                    }
                }
            }
        }
        return List.copyOf(runs);
    }
}
