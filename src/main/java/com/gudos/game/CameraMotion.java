package com.gudos.game;

/** Each CCTV camera sweeps on its own clock, even while a different feed is watched. */
public final class CameraMotion {
    private CameraMotion() { }

    public static double pan(Scene scene, double elapsedSeconds) {
        if (scene == null || scene == Scene.OFFICE || !Double.isFinite(elapsedSeconds)) throw new IllegalArgumentException("Invalid CCTV camera");
        // Camera number gives each feed a different sweep speed and starting phase.
        // Using session elapsed time makes sweeps pause with gameplay and continue off-screen.
        double period = 22 + scene.camera() * 2.3;
        double phase = elapsedSeconds / period + scene.camera() * 0.137;
        // Cosine produces a smooth left-right cycle in the normalized range 0.04-0.96.
        return 0.5 - 0.46 * Math.cos(phase * Math.PI * 2);
    }
}
