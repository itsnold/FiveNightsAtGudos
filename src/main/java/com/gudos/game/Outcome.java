package com.gudos.game;

/** Stored enum names remain stable in SQLite; labels are the player-facing descriptions. */
public enum Outcome {
    COMPLETED("Completed"), POWER_OUT("Power failure"), ABANDONED("Left shift"), CAUGHT("Caught by Sir Gudo");

    private final String label;

    Outcome(String label) { this.label = label; }
    public String label() { return label; }
}
