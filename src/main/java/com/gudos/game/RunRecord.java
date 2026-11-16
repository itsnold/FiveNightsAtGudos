package com.gudos.game;

import java.time.Instant;

/** Immutable result of one ended attempt. Database links it to a profile when saving. Reviewer (42-page edition): [p. 26: RunRecord](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26). */
/*
 * RunRecord
 *
 * Purpose: Carries the immutable snapshot of one ended attempt.
 *
 * Execution flow: GameSession creates it once; Application retains it; Database and ShiftLogStore
 * save the same ID and values; RecordsPanel displays it.
 *
 * Design rationale: A record supplies value equality and accessors. Capture final
 * values once so retries cannot silently turn the attempt into a new one.
 *
 * State: id, night, elapsedSeconds, powerRemaining, outcome, startedAt and endedAt.
 * durationLabel uses actual active seconds, not the fictional clock.
 *
 * Summary: The record is the receipt for an attempt, not a resumable checkpoint. Its UUID
 * identifies the attempt independently of the player profile.
 * Reviewer (42-page edition): [p. 26: RunRecord](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
 */
public record RunRecord(String id, int night, double elapsedSeconds, double powerRemaining,
                        Outcome outcome, Instant startedAt, Instant endedAt) {
    /*
     * Round active elapsed seconds to the nearest whole second and format minutes:seconds. A paused
     * 18-second attempt displays 00:18 even if more real wall time passed.
     * Reviewer (42-page edition): [p. 26: RunRecord](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
     */
    public String durationLabel() {
        // Display actual active play time as minutes:seconds, not the fictional in-game clock. | Reviewer (42-page edition): [p. 26: RunRecord](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-26).
        int seconds = (int) Math.round(elapsedSeconds);
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }
}
