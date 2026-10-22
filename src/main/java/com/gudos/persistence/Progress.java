package com.gudos.persistence;

public record Progress(int nextNight, int highestCompleted, int totalRuns, int completedRuns) {
    public static final Progress EMPTY = new Progress(1, 0, 0, 0);
    public boolean unlocked(int night) { return night >= 1 && night <= nextNight; }
}
