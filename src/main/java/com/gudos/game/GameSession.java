package com.gudos.game;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Pure shift simulation, including power, room occupancy and Sir Gudo's encounters. [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). Related: [Application.java:215 (tick)](../Application.java#L215) (advances the simulation); [ui/GamePanel.java:108 (GamePanel)](../ui/GamePanel.java#L108) (sends input and reads state). */
/*
 * GameSession
 *
 * Related: [Application.java:215 (tick)](../Application.java#L215) (advances the simulation);
 * [ui/GamePanel.java:108 (GamePanel)](../ui/GamePanel.java#L108) (sends input and reads state);
 * [game/GudoEncounter.java:153 (advance)](GudoEncounter.java#L153) (enemy rules);
 * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (persists the final record).
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
 * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
 */
public final class GameSession {
    // Time is measured in active seconds; power drain is percentage points per active second. | [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (advances the simulation); [ui/GamePanel.java:108 (GamePanel)](../ui/GamePanel.java#L108) (sends input and reads state).
    public static final double SHIFT_SECONDS = 360;
    public static final double BASE_DRAIN = 0.085;
    public static final double MONITOR_DRAIN = 0.22;
    public static final double LIGHT_DRAIN = 0.20;

    // Stable identity for this attempt; persistence uses it to avoid duplicate saves. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (advances the simulation); [ui/GamePanel.java:108 (GamePanel)](../ui/GamePanel.java#L108) (sends input and reads state).
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
    // Null means the shift is ongoing. A non-null immutable result marks it as finished. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (advances the simulation); [ui/GamePanel.java:108 (GamePanel)](../ui/GamePanel.java#L108) (sends input and reads state).
    private RunRecord result;

    /*
     * The one-night overload delegates to the duration overload with 360 seconds. The full
     * constructor rejects a night outside 1..5 and nonfinite/nonpositive duration, then creates one
     * GudoEncounter. Field initializers create UUID/start time, power100, centered pan and camera1.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:358 (startNight)](../Application.java#L358) (constructs a fresh attempt);
     * [game/GudoEncounter.java:125 (GudoEncounter)](GudoEncounter.java#L125) (owned enemy model).
     */
    public GameSession(int night) { this(night, SHIFT_SECONDS); }

    // A custom duration allows shorter or longer simulations without changing the clock rules. | [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:358 (startNight)](../Application.java#L358) (constructs a fresh attempt); [game/GudoEncounter.java:125 (GudoEncounter)](GudoEncounter.java#L125) (owned enemy model).
    /*
     * The one-night overload delegates to the duration overload with 360 seconds. The full
     * constructor rejects a night outside 1..5 and nonfinite/nonpositive duration, then creates one
     * GudoEncounter. Field initializers create UUID/start time, power100, centered pan and camera1.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:358 (startNight)](../Application.java#L358) (constructs a fresh attempt);
     * [game/GudoEncounter.java:125 (GudoEncounter)](GudoEncounter.java#L125) (owned enemy model).
     */
    public GameSession(int night, double duration) {
        if (night < 1 || night > 5) throw new IllegalArgumentException("Night must be 1–5");
        if (!Double.isFinite(duration) || duration <= 0) throw new IllegalArgumentException("Invalid duration");
        this.night = night;
        this.duration = duration;
        gudo = new GudoEncounter(night);
    }

    // Application calls this with elapsed time. This class applies rules without drawing any UI. | [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (per-frame caller); [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (next enemy deadline).
    /*
     * Validate a finite nonnegative delta; ignore paused/finished sessions. Repeatedly choose the
     * nearest remaining-frame, morning, power or encounter deadline. Advance elapsed/drain by that
     * step; finish morning first on a tie, then power, otherwise advance Gudo and test caught.
     * Splitting prevents a large delta from jumping over an arrival or attack.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:215 (tick)](../Application.java#L215) (per-frame caller);
     * [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (next enemy deadline);
     * [game/GudoEncounter.java:153 (advance)](GudoEncounter.java#L153) (advances enemy rules).
     */
    public void update(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid elapsed time");
        if (paused || finished()) return;
        double remaining = seconds;
        while (remaining > 0 && !finished()) {
            double untilMorning = duration - elapsed;
            double untilFailure = power / drainPerSecond();
            // Merely holding F is insufficient: the monitor must be down and the view must face Gudo. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (per-frame caller); [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (next enemy deadline).
            boolean illuminated = light && !monitor && gudo.approach().faces(officePan);
            // Stop at the nearest deadline, then process the rest of the frame in another step. | [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (per-frame caller); [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (next enemy deadline).
            // This prevents a long update from skipping an enemy arrival, retreat, or attack. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (per-frame caller); [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (next enemy deadline).
            double step = Math.min(remaining, Math.min(gudo.untilEvent(illuminated), Math.min(untilMorning, untilFailure)));
            elapsed += step;
            power = Math.max(0, power - drainPerSecond() * step);
            remaining = Math.max(0, remaining - step);
            // Morning wins a simultaneous deadline, then equipment failure, then an attack. | [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:215 (tick)](../Application.java#L215) (per-frame caller); [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (next enemy deadline).
            if (step >= untilMorning) finish(Outcome.COMPLETED);
            else if (step >= untilFailure) finish(Outcome.POWER_OUT);
            else {
                gudo.advance(step, illuminated);
                if (gudo.caught()) finish(Outcome.CAUGHT);
            }
        }
    }

    // Freeze the outcome once. Application later writes this snapshot to SQLite and the text log. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:538 (finishShift)](../Application.java#L538) (reads and presents the result); [game/RunRecord.java:29 (RunRecord)](RunRecord.java#L29) (immutable receipt).
    /*
     * Do nothing if result already exists. Otherwise create one RunRecord using this UUID/start
     * time/current elapsed/power/outcome, capture the end Instant, and turn off monitor and light.
     * The snapshot remains stable for retries.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:538 (finishShift)](../Application.java#L538) (reads and presents the result);
     * [game/RunRecord.java:29 (RunRecord)](RunRecord.java#L29) (immutable receipt);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (stable identity for retry).
     */
    private void finish(Outcome outcome) {
        if (result != null) return;
        result = new RunRecord(id, night, elapsed, power, outcome, startedAt, Instant.now());
        monitor = false;
        light = false;
    }

    // Pausing preserves the shift in memory; abandoning ends it and produces a result. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:516 (leaveShift)](../Application.java#L516) (early-menu departure caller); [Application.java:703 (quit)](../Application.java#L703) (orderly-exit caller).
    /*
     * Finish once with ABANDONED. It creates history but does not represent normal survival.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:516 (leaveShift)](../Application.java#L516) (early-menu departure caller);
     * [Application.java:703 (quit)](../Application.java#L703) (orderly-exit caller);
     * [game/Outcome.java:38 (Outcome)](Outcome.java#L38) (ABANDONED result definition).
     */
    public void abandon() { finish(Outcome.ABANDONED); }
    // Secret outcomes close this attempt without marking the night as normally survived. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:464 (finishSecret)](../Application.java#L464) (secret-ending caller); [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (secret results do not unlock normal nights).
    /*
     * Finish once with MINIGAME_COMPLETED, a separate result from COMPLETED. The database therefore
     * does not unlock the next night.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:464 (finishSecret)](../Application.java#L464) (secret-ending caller);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (secret results do not unlock normal nights).
     */
    public void completeSecret() { finish(Outcome.MINIGAME_COMPLETED); }
    /*
     * Finish once with MINIGAME_FAILED. Application still records it and starts a fresh attempt of
     * the same night after saving.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:464 (finishSecret)](../Application.java#L464) (secret-ending caller);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (secret results do not unlock normal nights).
     */
    public void failSecret() { finish(Outcome.MINIGAME_FAILED); }
    /*
     * If not finished, set paused and force light off. update then leaves elapsed/power/Gudo
     * unchanged; resume does not restore a held flashlight.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:482 (pauseShift)](../Application.java#L482) (normal pause coordinator);
     * [Application.java:500 (resumeShift)](../Application.java#L500) (normal resume coordinator);
     * [Application.java:408 (openSecret)](../Application.java#L408) (pauses the original shift).
     */
    public void pause() { if (!finished()) { paused = true; light = false; } }
    /*
     * If not finished, clear paused on the same in-memory session. It does not load a file or create
     * a checkpoint.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:482 (pauseShift)](../Application.java#L482) (normal pause coordinator);
     * [Application.java:500 (resumeShift)](../Application.java#L500) (normal resume coordinator);
     * [Application.java:408 (openSecret)](../Application.java#L408) (pauses the original shift).
     */
    public void resume() { if (!finished()) paused = false; }

    // Monitor and flashlight are mutually exclusive, keeping their gameplay and drain consistent. | [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [ui/GamePanel.java:327 (toggleMonitor)](../ui/GamePanel.java#L327) (input and accepted-change feedback); [Application.java:391 (updateGameAudio)](../Application.java#L391) (monitor or office ambience).
    /*
     * Ignore paused/finished input. Toggle monitor, then clear light even when lowering it. This
     * enforces the equipment exclusion inside the model, independent of UI behavior.
     * [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:327 (toggleMonitor)](../ui/GamePanel.java#L327) (input and accepted-change feedback);
     * [Application.java:391 (updateGameAudio)](../Application.java#L391) (monitor or office ambience).
     */
    public void toggleMonitor() {
        if (paused || finished()) return;
        monitor = !monitor;
        light = false;
    }

    /*
     * Always allow switching light off. Allow switching on only for an unfinished, unpaused session
     * with monitor closed. A blocked request leaves the model authoritative.
     * [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:311 (updateLight)](../ui/GamePanel.java#L311) (combines keyboard and mouse holds);
     * [game/GudoEncounter.java:153 (advance)](GudoEncounter.java#L153) (effective light changes exposure).
     */
    public void setLight(boolean on) {
        if (!on) light = false;
        else if (!paused && !finished() && !monitor) light = true;
    }

    // Validate the camera number even if switching is currently blocked by pause/monitor state. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [ui/GamePanel.java:348 (selectCamera)](../ui/GamePanel.java#L348) (camera-input caller); [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (draws selected feed).
    /*
     * Validate 1-6 through Scene.camera even when the action will otherwise be ignored. Store the
     * selection only while monitor is open and gameplay is active.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:348 (selectCamera)](../ui/GamePanel.java#L348) (camera-input caller);
     * [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (draws selected feed);
     * [game/Scene.java:82 (camera)](Scene.java#L82) (validates camera numbers).
     */
    public void selectCamera(int camera) {
        Scene.camera(camera);
        if (!paused && !finished() && monitor) this.camera = camera;
    }

    /** Optional room states; active enemy occupancy takes priority. [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (camera image-state client); [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (office image-state client). */
    /*
     * Reject null room or blank/null state, then store a custom visual override. This does not move
     * Gudo; real occupancy takes priority when sceneState is queried.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (camera image-state client);
     * [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (office image-state client);
     * [assets/AssetManager.java:113 (scene)](../assets/AssetManager.java#L113) (resolves image-state keys).
     */
    public void setSceneState(Scene scene, String state) {
        if (scene == null || state == null || state.isBlank()) throw new IllegalArgumentException("Invalid scene state");
        sceneStates.put(scene, state);
    }

    // Choose the image-state key: enemy occupancy overrides optional custom room states. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (camera image-state client); [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (office image-state client).
    /*
     * If Gudo occupies this room, return the appropriate office direction state or occupied camera
     * state. Otherwise return a custom override or default. The renderer uses the key; it does not
     * infer enemy location from image pixels.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (camera image-state client);
     * [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (office image-state client);
     * [assets/AssetManager.java:113 (scene)](../assets/AssetManager.java#L113) (resolves image-state keys).
     */
    public String sceneState(Scene scene) {
        if (gudo.location() == scene) return scene == Scene.OFFICE ? gudo.approach().officeState() : "occupied";
        return sceneStates.getOrDefault(scene, "default");
    }
    // Normalize the view direction: 0 = left edge, 0.5 = center, 1 = right edge. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (sends eased office direction); [game/GudoEncounter.java:102 (faces)](GudoEncounter.java#L102) (checks effective light direction).
    /*
     * Reject nonfinite values, then clamp pan into 0..1 only while active. The encounter uses this
     * normalized direction to decide whether the flashlight actually faces its door.
     * [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (sends eased office direction);
     * [game/GudoEncounter.java:102 (faces)](GudoEncounter.java#L102) (checks effective light direction).
     */
    public void setOfficePan(double pan) {
        if (!Double.isFinite(pan)) throw new IllegalArgumentException("Invalid office pan");
        if (!paused && !finished()) officePan = Math.max(0, Math.min(1, pan));
    }
    // Read access for the UI; power usage bars are an indicator, not the actual drain formula. | [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:371 (gudo call)](../Application.java#L371) (caller of gudo); [ui/GamePanel.java:409 (gudo call)](../ui/GamePanel.java#L409) (caller of gudo).
    /*
     * Read the encounter model owned by this session. This accessor does not advance time, save data
     * or change the model; callers use it to draw or coordinate behavior.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:371 (gudo call)](../Application.java#L371) (caller of gudo);
     * [ui/GamePanel.java:409 (gudo call)](../ui/GamePanel.java#L409) (caller of gudo);
     * [game/GudoEncounter.java:153 (advance)](GudoEncounter.java#L153) (enemy rules).
     */
    public GudoEncounter gudo() { return gudo; }
    /*
     * Return 0.085 base drain plus 0.22 for the monitor or 0.20 for light. The model prevents
     * simultaneous monitor/light. These are percentage points per active second, not the usage-bar
     * count.
     * [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:597 (renderHud)](../ui/GamePanel.java#L597) (displays power, usage and fictional hour);
     * [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (enemy deadline coordinated with power and morning).
     */
    public double drainPerSecond() { return BASE_DRAIN + (monitor ? MONITOR_DRAIN : 0) + (light ? LIGHT_DRAIN : 0); }
    /*
     * Return one base bar plus two for each active device. Under normal rules the possible values
     * are 1 or 3; the bars summarize usage and are not the actual percentage drain formula.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:597 (renderHud)](../ui/GamePanel.java#L597) (displays power, usage and fictional hour);
     * [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (enemy deadline coordinated with power and morning).
     */
    public int usage() { return 1 + (monitor ? 2 : 0) + (light ? 2 : 0); }
    /*
     * Read the difficulty night of this attempt. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:410 (night call)](../Application.java#L410) (caller of night);
     * [ui/GamePanel.java:619 (night call)](../ui/GamePanel.java#L619) (caller of night);
     * [game/GudoEncounter.java:153 (advance)](GudoEncounter.java#L153) (enemy rules).
     */
    public int night() { return night; }
    /*
     * Read active simulation seconds, excluding pause time. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (active clock drives CCTV sweep);
     * [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (active clock drives disruption and room cues).
     */
    public double elapsed() { return elapsed; }
    /*
     * Return elapsed/duration capped at one. The normalized fraction drives the clock, late-shift
     * audio and progress display.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (late-shift tension threshold);
     * [ui/GamePanel.java:597 (renderHud)](../ui/GamePanel.java#L597) (clock display).
     */
    public double progress() { return Math.min(1, elapsed / duration); }
    /*
     * Read unrounded remaining power percentage. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:597 (renderHud)](../ui/GamePanel.java#L597) (displays power, usage and fictional hour);
     * [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (enemy deadline coordinated with power and morning).
     */
    public double power() { return power; }
    /*
     * Read whether the model currently ignores updates/input. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:482 (pauseShift)](../Application.java#L482) (normal pause coordinator);
     * [Application.java:500 (resumeShift)](../Application.java#L500) (normal resume coordinator);
     * [Application.java:408 (openSecret)](../Application.java#L408) (pauses the original shift).
     */
    public boolean paused() { return paused; }
    /*
     * Read whether CCTV is raised. This accessor does not advance time, save data or change the
     * model; callers use it to draw or coordinate behavior.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:327 (toggleMonitor)](../ui/GamePanel.java#L327) (input and accepted-change feedback);
     * [Application.java:391 (updateGameAudio)](../Application.java#L391) (monitor or office ambience).
     */
    public boolean monitorOpen() { return monitor; }
    /*
     * Read whether the accepted flashlight state is on. This accessor does not advance time, save
     * data or change the model; callers use it to draw or coordinate behavior.
     * [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:311 (updateLight)](../ui/GamePanel.java#L311) (combines keyboard and mouse holds);
     * [game/GudoEncounter.java:153 (advance)](GudoEncounter.java#L153) (effective light changes exposure).
     */
    public boolean lightOn() { return light; }
    /*
     * Read the selected camera number. This accessor does not advance time, save data or change the
     * model; callers use it to draw or coordinate behavior.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:348 (selectCamera)](../ui/GamePanel.java#L348) (camera-input caller);
     * [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (draws selected feed);
     * [game/Scene.java:82 (camera)](Scene.java#L82) (validates camera numbers).
     */
    public int camera() { return camera; }
    /*
     * Return whether a nonnull RunRecord exists. A frozen final record, rather than a second mutable
     * finished flag, is the ending marker.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:538 (finishShift)](../Application.java#L538) (reads and presents the result);
     * [game/RunRecord.java:29 (RunRecord)](RunRecord.java#L29) (immutable receipt);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (stable identity for retry).
     */
    public boolean finished() { return result != null; }
    // Callers must wait until the shift ends; there is no final record during active gameplay. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [Application.java:538 (finishShift)](../Application.java#L538) (reads and presents the result); [game/RunRecord.java:29 (RunRecord)](RunRecord.java#L29) (immutable receipt).
    /*
     * Reject a request before the attempt ends; after finishing return the same immutable record.
     * Application must wait for finished() before obtaining a final result.
     * [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [Application.java:538 (finishShift)](../Application.java#L538) (reads and presents the result);
     * [game/RunRecord.java:29 (RunRecord)](RunRecord.java#L29) (immutable receipt);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (stable identity for retry).
     */
    public RunRecord result() {
        if (!finished()) throw new IllegalStateException("Shift is still running");
        return result;
    }

    // Convert shift completion into the fictional 12 AM-6 AM clock shown in the HUD. | [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [ui/GamePanel.java:597 (renderHud)](../ui/GamePanel.java#L597) (displays power, usage and fictional hour); [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (enemy deadline coordinated with power and morning).
    /*
     * Map progress 0..1 onto six fictional hours using integer truncation. Zero is displayed as 12
     * AM; full duration is 6 AM. One real minute corresponds to one displayed hour in a normal
     * six-minute shift.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/GamePanel.java:597 (renderHud)](../ui/GamePanel.java#L597) (displays power, usage and fictional hour);
     * [game/GudoEncounter.java:139 (untilEvent)](GudoEncounter.java#L139) (enemy deadline coordinated with power and morning).
     */
    public String clockLabel() {
        int hour = Math.min(6, (int) (progress() * 6));
        return (hour == 0 ? 12 : hour) + " AM";
    }

    // The pause screen includes fictional minutes instead of only the current hour. | [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18). | Related: [ui/PausePanel.java:58 (render)](../ui/PausePanel.java#L58) (displays fictional minutes); [game/RunRecord.java:39 (durationLabel)](RunRecord.java#L39) (contrasting actual active-time format).
    /*
     * Map progress onto 360 fictional minutes and format hour/minute for pause. This is a fictional
     * 12:00-06:00 AM clock, not the actual wall clock or the RunRecord duration.
     * [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 18: GameSession](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-18).
     * Related: [ui/PausePanel.java:58 (render)](../ui/PausePanel.java#L58) (displays fictional minutes);
     * [game/RunRecord.java:39 (durationLabel)](RunRecord.java#L39) (contrasting actual active-time format).
     */
    public String preciseClock() {
        int totalMinutes = Math.min(360, (int) (progress() * 360));
        int hour = totalMinutes / 60;
        return "%02d:%02d AM".formatted(hour == 0 ? 12 : hour, totalMinutes % 60);
    }
}
