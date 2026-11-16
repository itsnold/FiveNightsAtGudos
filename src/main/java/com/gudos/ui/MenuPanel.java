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

/** Supplied title renders flicker in brief, randomly spaced bursts beneath the static. [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20). Related: [Application.java:262 (showMenu)](../Application.java#L262) (creates the title screen); [persistence/Progress.java:27 (Progress)](../persistence/Progress.java#L27) (saved counts and next unlocked night). */
/*
 * MenuPanel
 *
 * Related: [Application.java:262 (showMenu)](../Application.java#L262) (creates the title screen);
 * [Application.java:358 (startNight)](../Application.java#L358) (handles New Game and Continue);
 * [persistence/Progress.java:27 (Progress)](../persistence/Progress.java#L27) (saved counts and next unlocked night);
 * [ui/ScenePanel.java:62 (ScenePanel)](ScenePanel.java#L62) (shared drawing and input infrastructure).
 *
 * Purpose: Displays the title menu, enabled Continue state and random portrait flicker.
 *
 * Execution flow: New Game -> startNight(1); Continue -> startNight(progress.nextNight()); Extras
 * -> page(EXTRAS); Quit -> quit.
 *
 * Design rationale: New Game deliberately preserves existing history. Continue is
 * enabled after any recorded attempt, even a loss. Keyboard selection skips disabled entries.
 * Flicker randomness is visual only.
 *
 * State: entries/selected track UI selection; renderIndex/flashesRemaining/untilFlicker
 * drive the five menu portraits. reducedEffects selects the clean portrait.
 *
 * Summary: The menu delegates the complex start operation to the Navigation/Application
 * facade. It does not create the session itself.
 * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
 */
public final class MenuPanel extends ScenePanel {
    private final AssetManager assets;
    private final Settings settings;
    private final Progress progress;
    private final List<GameButton> entries = new ArrayList<>();
    private int selected;
    private int renderIndex;
    private int flashesRemaining;
    private double untilFlicker = normalInterval();

    /*
     * Choose 3.5..10 seconds between visual flicker bursts using ThreadLocalRandom. This randomness
     * affects portraits only; Gudo route order remains deterministic.
     * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:215 (tick)](../Application.java#L215) (animation driver);
     * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (reduced-effects preference);
     * [assets/AssetManager.java:141 (menuRender)](../assets/AssetManager.java#L141) (portrait sequence).
     */
    private static double normalInterval() {
        return ThreadLocalRandom.current().nextDouble(3.5, 10.0);
    }

    // Update only the title's flicker animation; there is no active game simulation on this screen. | [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20). | Related: [Application.java:215 (tick)](../Application.java#L215) (animation driver); [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (reduced-effects preference).
    /*
     * Advance visual age; reduced effects resets to the normal portrait. Otherwise count down,
     * choose a short burst of one to four different frightening portraits, then return to the normal
     * waiting interval.
     * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:215 (tick)](../Application.java#L215) (animation driver);
     * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (reduced-effects preference);
     * [assets/AssetManager.java:141 (menuRender)](../assets/AssetManager.java#L141) (portrait sequence).
     */
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

    /*
     * Retain assets/settings/progress; wire New Game to startNight(1), Continue to
     * startNight(nextNight), Extras and Quit. Enable Continue only when totalRuns>0; install hover
     * and up/down/Enter selection. No database rows are erased.
     * Related: [ui/Navigation.java:68 (startNight)](Navigation.java#L68) (start request contract);
     * [Application.java:358 (startNight)](../Application.java#L358) (handles the New Game/Continue callbacks);
     * [persistence/Progress.java:27 (Progress)](../persistence/Progress.java#L27) (nextNight and totalRuns values).
     * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     */
    public MenuPanel(AssetManager assets, Navigation navigation, Settings settings, Progress progress) {
        this.assets = assets;
        this.settings = settings;
        this.progress = progress;
        // New Game starts Night 1 without erasing history; Continue starts the next unlocked night. | [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20). | Related: [Application.java:271 (MenuPanel call)](../Application.java#L271) (caller of MenuPanel); [ui/Navigation.java:68 (startNight)](Navigation.java#L68) (used by MenuPanel).
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

    // Share selection between mouse hover and keyboard navigation. | [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20). | Related: [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (selection and activation state); [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (Up/Down/Enter key bindings).
    /*
     * Create a text-style button for a supplied navigation callback, capture its list index for
     * hover selection, and register it at a logical position. The callback passed here is where menu
     * functionality starts.
     * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (selection and activation state);
     * [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (Up/Down/Enter key bindings);
     * [Application.java:358 (startNight)](../Application.java#L358) (eventual start-button handler).
     */
    private GameButton entry(String text, int y, Runnable action) {
        int index = entries.size();
        GameButton button = new GameButton(text, GameButton.Style.TEXT, action);
        button.setFocusable(false);
        button.addMouseListener(new MouseAdapter() {
            /*
             * If the hovered menu button is enabled, select its captured entry index. Hover and
             * keyboard navigation therefore update the same highlight state.
             * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
             * Related: [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (selection and activation state);
             * [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (Up/Down/Enter key bindings);
             * [Application.java:358 (startNight)](../Application.java#L358) (eventual start-button handler).
             */
            @Override public void mouseEntered(MouseEvent event) { if (button.isEnabled()) select(index); }
        });
        entries.add(place(button, 103, y, 450, 62));
        return button;
    }

    // Wrap around the menu and skip disabled entries such as Continue before the first attempt. | [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20). | Related: [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (selection and activation state); [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (Up/Down/Enter key bindings).
    /*
     * Wrap the selected index using floorMod and skip disabled entries. Continue is disabled for
     * zero recorded attempts, so keyboard navigation cannot select it prematurely.
     * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (selection and activation state);
     * [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (Up/Down/Enter key bindings);
     * [Application.java:358 (startNight)](../Application.java#L358) (eventual start-button handler).
     */
    private void move(int direction) {
        int index = selected;
        do { index = Math.floorMod(index + direction, entries.size()); } while (!entries.get(index).isEnabled());
        select(index);
    }

    /*
     * Store the current index and update every button selected state. Mouse hover and keyboard
     * movement share the same visible selection.
     * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (selection and activation state);
     * [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (Up/Down/Enter key bindings);
     * [Application.java:358 (startNight)](../Application.java#L358) (eventual start-button handler).
     */
    private void select(int index) {
        selected = index;
        for (int i = 0; i < entries.size(); i++) entries.get(i).setSelected(i == index);
    }

    /*
     * Draw the chosen title portrait, title, completion stars and next-night text from saved
     * Progress. Drawing progress does not itself unlock or save anything.
     * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [assets/AssetManager.java:141 (menuRender)](../assets/AssetManager.java#L141) (portrait lookup);
     * [persistence/Progress.java:27 (Progress)](../persistence/Progress.java#L27) (stars and next-night summary);
     * [ui/Theme.java:131 (text)](Theme.java#L131) (title typography).
     */
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

    /*
     * Draw the title VHS overlay at panel age and skip it when reducedEffects is enabled.
     * [p. 20: MenuPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [ui/ScenePanel.java:189 (paintChildren)](ScenePanel.java#L189) (overlay hook caller);
     * [ui/VhsOverlay.java:49 (paint)](VhsOverlay.java#L49) (title interference).
     */
    @Override protected void renderOverlay(Graphics2D g) {
        VhsOverlay.paint(g, assets, age, 0.46f, settings.reducedEffects());
    }
}
