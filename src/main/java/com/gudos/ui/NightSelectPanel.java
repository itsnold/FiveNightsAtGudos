package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.persistence.Progress;

import java.awt.*;

/** Displays five nights; buttons are enabled from the current profile's saved progress. [p. 21: NightSelectPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21). Related: [Application.java:358 (startNight)](../Application.java#L358) (handles the chosen night); [persistence/Progress.java:38 (unlocked)](../persistence/Progress.java#L38) (eligibility rule). */
/*
 * NightSelectPanel
 *
 * Related: [Application.java:358 (startNight)](../Application.java#L358) (handles the chosen night);
 * [persistence/Progress.java:38 (unlocked)](../persistence/Progress.java#L38) (eligibility rule);
 * [ui/Navigation.java:68 (startNight)](Navigation.java#L68) (request contract).
 *
 * Purpose: Shows five night buttons gated by the loaded progress.
 *
 * Execution flow: Extras -> Select Night -> enabled Night N -> navigation.startNight(N).
 *
 * Design rationale: UI disables locked nights; Application rechecks unlocked before
 * starting. Capture the loop value in selected so each listener keeps its own night.
 *
 * State: progress supplies nextNight/highestCompleted; the page renders Cleared, Locked
 * or no status.
 *
 * Summary: Disabling a button is user feedback. The coordinator guard is the second boundary
 * that rejects a locked start request.
 * [p. 21: NightSelectPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21).
 */
public final class NightSelectPanel extends BackdropPanel {
    private final Progress progress;

    /*
     * Create five night buttons with captured numeric callbacks to Navigation.startNight. Set each
     * enabled state from Progress.unlocked; Application rechecks eligibility.
     * [p. 21: NightSelectPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21).
     * Related: [Application.java:323 (NightSelectPanel call)](../Application.java#L323) (caller of NightSelectPanel);
     * [ui/Navigation.java:68 (startNight)](Navigation.java#L68) (used by NightSelectPanel);
     * [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (button API used by NightSelectPanel).
     */
    public NightSelectPanel(AssetManager assets, Navigation navigation, Progress progress) {
        super(assets);
        this.progress = progress;
        backButton(navigation);
        for (int night = 1; night <= 5; night++) {
            // Capture this iteration's value for the callback; the loop variable itself keeps changing. | [p. 21: NightSelectPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21). | Related: [Application.java:323 (NightSelectPanel call)](../Application.java#L323) (caller of NightSelectPanel); [ui/Navigation.java:68 (startNight)](Navigation.java#L68) (used by NightSelectPanel).
            int selected = night;
            GameButton button = new GameButton("Night " + night, GameButton.Style.TEXT, () -> navigation.startNight(selected));
            button.setFont(Theme.pixel(44));
            button.setEnabled(progress.unlocked(night));
            place(button, 105, 227 + (night - 1) * 68, 450, 55);
        }
    }

    /*
     * Show the saved selection page; label nights Cleared or Locked using loaded progress and draw
     * the increasing-difficulty hint. Button callbacks were created with a captured night value.
     * [p. 21: NightSelectPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21).
     * Related: [persistence/Progress.java:27 (highestCompleted)](../persistence/Progress.java#L27) (used by render);
     * [ui/Theme.java:131 (text)](Theme.java#L131) (used by render);
     * [Application.java:358 (startNight)](../Application.java#L358) (handles the chosen night).
     */
    @Override protected void render(Graphics2D g) {
        pageChrome(g, "Select Night", "Finish a shift to unlock the next. Replaying does not reset your progress.");
        for (int night = 1; night <= 5; night++) {
            String status = night <= progress.highestCompleted() ? "* Cleared" : !progress.unlocked(night) ? "Locked" : "";
            Theme.text(g, status, 478, 265 + (night - 1) * 68, Theme.pixel(28), progress.unlocked(night) ? Color.WHITE : Color.GRAY);
        }
        Theme.text(g, "Sir Gudo moves faster each night. Face him and hold F to survive.", 170, 630, Theme.pixel(25), Theme.MUTED);
    }
}
