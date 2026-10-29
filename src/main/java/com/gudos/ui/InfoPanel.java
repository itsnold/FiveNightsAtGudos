package com.gudos.ui;

import com.gudos.assets.AssetManager;

import java.awt.*;

/** Static guide/credits screen; the credits flag chooses which content is drawn. */
public final class InfoPanel extends BackdropPanel {
    private final boolean credits;

    public InfoPanel(AssetManager assets, Navigation navigation, boolean credits) {
        super(assets);
        this.credits = credits;
        backButton(navigation);
    }

    @Override protected void render(Graphics2D g) {
        if (credits) renderCredits(g);
        else renderGuide(g);
    }

    private void renderGuide(Graphics2D g) {
        pageChrome(g, "How to Play", "");
        Theme.tracked(g, "AT YOUR DESK", 82, 250, 12, Theme.AMBER);
        String[][] controls = {
                {"MOUSE EDGES / A · D", "Look left and right across the office"},
                {"SPACE / TAB", "Raise or lower the camera monitor"},
                {"1 — 6", "Select a camera while the monitor is open"},
                {"HOLD F", "Face his doorway; hold until he fades"},
                {"ESC", "Pause or resume the shift"},
                {"F11", "Toggle borderless fullscreen"}
        };
        for (int row = 0; row < controls.length; row++) {
            int y = 292 + row * 47;
            Theme.text(g, controls[row][0], 82, y, Theme.mono(13), Theme.PAPER);
            Theme.text(g, controls[row][1], 296, y, Theme.sans(14), Theme.MUTED);
            Theme.rule(g, 82, y + 16, 540);
        }
        g.setColor(Theme.LINE);
        g.drawLine(676, 241, 676, 593);
        String[] headings = {"01  FOLLOW HIS ROUTE", "02  HOLD THE LIGHT", "03  SURVIVE UNTIL 6 AM"};
        String[] paragraphs = {
                "Sir Gudo approaches via 04 > 03 > 02 (left), 06 > 05 (right), or 01 (center). Watch CCTV to see where he is heading.",
                "When footsteps reach the office, lower the monitor. Face his doorway with A/D, then hold F until he disappears. Looking away lets him recover.",
                "Last six minutes. Save power between attacks. Later nights give less time to react. A completed shift unlocks the next night."
        };
        for (int index = 0; index < headings.length; index++) {
            int y = 250 + index * 119;
            Theme.tracked(g, headings[index], 719, y, 11, Theme.AMBER);
            Theme.paragraph(g, paragraphs[index], 719, y + 30, 450, 23, Theme.sans(16), Theme.MUTED);
        }
        Theme.text(g, "Shifts auto-pause when the game loses focus. Mid-shift resume after closing the app is not supported.", 82, 625, Theme.sans(14), Theme.MUTED);
    }

    private void renderCredits(Graphics2D g) {
        pageChrome(g, "Credits", "Five Nights at Gudo's");
        Theme.text(g, "Java + Swing", 82, 273, Theme.pixel(42), Color.WHITE);
        Theme.paragraph(g, "Java2D graphics. Java Sound audio. SQLite through JDBC. No browser or game engine.", 82, 318, 485, 28, Theme.pixel(25), Theme.MUTED);
        Theme.text(g, "Environment", 82, 420, Theme.pixel(32), Color.WHITE);
        Theme.paragraph(g, "Carinderia: Night Shift - Render Pack v2. Seven supplied AI-generated rooms. Original pack notes are preserved in docs/assets/.", 82, 461, 485, 26, Theme.pixel(24), Theme.MUTED);
        Theme.text(g, "TV interference", 678, 260, Theme.pixel(31), Color.WHITE);
        Theme.paragraph(g, "Television static.gif by GravelTravel22\nWikimedia Commons / CC BY-SA 4.0\nAlpha-masked and scaled at runtime.", 678, 295, 510, 25, Theme.pixel(23), Theme.MUTED);
        Theme.text(g, "Pixel font", 678, 399, Theme.pixel(31), Color.WHITE);
        Theme.paragraph(g, "VT323 / The VT323 Project Authors\nSIL Open Font License 1.1\nFull credits and licenses bundled in licenses/.", 678, 434, 510, 25, Theme.pixel(23), Theme.MUTED);
        Theme.text(g, "Audio", 678, 539, Theme.pixel(31), Color.WHITE);
        Theme.paragraph(g, "Supplied Five Nights at Freddy's sound pack.\nVerify reuse permissions before distribution.", 678, 574, 510, 25, Theme.pixel(23), Theme.MUTED);
        Theme.text(g, "Sir Gudo states: GPT Image edits of supplied art. Encounter sounds: original synthesized audio.", 82, 655, Theme.pixel(22), Theme.MUTED);
    }
}
