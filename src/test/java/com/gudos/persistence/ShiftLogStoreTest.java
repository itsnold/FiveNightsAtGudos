package com.gudos.persistence;

import com.gudos.game.GameSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ShiftLogStoreTest {
    @TempDir Path directory;

    @Test void reportsAppendAndRetriesDoNotDuplicate() throws Exception {
        Path file = directory.resolve("shift-log.txt");
        ShiftLogStore store = new ShiftLogStore(file);
        GameSession first = new GameSession(1, 2);
        first.update(2);
        store.append("Guard ñ", first.result());
        store.append("Guard ñ", first.result());
        GameSession second = new GameSession(2);
        second.update(15);
        second.abandon();
        store.append("Guard ñ", second.result());
        assertEquals(2, Files.readAllLines(file).size());
        String text = Files.readString(file);
        assertTrue(text.contains("player=Guard ñ"));
        assertTrue(text.contains("result=Completed"));
        assertTrue(text.contains("result=Left shift"));
        assertTrue(text.contains("duration=00:15"));
    }
}
