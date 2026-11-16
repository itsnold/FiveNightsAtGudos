package com.gudos.ui;

import com.gudos.assets.AssetManager;

import java.awt.*;

/** Downloaded animated TV noise, with runtime scanlines and rolling tracking bands. Reviewer (42-page edition): [p. 23: VhsOverlay](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). */
/*
 * VhsOverlay
 *
 * Purpose: Draws animated noise, scanlines and a rolling band as a visual utility.
 *
 * Execution flow: Menu/GamePanel and VhsRendererDecorator call paint.
 *
 * Design rationale: It returns immediately for reduced effects and restores
 * composite/interpolation. The decorator uses a copied Graphics2D so color/paint changes do not leak
 * into later HUD drawing.
 *
 * State: Noise frames advance at about 16.6667 per second; room simulation is unchanged
 * by this layer.
 *
 * Summary: VhsOverlay is a helper function; VhsRendererDecorator is the custom Decorator
 * because it wraps the shared SceneRenderer interface.
 * Reviewer (42-page edition): [p. 23: VhsOverlay](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
 */
public final class VhsOverlay {
    /*
     * Private constructor prevents utility instances; paint is a stateless visual-layer operation.
     * Reviewer (42-page edition): [p. 23: VhsOverlay](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     */
    private VhsOverlay() { }

    /*
     * Return for reduced effects or nonpositive opacity. Preserve composite/interpolation, draw a
     * wrapped noise frame scaled nearest-neighbor, restore those settings, then add scanlines and a
     * moving tracking band. Caller-owned graphics isolation is still needed for color/paint changes.
     * Reviewer (42-page edition): [p. 23: VhsOverlay](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     */
    public static void paint(Graphics2D g, AssetManager assets, double time, float opacity, boolean reducedEffects) {
        // This is a visual layer only; disabling it does not change gameplay or enemy timing. | Reviewer (42-page edition): [p. 23: VhsOverlay](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
        if (reducedEffects || opacity <= 0) return;
        // Restore the incoming graphics settings after the noise so later painting is unaffected. | Reviewer (42-page edition): [p. 23: VhsOverlay](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
        Composite composite = g.getComposite();
        Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1, opacity)));
        g.drawImage(assets.noise((int) (time * 16.6667)), 0, 0, 1280, 720, null);
        g.setComposite(composite);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        g.setColor(new Color(0, 0, 0, (int) (110 * opacity)));
        for (int y = 0; y < 720; y += 3) g.fillRect(0, y, 1280, 1);
        int band = (int) ((time * 28 + 420) % 890) - 85;
        g.setColor(new Color(220, 220, 220, (int) (61 * opacity)));
        g.fillRect(0, band, 1280, 35);
        g.setColor(new Color(255, 255, 255, (int) (100 * opacity)));
        g.fillRect(0, band + 35, 1280, 2);
        g.setColor(new Color(0, 0, 0, (int) (150 * opacity)));
        g.fillRect(0, band + 38, 1280, 5);
    }
}
