package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Scene;

import java.awt.*;

public abstract class BackdropPanel extends ScenePanel {
    protected final AssetManager assets;

    protected BackdropPanel(AssetManager assets) { this.assets = assets; }

    protected void backdrop(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Theme.cover(g, assets.scene(Scene.OFFICE, "default"), 0, 0, WIDTH, HEIGHT, 0.64);
        g.setColor(new Color(0, 0, 0, 235));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    protected void pageChrome(Graphics2D g, String title, String subtitle) {
        backdrop(g);
        Theme.text(g, title, 80, 158, Theme.pixel(52), Theme.PAPER);
        Theme.text(g, subtitle, 82, 192, Theme.sans(16), Theme.MUTED);
        Theme.text(g, "v 0.1", 20, 706, Theme.pixel(21), Theme.MUTED);
    }

    protected void backButton(Navigation navigation) {
        place(new GameButton("← BACK", GameButton.Style.QUIET, navigation::menu), 72, 38, 112, 38);
        bind("ESCAPE", "back", navigation::menu);
    }
}
