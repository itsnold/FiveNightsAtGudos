package com.gudos.ui.rendering;

import com.gudos.assets.AssetManager;
import com.gudos.ui.VhsOverlay;

import java.awt.Graphics2D;
import java.util.Objects;

/** Adds animated VHS interference without changing the wrapped renderer. Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: VhsRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29). */
/*
 * VhsRendererDecorator
 *
 * Purpose: Adds optional VHS interference after its wrapped renderer draws.
 *
 * Execution flow: Normal camera uses opacity 0.25; switching adds an outer opacity 0.85 layer at
 * speed 3.
 *
 * Design rationale: Delegate with original time so effect speed affects only this
 * wrapper. Skip noise when reducedEffects or opacity=0, but still render the wrapped room.
 *
 * State: assets/opacity/speed are validated configuration; copied effect Graphics2D is
 * disposed in finally.
 *
 * Summary: The wrapper changes rendering by adding a layer, without modifying
 * CameraSceneRenderer. Another decorator can wrap it.
 * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: VhsRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
 */
public final class VhsRendererDecorator extends RendererDecorator {
    private final AssetManager assets;
    private final float opacity;
    private final double speed;

    /*
     * The three-argument overload delegates with speed1. The full overload retains a nonnull asset
     * manager, validates finite opacity0..1 and positive finite speed, and retains the wrapped
     * renderer through super.
     * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: VhsRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
     */
    public VhsRendererDecorator(SceneRenderer wrapped, AssetManager assets, float opacity) {
        this(wrapped, assets, opacity, 1);
    }

    /*
     * The three-argument overload delegates with speed1. The full overload retains a nonnull asset
     * manager, validates finite opacity0..1 and positive finite speed, and retains the wrapped
     * renderer through super.
     * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: VhsRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
     */
    public VhsRendererDecorator(SceneRenderer wrapped, AssetManager assets, float opacity, double speed) {
        super(wrapped);
        this.assets = Objects.requireNonNull(assets);
        if (!Float.isFinite(opacity) || opacity < 0 || opacity > 1) throw new IllegalArgumentException("Invalid opacity");
        if (!Double.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("Invalid effect speed");
        this.opacity = opacity;
        this.speed = speed;
    }

    /*
     * Render the wrapped component with original time first. If effects are allowed and opacity>0,
     * draw VHS on a copied context using time*speed for this wrapper only. Dispose the copy so its
     * graphics state cannot leak into the HUD.
     * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: VhsRendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
     */
    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        super.render(graphics, time, reducedEffects);
        if (reducedEffects || opacity == 0) return;
        Graphics2D effect = (Graphics2D) graphics.create();
        try {
            VhsOverlay.paint(effect, assets, time * speed, opacity, false);
        } finally {
            effect.dispose();
        }
    }
}
