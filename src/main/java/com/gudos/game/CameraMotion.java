package com.gudos.game;

/** Each CCTV camera sweeps on its own clock, even while a different feed is watched. */
public final class CameraMotion {
    private CameraMotion() { }

    public static double pan(Scene scene, double elapsedSeconds) {
        if (scene == null || scene == Scene.OFFICE || !Double.isFinite(elapsedSeconds)) throw new IllegalArgumentException("Invalid CCTV camera");
        double period = 22 + scene.camera() * 2.3;
        double phase = elapsedSeconds / period + scene.camera() * 0.137;
        return 0.5 - 0.46 * Math.cos(phase * Math.PI * 2);
    }
}
