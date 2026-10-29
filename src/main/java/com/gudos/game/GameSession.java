package com.gudos.game;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Pure shift simulation, including power, room occupancy and Sir Gudo's encounters. */
public final class GameSession {
    // Time is measured in active seconds; power drain is percentage points per active second.
    public static final double SHIFT_SECONDS = 360;
    public static final double BASE_DRAIN = 0.085;
    public static final double MONITOR_DRAIN = 0.22;
    public static final double LIGHT_DRAIN = 0.20;

    // Stable identity for this attempt; persistence uses it to avoid duplicate saves.
    private final String id = UUID.randomUUID().toString();
    private final Instant startedAt = Instant.now();
    private final int night;
    private final double duration;
    private final GudoEncounter gudo;
    private final Map<Scene, String> sceneStates = new EnumMap<>(Scene.class);
    private double elapsed;
    private double power = 100;
    private boolean paused;
    private boolean monitor;
    private boolean light;
    private double officePan = 0.5;
    private int camera = 1;
    // Null means the shift is ongoing. A non-null immutable result marks it as finished.
    private RunRecord result;

    public GameSession(int night) { this(night, SHIFT_SECONDS); }

    // The custom duration allows tests to exercise a whole shift without waiting six minutes.
    public GameSession(int night, double duration) {
        if (night < 1 || night > 5) throw new IllegalArgumentException("Night must be 1–5");
        if (!Double.isFinite(duration) || duration <= 0) throw new IllegalArgumentException("Invalid duration");
        this.night = night;
        this.duration = duration;
        gudo = new GudoEncounter(night);
    }

    // Application calls this with elapsed time. This class applies rules without drawing any UI.
    public void update(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid elapsed time");
        if (paused || finished()) return;
        double remaining = seconds;
        while (remaining > 0 && !finished()) {
            double untilMorning = duration - elapsed;
            double untilFailure = power / drainPerSecond();
            // Merely holding F is insufficient: the monitor must be down and the view must face Gudo.
            boolean illuminated = light && !monitor && gudo.approach().faces(officePan);
            // Stop at the nearest deadline, then process the rest of the frame in another step.
            // This prevents a long update from skipping an enemy arrival, retreat, or attack.
            double step = Math.min(remaining, Math.min(gudo.untilEvent(illuminated), Math.min(untilMorning, untilFailure)));
            elapsed += step;
            power = Math.max(0, power - drainPerSecond() * step);
            remaining = Math.max(0, remaining - step);
            // Morning wins a simultaneous deadline, then equipment failure, then an attack.
            if (step >= untilMorning) finish(Outcome.COMPLETED);
            else if (step >= untilFailure) finish(Outcome.POWER_OUT);
            else {
                gudo.advance(step, illuminated);
                if (gudo.caught()) finish(Outcome.CAUGHT);
            }
        }
    }

    // Freeze the outcome once. Application later writes this snapshot to SQLite and the text log.
    private void finish(Outcome outcome) {
        if (result != null) return;
        result = new RunRecord(id, night, elapsed, power, outcome, startedAt, Instant.now());
        monitor = false;
        light = false;
    }

    // Pausing preserves the shift in memory; abandoning ends it and produces a result.
    public void abandon() { finish(Outcome.ABANDONED); }
    public void pause() { if (!finished()) { paused = true; light = false; } }
    public void resume() { if (!finished()) paused = false; }

    // Monitor and flashlight are mutually exclusive, keeping their gameplay and drain consistent.
    public void toggleMonitor() {
        if (paused || finished()) return;
        monitor = !monitor;
        light = false;
    }

    public void setLight(boolean on) {
        if (!on) light = false;
        else if (!paused && !finished() && !monitor) light = true;
    }

    // Validate the camera number even if switching is currently blocked by pause/monitor state.
    public void selectCamera(int camera) {
        Scene.camera(camera);
        if (!paused && !finished() && monitor) this.camera = camera;
    }

    /** Optional room states; active enemy occupancy takes priority. */
    public void setSceneState(Scene scene, String state) {
        if (scene == null || state == null || state.isBlank()) throw new IllegalArgumentException("Invalid scene state");
        sceneStates.put(scene, state);
    }

    // Choose the image-state key: enemy occupancy overrides optional custom room states.
    public String sceneState(Scene scene) {
        if (gudo.location() == scene) return scene == Scene.OFFICE ? gudo.approach().officeState() : "occupied";
        return sceneStates.getOrDefault(scene, "default");
    }
    // Normalize the view direction: 0 = left edge, 0.5 = center, 1 = right edge.
    public void setOfficePan(double pan) {
        if (!Double.isFinite(pan)) throw new IllegalArgumentException("Invalid office pan");
        if (!paused && !finished()) officePan = Math.max(0, Math.min(1, pan));
    }
    // Read access for the UI; power usage bars are an indicator, not the actual drain formula.
    public GudoEncounter gudo() { return gudo; }
    public double drainPerSecond() { return BASE_DRAIN + (monitor ? MONITOR_DRAIN : 0) + (light ? LIGHT_DRAIN : 0); }
    public int usage() { return 1 + (monitor ? 2 : 0) + (light ? 2 : 0); }
    public int night() { return night; }
    public double elapsed() { return elapsed; }
    public double progress() { return Math.min(1, elapsed / duration); }
    public double power() { return power; }
    public boolean paused() { return paused; }
    public boolean monitorOpen() { return monitor; }
    public boolean lightOn() { return light; }
    public int camera() { return camera; }
    public boolean finished() { return result != null; }
    // Callers must wait until the shift ends; there is no final record during active gameplay.
    public RunRecord result() {
        if (!finished()) throw new IllegalStateException("Shift is still running");
        return result;
    }

    // Convert shift completion into the fictional 12 AM-6 AM clock shown in the HUD.
    public String clockLabel() {
        int hour = Math.min(6, (int) (progress() * 6));
        return (hour == 0 ? 12 : hour) + " AM";
    }

    // The pause screen includes fictional minutes instead of only the current hour.
    public String preciseClock() {
        int totalMinutes = Math.min(360, (int) (progress() * 360));
        int hour = totalMinutes / 60;
        return "%02d:%02d AM".formatted(hour == 0 ? 12 : hour, totalMinutes % 60);
    }
}
