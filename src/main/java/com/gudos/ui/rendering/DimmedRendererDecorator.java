package com.gudos.ui.rendering;

import com.gudos.ui.ScenePanel;

import java.awt.Color;
import java.awt.Graphics2D;

/** Adds a black tint after rendering the wrapped component. */
public final class DimmedRendererDecorator extends RendererDecorator {
    private final Color shade;

    public DimmedRendererDecorator(SceneRenderer wrapped, int alpha) {
        super(wrapped);
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Alpha must be 0–255");
        shade = new Color(0, 0, 0, alpha);
    }

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
