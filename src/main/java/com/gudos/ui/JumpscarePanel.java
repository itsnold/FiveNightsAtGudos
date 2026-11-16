package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Scene;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/** A generated still animated with a lunge, shake and cached directional blur. [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). Related: [Application.java:538 (finishShift)](../Application.java#L538) (shows the scare and wires completion); [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (screen shown afterward). */
/*
 * JumpscarePanel
 *
 * Related: [Application.java:538 (finishShift)](../Application.java#L538) (shows the scare and wires completion);
 * [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (screen shown afterward);
 * [assets/AssetManager.java:55 (AssetManager)](../assets/AssetManager.java#L55) (loads the scare image);
 * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (reduced-effects preference).
 *
 * Purpose: Animates the caught still and calls back once after 2.1 seconds.
 *
 * Execution flow: Application.finishShift on CAUGHT starts sound/save and shows this; completion
 * opens the prepared ResultPanel.
 *
 * Design rationale: Precompute a horizontal blur once. Zoom/shake reuse the still;
 * reducedEffects keeps it stationary. Guard delivered avoids repeated navigation.
 *
 * State: DURATION=2.1; still/blurred are cached images; age drives zoom/shake/final
 * fade; delivered gates callback.
 *
 * Summary: The scare is a visual transition after the simulation has already ended. Database
 * saving starts during it.
 * [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
 */
public final class JumpscarePanel extends ScenePanel {
    public static final double DURATION = 2.1;
    private final BufferedImage still;
    private final BufferedImage blurred;
    private final boolean reducedEffects;
    private final Runnable complete;
    private boolean delivered;

    /*
     * Fetch the jumpscare image, prepare a viewport-sized copy and precompute the nine-sample
     * horizontal convolution blur. Store reduced-effects and exactly-once completion callback; do
     * not advance/save the model here.
     * [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [Application.java:552 (JumpscarePanel call)](../Application.java#L552) (caller of JumpscarePanel);
     * [assets/AssetManager.java:113 (scene)](../assets/AssetManager.java#L113) (used by JumpscarePanel);
     * [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (screen shown afterward).
     */
    public JumpscarePanel(AssetManager assets, boolean reducedEffects, Runnable complete) {
        this.reducedEffects = reducedEffects;
        this.complete = complete;
        still = assets.scene(Scene.OFFICE, "jumpscare");
        BufferedImage frame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.drawImage(still, 0, 0, WIDTH, HEIGHT, null);
        g.dispose();
        // Precompute horizontal blur once; rendering reuses it instead of filtering every frame. | [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). | Related: [Application.java:552 (JumpscarePanel call)](../Application.java#L552) (caller of JumpscarePanel); [assets/AssetManager.java:113 (scene)](../assets/AssetManager.java#L113) (used by JumpscarePanel).
        float[] streak = {0.06f, 0.09f, 0.12f, 0.15f, 0.16f, 0.15f, 0.12f, 0.09f, 0.06f};
        blurred = new ConvolveOp(new Kernel(9, 1, streak), ConvolveOp.EDGE_NO_OP, null).filter(frame, null);
    }

    // Deliver the completion callback exactly once, letting Application show the result screen. | [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). | Related: [ui/ScenePanel.java:219 (tick)](ScenePanel.java#L219) (used by tick); [Application.java:538 (finishShift)](../Application.java#L538) (shows the scare and wires completion).
    /*
     * Advance age and, after 2.1 seconds, set delivered before invoking the completion callback.
     * Even later ticks cannot show the result twice.
     * [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [ui/ScenePanel.java:219 (tick)](ScenePanel.java#L219) (used by tick);
     * [Application.java:538 (finishShift)](../Application.java#L538) (shows the scare and wires completion);
     * [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (screen shown afterward).
     */
    @Override public void tick(double seconds) {
        super.tick(seconds);
        if (age >= DURATION && !delivered) {
            delivered = true;
            complete.run();
        }
    }

    /*
     * Draw the cached still with zoom/shake and optional cached blur; reducedEffects uses a
     * stationary image. Fade to black in the final 0.35 seconds. This transition happens after the
     * model outcome is already CAUGHT.
     * [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [Application.java:538 (finishShift)](../Application.java#L538) (shows the scare and wires completion);
     * [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (screen shown afterward);
     * [assets/AssetManager.java:55 (AssetManager)](../assets/AssetManager.java#L55) (loads the scare image).
     */
    @Override protected void render(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // Animate one still with zoom/shake; reduced effects keeps the image stationary. | [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). | Related: [Application.java:538 (finishShift)](../Application.java#L538) (shows the scare and wires completion); [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (screen shown afterward).
        double zoom = reducedEffects ? 1 : 1.06 + Math.min(0.14, age * 0.18);
        int w = (int) Math.ceil(WIDTH * zoom);
        int h = (int) Math.ceil(HEIGHT * zoom);
        int shakeX = reducedEffects ? 0 : (int) (Math.sin(age * 109) * 18 + Math.cos(age * 73) * 8);
        int shakeY = reducedEffects ? 0 : (int) (Math.cos(age * 97) * 13);
        int x = (WIDTH - w) / 2 + shakeX;
        int y = (HEIGHT - h) / 2 + shakeY;
        g.drawImage(still, x, y, w, h, null);
        if (!reducedEffects) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.42f));
            g.drawImage(blurred, x + (int) (Math.sin(age * 89) * 18), y, w, h, null);
            g.setComposite(AlphaComposite.SrcOver);
        }
        // A single fade to black, never a flashing sequence. | [p. 24: JumpscarePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). | Related: [Application.java:538 (finishShift)](../Application.java#L538) (shows the scare and wires completion); [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (screen shown afterward).
        if (age > DURATION - 0.35) {
            g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, (age - DURATION + 0.35) / 0.35))));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }
}
