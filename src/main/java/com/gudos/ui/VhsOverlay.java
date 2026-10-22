package com.gudos.ui;

import com.gudos.assets.AssetManager;

import java.awt.*;

/** Downloaded animated TV noise, with runtime scanlines and rolling tracking bands. */
public final class VhsOverlay {
    private VhsOverlay() { }

    public static void paint(Graphics2D g, AssetManager assets, double time, float opacity, boolean reducedEffects) {
        if (reducedEffects || opacity <= 0) return;
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
