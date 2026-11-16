package com.gudos.game;

/** Each CCTV camera sweeps on its own clock, even while a different feed is watched. [p. 27: CameraMotion](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27). Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (uses the sweep to draw feeds); [ui/GamePanel.java:217 (mouseClicked)](../ui/GamePanel.java#L217) (uses the same sweep for hit testing). */
/*
 * CameraMotion
 *
 * Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (uses the sweep to draw feeds);
 * [ui/GamePanel.java:217 (mouseClicked)](../ui/GamePanel.java#L217) (uses the same sweep for hit testing);
 * [game/GameSession.java:306 (elapsed)](GameSession.java#L306) (active clock driving the sweep).
 *
 * Purpose: Computes the automatic CCTV sweep from camera identity and active shift time.
 *
 * Execution flow: CameraSceneRenderer and the secret-paper hit test call pan(scene,elapsed).
 *
 * Design rationale: A cosine gives smooth reversal. Each camera has a different
 * phase and period. Using elapsed means unseen cameras continue their sweep and all sweeps freeze
 * during pause.
 *
 * State: period = 22 + cameraNumber * 2.3; phase = elapsed/period + cameraNumber *
 * 0.137; pan stays between 0.04 and 0.96.
 *
 * Summary: Office panning is player-controlled; CCTV panning is an automatic function of the
 * session clock.
 * [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 27: CameraMotion](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27).
 */
public final class CameraMotion {
    /*
     * Private constructor prevents instantiating this stateless mathematical utility.
     * [p. 27: CameraMotion](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27).
     * Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (uses the sweep to draw feeds);
     * [ui/GamePanel.java:217 (mouseClicked)](../ui/GamePanel.java#L217) (uses the same sweep for hit testing);
     * [game/GameSession.java:306 (elapsed)](GameSession.java#L306) (active clock driving the sweep).
     */
    private CameraMotion() { }

    /*
     * Validate that this is a finite-time camera scene, derive period/phase from its camera number,
     * and return 0.5 - 0.46*cos(2*pi*phase). Reading session elapsed gives smooth independent sweeps
     * that pause with the night.
     * [p. 27: CameraMotion](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27).
     * Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (feed-rendering caller);
     * [ui/GamePanel.java:217 (mouseClicked)](../ui/GamePanel.java#L217) (paper-hit-test caller);
     * [game/GameSession.java:306 (elapsed)](GameSession.java#L306) (shared active clock).
     */
    public static double pan(Scene scene, double elapsedSeconds) {
        if (scene == null || scene == Scene.OFFICE || !Double.isFinite(elapsedSeconds)) throw new IllegalArgumentException("Invalid CCTV camera");
        // Camera number gives each feed a different sweep speed and starting phase. | [p. 27: CameraMotion](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27). | Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (feed-rendering caller); [ui/GamePanel.java:217 (mouseClicked)](../ui/GamePanel.java#L217) (paper-hit-test caller).
        // Using session elapsed time makes sweeps pause with gameplay and continue off-screen. | [p. 27: CameraMotion](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27). | Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (feed-rendering caller); [ui/GamePanel.java:217 (mouseClicked)](../ui/GamePanel.java#L217) (paper-hit-test caller).
        double period = 22 + scene.camera() * 2.3;
        double phase = elapsedSeconds / period + scene.camera() * 0.137;
        // Cosine produces a smooth left-right cycle in the normalized range 0.04-0.96. | [p. 27: CameraMotion](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-27). | Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (feed-rendering caller); [ui/GamePanel.java:217 (mouseClicked)](../ui/GamePanel.java#L217) (paper-hit-test caller).
        return 0.5 - 0.46 * Math.cos(phase * Math.PI * 2);
    }
}
