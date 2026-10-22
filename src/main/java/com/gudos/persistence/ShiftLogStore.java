package com.gudos.persistence;

import com.gudos.game.RunRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.stream.Stream;

/** Append-only human-readable reports, separate from authoritative SQLite progress. */
public final class ShiftLogStore {
    private final Path file;

    public ShiftLogStore(Path file) { this.file = file; }

    public void append(String profile, RunRecord run) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        String marker = "run=" + run.id() + " |";
        if (Files.exists(file)) {
            try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
                if (lines.anyMatch(line -> line.contains(marker))) return;
            }
        }
        String line = String.format(Locale.ROOT,
                "%s | run=%s | player=%s | night=%d | result=%s | duration=%s | power=%.1f%%%n",
                run.endedAt(), run.id(), profile.replaceAll("\\p{Cntrl}", ""), run.night(),
                run.outcome().label(), run.durationLabel(), run.powerRemaining());
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
