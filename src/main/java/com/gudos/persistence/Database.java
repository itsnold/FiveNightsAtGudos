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

/** SQLite via JDBC. Connections are short-lived and all calls belong on the IO executor. */
public final class Database {
    private final String url;

    public Database(Path file) throws SQLException {
        // SQLite is an embedded database file; there is no separate database server to connect to.
        url = "jdbc:sqlite:" + file.toAbsolutePath();
        try (Connection connection = connect(); Statement sql = connection.createStatement()) {
            // Write-ahead logging lets SQLite coordinate reads with committed writes.
            sql.execute("PRAGMA journal_mode=WAL");
            int schemaVersion;
            try (ResultSet version = sql.executeQuery("PRAGMA user_version")) {
                schemaVersion = version.next() ? version.getInt(1) : 0;
                if (schemaVersion > 2) throw new SQLException("This database belongs to a newer game version.");
            }
            // profiles identifies a player by a unique, case-insensitive local name.
            sql.execute("""
                    CREATE TABLE IF NOT EXISTS profiles (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        created_at TEXT NOT NULL
                    )
                    """);
            // One progress row per profile holds unlock state, not individual attempt history.
            sql.execute("""
                    CREATE TABLE IF NOT EXISTS progress (
                        profile_id INTEGER PRIMARY KEY REFERENCES profiles(id),
                        next_night INTEGER NOT NULL DEFAULT 1 CHECK(next_night BETWEEN 1 AND 5),
                        highest_completed INTEGER NOT NULL DEFAULT 0 CHECK(highest_completed BETWEEN 0 AND 5),
                        updated_at TEXT NOT NULL
                    )
                    """);
            sql.execute(runsTable("night_runs"));
            // Version 2 adds CAUGHT as an allowed outcome. Rebuild/copy the old constrained table
            // inside a transaction so a migration failure can restore the previous schema/data.
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
            // Match the records screen's lookup: one profile, most recent end time first.
            sql.execute("CREATE INDEX IF NOT EXISTS runs_by_profile ON night_runs(profile_id, ended_at DESC)");
            sql.execute("PRAGMA user_version=2");
        }
    }

    // Each night_runs row is a RunRecord linked to profiles.id through profile_id.
    // The table argument is an internal schema name, never supplied by a player.
    private static String runsTable(String table) {
        return """
                    CREATE TABLE IF NOT EXISTS %s (
                        id TEXT PRIMARY KEY,
                        profile_id INTEGER NOT NULL REFERENCES profiles(id),
                        night INTEGER NOT NULL CHECK(night BETWEEN 1 AND 5),
                        elapsed_seconds REAL NOT NULL CHECK(elapsed_seconds >= 0),
                        power_remaining REAL NOT NULL CHECK(power_remaining BETWEEN 0 AND 100),
                        outcome TEXT NOT NULL CHECK(outcome IN ('COMPLETED','POWER_OUT','ABANDONED','CAUGHT')),
                        started_at TEXT NOT NULL,
                        ended_at TEXT NOT NULL
                    )
                    """.formatted(table);
    }

    // Each operation opens/closes its own connection. SQLite needs foreign keys enabled per connection.
    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            // Wait up to five seconds for a lock instead of immediately failing a busy write.
            statement.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    // Select or create the local profile, then ensure it has a default progress row.
    // SQL placeholders bind values separately from SQL syntax, including names with quotes.
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

    // Unlock fields come from progress; attempt/completion counts are calculated from night_runs.
    // Loading a name can create its profile if this is the first time it is used.
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

    // Atomically save an attempt and its unlock update. The separate text report is not in this transaction.
    public Progress saveRun(String name, RunRecord run) throws SQLException {
        try (Connection connection = connect()) {
            long profileId = profile(connection, name);
            connection.setAutoCommit(false);
            try {
                // A UUID primary key makes retries idempotent: the same run is inserted at most once.
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
                // Only a newly saved win advances progress. MAX preserves unlocks on earlier-night replays.
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
                // Both attempt and progress changes become permanent together.
                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        }
        return loadProgress(name);
    }

    // Return the newest 200 attempts for this player; older rows remain stored in SQLite.
    public List<RunRecord> loadRuns(String name) throws SQLException {
        List<RunRecord> runs = new ArrayList<>();
        try (Connection connection = connect()) {
            long id = profile(connection, name);
            try (PreparedStatement query = connection.prepareStatement("SELECT * FROM night_runs WHERE profile_id=? ORDER BY ended_at DESC LIMIT 200")) {
                query.setLong(1, id);
                try (ResultSet row = query.executeQuery()) {
                    while (row.next()) {
                        // Convert SQL columns back into the game's immutable result representation.
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
