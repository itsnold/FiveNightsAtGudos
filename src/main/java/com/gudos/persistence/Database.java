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
        url = "jdbc:sqlite:" + file.toAbsolutePath();
        try (Connection connection = connect(); Statement sql = connection.createStatement()) {
            sql.execute("PRAGMA journal_mode=WAL");
            try (ResultSet version = sql.executeQuery("PRAGMA user_version")) {
                if (version.next() && version.getInt(1) > 1) throw new SQLException("This database belongs to a newer game version.");
            }
            sql.execute("""
                    CREATE TABLE IF NOT EXISTS profiles (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        created_at TEXT NOT NULL
                    )
                    """);
            sql.execute("""
                    CREATE TABLE IF NOT EXISTS progress (
                        profile_id INTEGER PRIMARY KEY REFERENCES profiles(id),
                        next_night INTEGER NOT NULL DEFAULT 1 CHECK(next_night BETWEEN 1 AND 5),
                        highest_completed INTEGER NOT NULL DEFAULT 0 CHECK(highest_completed BETWEEN 0 AND 5),
                        updated_at TEXT NOT NULL
                    )
                    """);
            sql.execute("""
                    CREATE TABLE IF NOT EXISTS night_runs (
                        id TEXT PRIMARY KEY,
                        profile_id INTEGER NOT NULL REFERENCES profiles(id),
                        night INTEGER NOT NULL CHECK(night BETWEEN 1 AND 5),
                        elapsed_seconds REAL NOT NULL CHECK(elapsed_seconds >= 0),
                        power_remaining REAL NOT NULL CHECK(power_remaining BETWEEN 0 AND 100),
                        outcome TEXT NOT NULL CHECK(outcome IN ('COMPLETED','POWER_OUT','ABANDONED')),
                        started_at TEXT NOT NULL,
                        ended_at TEXT NOT NULL
                    )
                    """);
            sql.execute("CREATE INDEX IF NOT EXISTS runs_by_profile ON night_runs(profile_id, ended_at DESC)");
            sql.execute("PRAGMA user_version=1");
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

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

    public Progress saveRun(String name, RunRecord run) throws SQLException {
        try (Connection connection = connect()) {
            long profileId = profile(connection, name);
            connection.setAutoCommit(false);
            try {
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
                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        }
        return loadProgress(name);
    }

    public List<RunRecord> loadRuns(String name) throws SQLException {
        List<RunRecord> runs = new ArrayList<>();
        try (Connection connection = connect()) {
            long id = profile(connection, name);
            try (PreparedStatement query = connection.prepareStatement("SELECT * FROM night_runs WHERE profile_id=? ORDER BY ended_at DESC LIMIT 200")) {
                query.setLong(1, id);
                try (ResultSet row = query.executeQuery()) {
                    while (row.next()) {
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
