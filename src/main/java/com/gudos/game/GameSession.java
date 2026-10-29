package com.gudos.game;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Pure shift simulation, including power, room occupancy and Sir Gudo's encounters. */
public final class GameSession {
    public static final double SHIFT_SECONDS = 360;
    public static final double BASE_DRAIN = 0.085;
    public static final double MONITOR_DRAIN = 0.22;
    public static final double LIGHT_DRAIN = 0.20;

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
    private RunRecord result;

    public GameSession(int night) { this(night, SHIFT_SECONDS); }

    public GameSession(int night, double duration) {
        if (night < 1 || night > 5) throw new IllegalArgumentException("Night must be 1–5");
        if (!Double.isFinite(duration) || duration <= 0) throw new IllegalArgumentException("Invalid duration");
        this.night = night;
        this.duration = duration;
        gudo = new GudoEncounter(night);
    }

    public void update(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid elapsed time");
        if (paused || finished()) return;
        double remaining = seconds;
        while (remaining > 0 && !finished()) {
            double untilMorning = duration - elapsed;
            double untilFailure = power / drainPerSecond();
            boolean illuminated = light && !monitor && gudo.approach().faces(officePan);
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

    private void finish(Outcome outcome) {
        if (result != null) return;
        result = new RunRecord(id, night, elapsed, power, outcome, startedAt, Instant.now());
        monitor = false;
        light = false;
    }

    public void abandon() { finish(Outcome.ABANDONED); }
    public void pause() { if (!finished()) { paused = true; light = false; } }
    public void resume() { if (!finished()) paused = false; }

    public void toggleMonitor() {
        if (paused || finished()) return;
        monitor = !monitor;
        light = false;
    }

    public void setLight(boolean on) {
        if (!on) light = false;
        else if (!paused && !finished() && !monitor) light = true;
    }

    public void selectCamera(int camera) {
        Scene.camera(camera);
        if (!paused && !finished() && monitor) this.camera = camera;
    }

    /** Optional room states; active enemy occupancy takes priority. */
    public void setSceneState(Scene scene, String state) {
        if (scene == null || state == null || state.isBlank()) throw new IllegalArgumentException("Invalid scene state");
        sceneStates.put(scene, state);
    }

    public String sceneState(Scene scene) {
        if (gudo.location() == scene) return scene == Scene.OFFICE ? gudo.approach().officeState() : "occupied";
        return sceneStates.getOrDefault(scene, "default");
    }
    public void setOfficePan(double pan) {
        if (!Double.isFinite(pan)) throw new IllegalArgumentException("Invalid office pan");
        if (!paused && !finished()) officePan = Math.max(0, Math.min(1, pan));
    }
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
    public RunRecord result() {
        if (!finished()) throw new IllegalStateException("Shift is still running");
        return result;
    }

    public String clockLabel() {
        int hour = Math.min(6, (int) (progress() * 6));
        return (hour == 0 ? 12 : hour) + " AM";
    }

    public String preciseClock() {
        int totalMinutes = Math.min(360, (int) (progress() * 360));
        int hour = totalMinutes / 60;
        return "%02d:%02d AM".formatted(hour == 0 ? 12 : hour, totalMinutes % 60);
    }
}
