package com.gudos.game;

/** Stored enum names remain stable in SQLite; labels are the player-facing descriptions. Reviewer (42-page edition): [p. 27: Outcome](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27). */
/*
 * Outcome
 *
 * Purpose: Defines the six final result categories stored in SQLite.
 *
 * Execution flow: GameSession.finish selects the enum; Database stores name(); UI/logs display
 * label().
 *
 * Design rationale: Stable machine names separate storage from readable labels.
 * Only COMPLETED advances normal-night progress; MINIGAME_COMPLETED is a different outcome.
 *
 * State: COMPLETED, POWER_OUT, ABANDONED, CAUGHT, MINIGAME_FAILED, MINIGAME_COMPLETED.
 *
 * Summary: Caught, power failure, leaving early and secret results all count as attempts;
 * only normal survival unlocks nights.
 * Reviewer (42-page edition): [p. 27: Outcome](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27).
 */
public enum Outcome {
    COMPLETED("Completed"), POWER_OUT("Power failure"), ABANDONED("Left shift"), CAUGHT("Caught by Sir Gudo"), MINIGAME_FAILED("Secret failed"), MINIGAME_COMPLETED("Secret completed");

    private final String label;

    /*
     * Store the readable label alongside the stable enum name. SQL uses the name; UI and text
     * reports use the label.
     * Reviewer (42-page edition): [p. 27: Outcome](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27).
     */
    Outcome(String label) { this.label = label; }
    /*
     * Read the readable UI label. This accessor does not advance time, save data or change the
     * model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 27: Outcome](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27).
     */
    public String label() { return label; }
}
