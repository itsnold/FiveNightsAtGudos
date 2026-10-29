package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.GameSession;

import java.awt.*;

public final class PausePanel extends BackdropPanel {
    private final GameSession session;

    public PausePanel(AssetManager assets, Navigation navigation, GameSession session) {
        super(assets);
        this.session = session;
        place(new GameButton("Resume", GameButton.Style.TEXT, navigation::resumeShift), 423, 350, 450, 60);
        place(new GameButton("RETURN TO MENU", GameButton.Style.SECONDARY, navigation::leaveShift), 485, 439, 315, 46);
        bind("ESCAPE", "resume", navigation::resumeShift);
    }

    @Override protected void render(Graphics2D g) {
        backdrop(g);
        Theme.text(g, "Paused", 530, 265, Theme.pixel(76), Color.WHITE);
        Theme.text(g, "Night %d  /  %s".formatted(session.night(), session.preciseClock()), 510, 310, Theme.pixel(28), Color.WHITE);
        Theme.text(g, "Clock, power and Sir Gudo are stopped.", 451, 535, Theme.pixel(25), Theme.MUTED);
        Theme.text(g, "Leaving records an unfinished attempt.", 451, 572, Theme.pixel(24), Theme.MUTED);
    }
}
