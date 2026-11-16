package com.gudos.persistence;

import com.gudos.game.RunRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.stream.Stream;

/** Append-only human-readable reports, separate from authoritative SQLite progress. [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 31: ShiftLogStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (appends after the database save); [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (record and UUID written to the report). */
/*
 * ShiftLogStore
 *
 * Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (appends after the database save);
 * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (record and UUID written to the report);
 * [persistence/AppPaths.java:92 (shiftLog)](AppPaths.java#L92) (report location);
 * [persistence/Database.java:261 (saveRun)](Database.java#L261) (separate authoritative attempt store).
 *
 * Purpose: Appends a readable one-line report for each ended attempt.
 *
 * Execution flow: Application.savePendingRun calls append after Database.saveRun.
 *
 * Design rationale: A run=UUID marker detects an already-written report on retry.
 * Locale.ROOT stabilizes decimal formatting; UTF-8 preserves names. This is best-effort
 * application-level deduplication, not a transactional database.
 *
 * State: One report line includes end time, UUID, sanitized name, night, outcome label,
 * actual duration and remaining power.
 *
 * Summary: The log is an audit-style readable report. Continue does not parse it. A scan of
 * existing lines avoids normal retry duplicates, but a process crash during append is not fully
 * atomic.
 * [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: ShiftLogStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
 */
public final class ShiftLogStore {
    private final Path file;

    /*
     * Retain the report path. append performs directory creation, retry-marker detection and writing
     * later.
     * [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: ShiftLogStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     * Related: [Application.java:119 (ShiftLogStore call)](../Application.java#L119) (caller of ShiftLogStore);
     * [game/RunRecord.java:29 (RunRecord)](../game/RunRecord.java#L29) (record and UUID written to the report);
     * [persistence/AppPaths.java:92 (shiftLog)](AppPaths.java#L92) (report location).
     */
    public ShiftLogStore(Path file) { this.file = file; }

    /*
     * Ensure the parent folder exists; scan existing UTF-8 lines for run=UUID plus delimiter and
     * return if already present. Sanitize the name, format a stable report with Locale.ROOT and
     * append one line. Database may already be committed when this fails; retry reuses the same
     * UUID.
     * [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: ShiftLogStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     * Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (calls after database save);
     * [game/RunRecord.java:39 (durationLabel)](../game/RunRecord.java#L39) (receipt duration formatting);
     * [persistence/Database.java:261 (saveRun)](Database.java#L261) (also recognizes the stable UUID).
     */
    public void append(String profile, RunRecord run) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        // Reuse the run ID as a marker so retrying after a partial save does not append twice. | [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 31: ShiftLogStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (calls after database save); [game/RunRecord.java:39 (durationLabel)](../game/RunRecord.java#L39) (receipt duration formatting).
        String marker = "run=" + run.id() + " |";
        if (Files.exists(file)) {
            try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
                if (lines.anyMatch(line -> line.contains(marker))) return;
            }
        }
        // Store one UTF-8 report line per attempt. This file is readable, not a progress checkpoint. | [p. 31: ShiftLogStore](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (calls after database save); [game/RunRecord.java:39 (durationLabel)](../game/RunRecord.java#L39) (receipt duration formatting).
        String line = String.format(Locale.ROOT,
                "%s | run=%s | player=%s | night=%d | result=%s | duration=%s | power=%.1f%%%n",
                run.endedAt(), run.id(), profile.replaceAll("\\p{Cntrl}", ""), run.night(),
                run.outcome().label(), run.durationLabel(), run.powerRemaining());
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
