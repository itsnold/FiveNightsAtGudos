package com.gudos.persistence;

import com.gudos.game.GameSession;
import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    @TempDir Path directory;
    private Database database;

    @BeforeEach void create() throws SQLException { database = new Database(directory.resolve("test.db")); }

    private RunRecord completed(int night) {
        GameSession game = new GameSession(night, 2);
        game.update(2);
        return game.result();
    }

    @Test void createsProfileAndPersistsAcrossNewConnections() throws Exception {
        assertEquals(Progress.EMPTY, database.loadProgress("Night Guard"));
        RunRecord run = completed(1);
        Progress progress = database.saveRun("Night Guard", run);
        assertEquals(2, progress.nextNight());
        assertEquals(1, progress.highestCompleted());
        Database reopened = new Database(directory.resolve("test.db"));
        assertEquals(progress, reopened.loadProgress("Night Guard"));
        assertEquals(run, reopened.loadRuns("Night Guard").get(0));
    }

    @Test void retriesAreIdempotentAndReplaysDoNotRelockNights() throws Exception {
        RunRecord first = completed(1);
        database.saveRun("Player", first);
        database.saveRun("Player", first);
        assertEquals(1, database.loadProgress("Player").totalRuns());
        database.saveRun("Player", completed(2));
        database.saveRun("Player", completed(1));
        assertEquals(3, database.loadProgress("Player").nextNight());
        assertEquals(2, database.loadProgress("Player").highestCompleted());
        assertEquals(3, database.loadRuns("Player").size());
    }

    @Test void onlySuccessfulShiftsUnlockProgress() throws Exception {
        GameSession game = new GameSession(1);
        game.update(10);
        game.abandon();
        Progress progress = database.saveRun("Player", game.result());
        assertEquals(1, progress.nextNight());
        assertEquals(0, progress.completedRuns());
        assertEquals(1, progress.totalRuns());
        assertEquals(Outcome.ABANDONED, database.loadRuns("Player").get(0).outcome());
    }

    @Test void separateProfilesAndQuotedNamesAreSafe() throws Exception {
        String unusual = "O'Brien'); DROP TABLE profiles;--";
        database.saveRun(unusual, completed(1));
        assertEquals(1, database.loadProgress(unusual).totalRuns());
        assertEquals(Progress.EMPTY, database.loadProgress("Somebody Else"));
        assertEquals(1, database.loadProgress(unusual.toLowerCase(java.util.Locale.ROOT)).totalRuns());
    }

    @Test void failedInsertDoesNotAdvanceProgress() throws Exception {
        RunRecord invalid = new RunRecord(UUID.randomUUID().toString(), 0, 10, 80, Outcome.COMPLETED, Instant.now(), Instant.now());
        assertThrows(SQLException.class, () -> database.saveRun("Player", invalid));
        assertEquals(Progress.EMPTY, database.loadProgress("Player"));
        assertTrue(database.loadRuns("Player").isEmpty());
    }

    @Test void fifthNightDoesNotUnlockANonexistentSixth() throws Exception {
        database.saveRun("Player", completed(5));
        Progress progress = database.loadProgress("Player");
        assertEquals(5, progress.nextNight());
        assertFalse(progress.unlocked(6));
    }
}
