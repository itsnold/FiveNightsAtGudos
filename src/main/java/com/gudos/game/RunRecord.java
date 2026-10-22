package com.gudos.game;

import java.time.Instant;

public record RunRecord(String id, int night, double elapsedSeconds, double powerRemaining,
                        Outcome outcome, Instant startedAt, Instant endedAt) {
    public String durationLabel() {
        int seconds = (int) Math.round(elapsedSeconds);
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }
}
