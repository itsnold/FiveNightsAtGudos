package com.gudos.game;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Pure shift simulation, including power, room occupancy and Sir Gudo's encounters. Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). */
/*
 * GameSession
 *
 * Purpose: Owns the UI-independent rules and state of one normal night attempt.
 *
 * Execution flow: Application.tick calls update; GamePanel requests monitor/light/camera/pan
 * changes; update drains power, advances encounter events and freezes one RunRecord at an ending.
 *
 * Design rationale: The simulation can run without a Swing window. It splits long
 * frames at deadlines and defines tie priority: morning, then power failure, then encounter attack.
 * UUID identity makes one attempt stable across save retries.
 *
 * State: elapsed is active seconds; power starts at 100; duration defaults to 360;
 * monitor/light are exclusive; officePan is normalized; camera starts at 1; result=null means
 * active; sceneStates stores optional visual overrides.
 *
 * Summary: GamePanel is the screen; GameSession is the rules. The model enforces
 * monitor/light exclusion even if the UI sends an invalid combination.
 * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
 */
public final class GameSession {
    // Time is measured in active seconds; power drain is percentage points per active second. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    public static final double SHIFT_SECONDS = 360;
    public static final double BASE_DRAIN = 0.085;
    public static final double MONITOR_DRAIN = 0.22;
    public static final double LIGHT_DRAIN = 0.20;

    // Stable identity for this attempt; persistence uses it to avoid duplicate saves. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
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
    // Null means the shift is ongoing. A non-null immutable result marks it as finished. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    private RunRecord result;

    /*
     * The one-night overload delegates to the duration overload with 360 seconds. The full
     * constructor rejects a night outside 1..5 and nonfinite/nonpositive duration, then creates one
     * GudoEncounter. Field initializers create UUID/start time, power100, centered pan and camera1.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public GameSession(int night) { this(night, SHIFT_SECONDS); }

    // A custom duration allows shorter or longer simulations without changing the clock rules. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * The one-night overload delegates to the duration overload with 360 seconds. The full
     * constructor rejects a night outside 1..5 and nonfinite/nonpositive duration, then creates one
     * GudoEncounter. Field initializers create UUID/start time, power100, centered pan and camera1.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public GameSession(int night, double duration) {
        if (night < 1 || night > 5) throw new IllegalArgumentException("Night must be 1–5");
        if (!Double.isFinite(duration) || duration <= 0) throw new IllegalArgumentException("Invalid duration");
        this.night = night;
        this.duration = duration;
        gudo = new GudoEncounter(night);
    }

    // Application calls this with elapsed time. This class applies rules without drawing any UI. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Validate a finite nonnegative delta; ignore paused/finished sessions. Repeatedly choose the
     * nearest remaining-frame, morning, power or encounter deadline. Advance elapsed/drain by that
     * step; finish morning first on a tie, then power, otherwise advance Gudo and test caught.
     * Splitting prevents a large delta from jumping over an arrival or attack.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void update(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid elapsed time");
        if (paused || finished()) return;
        double remaining = seconds;
        while (remaining > 0 && !finished()) {
            double untilMorning = duration - elapsed;
            double untilFailure = power / drainPerSecond();
            // Merely holding F is insufficient: the monitor must be down and the view must face Gudo. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
            boolean illuminated = light && !monitor && gudo.approach().faces(officePan);
            // Stop at the nearest deadline, then process the rest of the frame in another step. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
            // This prevents a long update from skipping an enemy arrival, retreat, or attack. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
            double step = Math.min(remaining, Math.min(gudo.untilEvent(illuminated), Math.min(untilMorning, untilFailure)));
            elapsed += step;
            power = Math.max(0, power - drainPerSecond() * step);
            remaining = Math.max(0, remaining - step);
            // Morning wins a simultaneous deadline, then equipment failure, then an attack. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
            if (step >= untilMorning) finish(Outcome.COMPLETED);
            else if (step >= untilFailure) finish(Outcome.POWER_OUT);
            else {
                gudo.advance(step, illuminated);
                if (gudo.caught()) finish(Outcome.CAUGHT);
            }
        }
    }

    // Freeze the outcome once. Application later writes this snapshot to SQLite and the text log. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Do nothing if result already exists. Otherwise create one RunRecord using this UUID/start
     * time/current elapsed/power/outcome, capture the end Instant, and turn off monitor and light.
     * The snapshot remains stable for retries.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    private void finish(Outcome outcome) {
        if (result != null) return;
        result = new RunRecord(id, night, elapsed, power, outcome, startedAt, Instant.now());
        monitor = false;
        light = false;
    }

    // Pausing preserves the shift in memory; abandoning ends it and produces a result. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Finish once with ABANDONED. It creates history but does not represent normal survival.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void abandon() { finish(Outcome.ABANDONED); }
    // Secret outcomes close this attempt without marking the night as normally survived. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Finish once with MINIGAME_COMPLETED, a separate result from COMPLETED. The database therefore
     * does not unlock the next night.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void completeSecret() { finish(Outcome.MINIGAME_COMPLETED); }
    /*
     * Finish once with MINIGAME_FAILED. Application still records it and starts a fresh attempt of
     * the same night after saving.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void failSecret() { finish(Outcome.MINIGAME_FAILED); }
    /*
     * If not finished, set paused and force light off. update then leaves elapsed/power/Gudo
     * unchanged; resume does not restore a held flashlight.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void pause() { if (!finished()) { paused = true; light = false; } }
    /*
     * If not finished, clear paused on the same in-memory session. It does not load a file or create
     * a checkpoint.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void resume() { if (!finished()) paused = false; }

    // Monitor and flashlight are mutually exclusive, keeping their gameplay and drain consistent. | Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Ignore paused/finished input. Toggle monitor, then clear light even when lowering it. This
     * enforces the equipment exclusion inside the model, independent of UI behavior.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void toggleMonitor() {
        if (paused || finished()) return;
        monitor = !monitor;
        light = false;
    }

    /*
     * Always allow switching light off. Allow switching on only for an unfinished, unpaused session
     * with monitor closed. A blocked request leaves the model authoritative.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void setLight(boolean on) {
        if (!on) light = false;
        else if (!paused && !finished() && !monitor) light = true;
    }

    // Validate the camera number even if switching is currently blocked by pause/monitor state. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Validate 1-6 through Scene.camera even when the action will otherwise be ignored. Store the
     * selection only while monitor is open and gameplay is active.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void selectCamera(int camera) {
        Scene.camera(camera);
        if (!paused && !finished() && monitor) this.camera = camera;
    }

    /** Optional room states; active enemy occupancy takes priority. Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). */
    /*
     * Reject null room or blank/null state, then store a custom visual override. This does not move
     * Gudo; real occupancy takes priority when sceneState is queried.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void setSceneState(Scene scene, String state) {
        if (scene == null || state == null || state.isBlank()) throw new IllegalArgumentException("Invalid scene state");
        sceneStates.put(scene, state);
    }

    // Choose the image-state key: enemy occupancy overrides optional custom room states. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * If Gudo occupies this room, return the appropriate office direction state or occupied camera
     * state. Otherwise return a custom override or default. The renderer uses the key; it does not
     * infer enemy location from image pixels.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public String sceneState(Scene scene) {
        if (gudo.location() == scene) return scene == Scene.OFFICE ? gudo.approach().officeState() : "occupied";
        return sceneStates.getOrDefault(scene, "default");
    }
    // Normalize the view direction: 0 = left edge, 0.5 = center, 1 = right edge. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Reject nonfinite values, then clamp pan into 0..1 only while active. The encounter uses this
     * normalized direction to decide whether the flashlight actually faces its door.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public void setOfficePan(double pan) {
        if (!Double.isFinite(pan)) throw new IllegalArgumentException("Invalid office pan");
        if (!paused && !finished()) officePan = Math.max(0, Math.min(1, pan));
    }
    // Read access for the UI; power usage bars are an indicator, not the actual drain formula. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Read the encounter model owned by this session. This accessor does not advance time, save data
     * or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public GudoEncounter gudo() { return gudo; }
    /*
     * Return 0.085 base drain plus 0.22 for the monitor or 0.20 for light. The model prevents
     * simultaneous monitor/light. These are percentage points per active second, not the usage-bar
     * count.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public double drainPerSecond() { return BASE_DRAIN + (monitor ? MONITOR_DRAIN : 0) + (light ? LIGHT_DRAIN : 0); }
    /*
     * Return one base bar plus two for each active device. Under normal rules the possible values
     * are 1 or 3; the bars summarize usage and are not the actual percentage drain formula.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public int usage() { return 1 + (monitor ? 2 : 0) + (light ? 2 : 0); }
    /*
     * Read the difficulty night of this attempt. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public int night() { return night; }
    /*
     * Read active simulation seconds, excluding pause time. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public double elapsed() { return elapsed; }
    /*
     * Return elapsed/duration capped at one. The normalized fraction drives the clock, late-shift
     * audio and progress display.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public double progress() { return Math.min(1, elapsed / duration); }
    /*
     * Read unrounded remaining power percentage. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public double power() { return power; }
    /*
     * Read whether the model currently ignores updates/input. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public boolean paused() { return paused; }
    /*
     * Read whether CCTV is raised. This accessor does not advance time, save data or change the
     * model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public boolean monitorOpen() { return monitor; }
    /*
     * Read whether the accepted flashlight state is on. This accessor does not advance time, save
     * data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public boolean lightOn() { return light; }
    /*
     * Read the selected camera number. This accessor does not advance time, save data or change the
     * model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public int camera() { return camera; }
    /*
     * Return whether a nonnull RunRecord exists. A frozen final record, rather than a second mutable
     * finished flag, is the ending marker.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public boolean finished() { return result != null; }
    // Callers must wait until the shift ends; there is no final record during active gameplay. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Reject a request before the attempt ends; after finishing return the same immutable record.
     * Application must wait for finished() before obtaining a final result.
     * Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public RunRecord result() {
        if (!finished()) throw new IllegalStateException("Shift is still running");
        return result;
    }

    // Convert shift completion into the fictional 12 AM-6 AM clock shown in the HUD. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Map progress 0..1 onto six fictional hours using integer truncation. Zero is displayed as 12
     * AM; full duration is 6 AM. One real minute corresponds to one displayed hour in a normal
     * six-minute shift.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public String clockLabel() {
        int hour = Math.min(6, (int) (progress() * 6));
        return (hour == 0 ? 12 : hour) + " AM";
    }

    // The pause screen includes fictional minutes instead of only the current hour. | Reviewer (42-page edition): [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
    /*
     * Map progress onto 360 fictional minutes and format hour/minute for pause. This is a fictional
     * 12:00-06:00 AM clock, not the actual wall clock or the RunRecord duration.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     */
    public String preciseClock() {
        int totalMinutes = Math.min(360, (int) (progress() * 360));
        int hour = totalMinutes / 60;
        return "%02d:%02d AM".formatted(hour == 0 ? 12 : hour, totalMinutes % 60);
    }
}
