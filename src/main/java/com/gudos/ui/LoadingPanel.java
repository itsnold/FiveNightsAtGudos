package com.gudos.ui;

import java.awt.*;

/** Animated waiting screen; the calling worker performs loading and decides when to leave it. */
public final class LoadingPanel extends ScenePanel {
    private final String message;

    public LoadingPanel(String message) { this.message = message; }

    @Override protected void render(Graphics2D g) {
        g.setColor(Theme.BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        String label = "Loading" + ".".repeat(1 + (int) age % 3);
        Theme.text(g, label, 542, 341, Theme.pixel(52), Color.WHITE);
        g.setFont(Theme.pixel(25));
        Theme.text(g, message, (WIDTH - g.getFontMetrics().stringWidth(message)) / 2, 395, Theme.pixel(25), Theme.MUTED);
    }
}
