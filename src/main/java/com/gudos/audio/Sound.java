package com.gudos.audio;

/** Named cues have gains relative to master volume; a null path means Chiptune synthesizes them. Reviewer (42-page edition): [p. 30: Sound](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). */
/*
 * Sound
 *
 * Purpose: Names each cue and stores its classpath path and relative gain.
 *
 * Execution flow: GamePanel/SecretPanel/Application choose a Sound enum; AudioService reads path
 * and gain.
 *
 * Design rationale: Enum values avoid scattered filename strings. Master volume and
 * per-sound gain are separate. Null paths mark the three Chiptune sounds.
 *
 * State: path is prefixed with /audio/ for WAVs; gain scales the master volume. The
 * active cue list is this enum, not every file in asset-sources.
 *
 * Summary: Sound is metadata, AudioService performs playback, and GamePanel decides when
 * gameplay cues should occur.
 * Reviewer (42-page edition): [p. 30: Sound](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
 */
public enum Sound {
    MENU_MUSIC("music/menu-darkness.wav", 0.65f),
    MENU_STATIC("effects/menu-static.wav", 0.22f),
    HUM("ambience/electrical-hum.wav", 0.32f),
    FAN("ambience/office-fan.wav", 0.45f),
    CAMERA_SWITCH("effects/camera-switch.wav", 0.38f),
    MONITOR_OPEN("effects/monitor-open.wav", 0.35f),
    MONITOR_CLOSE("effects/monitor-close.wav", 0.50f),
    COMPLETE("effects/shift-complete.wav", 0.50f),
    POWER_DOWN("effects/power-down.wav", 0.45f),
    ERROR("effects/error.wav", 0.30f),
    ROOM_TONE("ambience/room-tone.wav", 0.12f),
    CAMERA_HISS("ambience/camera-hiss.wav", 0.09f),
    TENSION("ambience/late-shift.wav", 0.10f),
    GUDO_BREATH("ambience/gudo-breath.wav", 0.24f),
    FLASHLIGHT_ON("effects/flashlight-on.wav", 0.24f),
    FLASHLIGHT_OFF("effects/flashlight-off.wav", 0.20f),
    GARBLE_1("effects/garble1.wav", 0.32f),
    GARBLE_2("effects/garble2.wav", 0.32f),
    GARBLE_3("effects/garble3.wav", 0.32f),
    CAMERA_DIGITAL("effects/camera-garble-digital.wav", 0.24f),
    METAL_CREAK("effects/metal-creak.wav", 0.18f),
    METAL_RATTLE("effects/metal-rattle.wav", 0.16f),
    LOW_POWER("effects/low-power.wav", 0.32f),
    GUDO_STEP("effects/deep-steps.wav", 0.42f),
    GUDO_WARNING("effects/entrance-knock.wav", 0.48f),
    GUDO_POUND("effects/corridor-pound.wav", 0.42f),
    GUDO_WHISPER("effects/entrance-whisper.wav", 0.30f),
    GUDO_RUSH("effects/attack-rush.wav", 0.44f),
    GUDO_RETREAT("effects/retreat-fast.wav", 0.34f),
    JUMPSCARE("effects/caught-scream2.wav", 0.40f),
    SECRET_AMBIENCE(null, 0.20f),
    SECRET_JUMP(null, 0.35f),
    SECRET_PICKUP(null, 0.35f);

    private final String path;
    private final float gain;

    /*
     * Store a null path for synthesized secret cues or prefix a WAV path with /audio/. Retain
     * relative gain; AudioService later combines it with master volume.
     * Reviewer (42-page edition): [p. 30: Sound](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     */
    Sound(String path, float gain) { this.path = path == null ? null : "/audio/" + path; this.gain = gain; }
    /*
     * Read the resource path, or null for a synthesized secret cue. This accessor does not advance
     * time, save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 30: Sound](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     */
    public String path() { return path; }
    /*
     * Read the sound multiplier relative to master volume. This accessor does not advance time, save
     * data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 30: Sound](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     */
    public float gain() { return gain; }
}
