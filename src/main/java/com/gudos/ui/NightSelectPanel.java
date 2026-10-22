package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.persistence.Progress;

import java.awt.*;

public final class NightSelectPanel extends BackdropPanel {
    private final Progress progress;

    public NightSelectPanel(AssetManager assets, Navigation navigation, Progress progress) {
        super(assets);
        this.progress = progress;
        backButton(navigation);
        for (int night = 1; night <= 5; night++) {
            int selected = night;
            GameButton button = new GameButton("Night " + night, GameButton.Style.TEXT, () -> navigation.startNight(selected));
            button.setFont(Theme.pixel(44));
            button.setEnabled(progress.unlocked(night));
            place(button, 105, 227 + (night - 1) * 68, 450, 55);
        }
    }

    @Override protected void render(Graphics2D g) {
        pageChrome(g, "Select Night", "Finish a shift to unlock the next. Replaying does not reset your progress.");
        for (int night = 1; night <= 5; night++) {
            String status = night <= progress.highestCompleted() ? "* Cleared" : !progress.unlocked(night) ? "Locked" : "";
            Theme.text(g, status, 478, 265 + (night - 1) * 68, Theme.pixel(28), progress.unlocked(night) ? Color.WHITE : Color.GRAY);
        }
        Theme.text(g, "No enemies yet. All five nights use the same power rules.", 170, 630, Theme.pixel(25), Theme.MUTED);
    }
}
