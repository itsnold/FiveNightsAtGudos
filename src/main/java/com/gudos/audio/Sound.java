package com.gudos.audio;

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
    ERROR("effects/error.wav", 0.30f);

    private final String path;
    private final float gain;

    Sound(String path, float gain) { this.path = "/audio/" + path; this.gain = gain; }
    public String path() { return path; }
    public float gain() { return gain; }
}
