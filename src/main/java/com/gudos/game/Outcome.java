package com.gudos.game;

public enum Outcome {
    COMPLETED("Completed"), POWER_OUT("Power failure"), ABANDONED("Left shift");

    private final String label;

    Outcome(String label) { this.label = label; }
    public String label() { return label; }
}
