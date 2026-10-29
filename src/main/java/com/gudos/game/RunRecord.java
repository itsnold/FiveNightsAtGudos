package com.gudos.game;

import java.time.Instant;

/** Immutable result of one ended attempt. Database links it to a profile when saving. */
public record RunRecord(String id, int night, double elapsedSeconds, double powerRemaining,
                        Outcome outcome, Instant startedAt, Instant endedAt) {
    public String durationLabel() {
        // Display actual active play time as minutes:seconds, not the fictional in-game clock.
        int seconds = (int) Math.round(elapsedSeconds);
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }
}
