package com.gudos.ui.rendering;

import com.gudos.assets.AssetManager;
import com.gudos.ui.VhsOverlay;

import java.awt.Graphics2D;
import java.util.Objects;

/** Adds animated VHS interference without changing the wrapped renderer. */
public final class VhsRendererDecorator extends RendererDecorator {
    private final AssetManager assets;
    private final float opacity;
    private final double speed;

    public VhsRendererDecorator(SceneRenderer wrapped, AssetManager assets, float opacity) {
        this(wrapped, assets, opacity, 1);
    }

    public VhsRendererDecorator(SceneRenderer wrapped, AssetManager assets, float opacity, double speed) {
        super(wrapped);
        this.assets = Objects.requireNonNull(assets);
        if (!Float.isFinite(opacity) || opacity < 0 || opacity > 1) throw new IllegalArgumentException("Invalid opacity");
        if (!Double.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("Invalid effect speed");
        this.opacity = opacity;
        this.speed = speed;
    }

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
