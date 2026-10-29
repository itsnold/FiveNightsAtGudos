package com.gudos.audio;

/** Named sound cues map to bundled WAV paths and individual gains relative to master volume. */
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
    GUDO_STEP("effects/gudo-step.wav", 0.46f),
    GUDO_WARNING("effects/gudo-warning.wav", 0.48f),
    GUDO_RETREAT("effects/gudo-retreat.wav", 0.34f),
    JUMPSCARE("effects/gudo-jumpscare.wav", 0.60f);

    private final String path;
    private final float gain;

    Sound(String path, float gain) { this.path = "/audio/" + path; this.gain = gain; }
    public String path() { return path; }
    public float gain() { return gain; }
}
