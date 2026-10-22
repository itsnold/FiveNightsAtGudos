package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.persistence.Progress;
import com.gudos.persistence.Settings;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Supplied title renders flicker in brief, randomly spaced bursts beneath the static. */
public final class MenuPanel extends ScenePanel {
    private final AssetManager assets;
    private final Settings settings;
    private final Progress progress;
    private final List<GameButton> entries = new ArrayList<>();
    private int selected;
    private int renderIndex;
    private int flashesRemaining;
    private double untilFlicker = normalInterval();

    private static double normalInterval() {
        return ThreadLocalRandom.current().nextDouble(3.5, 10.0);
    }

    @Override public void tick(double seconds) {
        super.tick(seconds);
        if (settings.reducedEffects()) {
            renderIndex = flashesRemaining = 0;
            untilFlicker = normalInterval();
            return;
        }
        untilFlicker -= seconds;
        if (untilFlicker > 0) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (renderIndex == 0 && flashesRemaining == 0) {
            flashesRemaining = random.nextInt(1, 5);
        } else if (flashesRemaining == 0) {
            renderIndex = 0;
            untilFlicker = normalInterval();
            return;
        }
        int next;
        do { next = random.nextInt(1, 5); } while (next == renderIndex);
        renderIndex = next;
        flashesRemaining--;
        untilFlicker = random.nextDouble(0.055, 0.14);
    }

    public MenuPanel(AssetManager assets, Navigation navigation, Settings settings, Progress progress) {
        this.assets = assets;
        this.settings = settings;
        this.progress = progress;
        entry("New Game", 385, () -> navigation.startNight(1));
        GameButton continueButton = entry("Continue", 457, () -> navigation.startNight(progress.nextNight()));
        continueButton.setEnabled(progress.totalRuns() > 0);
        entry("Extras", 535, () -> navigation.page(Navigation.Page.EXTRAS));
        select(0);
        GameButton quit = new GameButton("Quit", GameButton.Style.QUIET, navigation::quit);
        quit.setFont(Theme.pixel(24));
        place(quit, 18, 645, 80, 32);
        bind("pressed DOWN", "next", () -> move(1));
        bind("pressed UP", "previous", () -> move(-1));
        bind("pressed ENTER", "choose", () -> entries.get(selected).doClick());
    }

    private GameButton entry(String text, int y, Runnable action) {
        int index = entries.size();
        GameButton button = new GameButton(text, GameButton.Style.TEXT, action);
        button.setFocusable(false);
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) { if (button.isEnabled()) select(index); }
        });
        entries.add(place(button, 103, y, 450, 62));
        return button;
    }

    private void move(int direction) {
        int index = selected;
        do { index = Math.floorMod(index + direction, entries.size()); } while (!entries.get(index).isEnabled());
        select(index);
    }

    private void select(int index) {
        selected = index;
        for (int i = 0; i < entries.size(); i++) entries.get(i).setSelected(i == index);
    }

    @Override protected void render(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Theme.cover(g, assets.menuRender(settings.reducedEffects() ? 0 : renderIndex), 0, 0, WIDTH, HEIGHT, 0.5);
        String[] title = {"Five", "Nights", "at", "Gudo's"};
        for (int line = 0; line < title.length; line++) Theme.text(g, title[line], 170, 112 + line * 56, Theme.pixel(56), Color.WHITE);
        if (progress.highestCompleted() > 0) Theme.text(g, "*".repeat(progress.highestCompleted()), 425, 110, Theme.pixel(36), Color.WHITE);
        if (progress.totalRuns() > 0) Theme.text(g, "Night " + progress.nextNight(), 172, 530, Theme.pixel(21), Color.WHITE);
        Theme.text(g, "v 0.1", 19, 707, Theme.pixel(23), Color.WHITE);
        Theme.text(g, "Five Nights at Gudo's", 1010, 707, Theme.pixel(22), Color.WHITE);
    }

    @Override protected void renderOverlay(Graphics2D g) {
        VhsOverlay.paint(g, assets, age, 0.46f, settings.reducedEffects());
    }
}
