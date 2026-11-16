package com.gudos.ui.rendering;

import com.gudos.ui.ScenePanel;

import java.awt.Color;
import java.awt.Graphics2D;

/** Adds a black tint after rendering the wrapped component. Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 30: DimmedRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). */
/*
 * DimmedRendererDecorator
 *
 * Purpose: Adds a translucent black layer after its wrapped renderer draws.
 *
 * Execution flow: GamePanel puts it around the normal camera chain for switching interference.
 *
 * Design rationale: Validate alpha=0-255, delegate first, and draw using a copied
 * graphics context. Layer order changes pixels.
 *
 * State: shade holds black with configured alpha. It dims when called even with
 * reducedEffects; GamePanel bypasses the switching chain in that mode.
 *
 * Summary: It is a decorator because it accepts and remains a SceneRenderer, delegates, then
 * adds darkness.
 * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 30: DimmedRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
 */
public final class DimmedRendererDecorator extends RendererDecorator {
    private final Color shade;

    /*
     * Pass the wrapped component to the base, reject alpha outside0..255 and create the black tint.
     * The layer is applied only when render is called.
     * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 30: DimmedRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     */
    public DimmedRendererDecorator(SceneRenderer wrapped, int alpha) {
        super(wrapped);
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Alpha must be 0–255");
        shade = new Color(0, 0, 0, alpha);
    }

    /*
     * Render the wrapped component first, then fill a copied Graphics2D with the configured
     * translucent black shade and dispose it in finally. It always dims if called; reduced switching
     * is bypassed by GamePanel.
     * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 30: DimmedRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     */
    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        super.render(graphics, time, reducedEffects);
        Graphics2D effect = (Graphics2D) graphics.create();
        try {
            effect.setColor(shade);
            effect.fillRect(0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT);
        } finally {
            effect.dispose();
        }
    }
}
