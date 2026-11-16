package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.GameSession;

import java.awt.*;

/** Shows the same session's paused state; Application performs resume or abandoned-shift saving. Reviewer (42-page edition): [p. 24: PausePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). */
/*
 * PausePanel
 *
 * Purpose: Displays a frozen active attempt and routes Resume or Return to Menu.
 *
 * Execution flow: Escape in gameplay -> Application.pauseShift -> PausePanel; Resume/Escape ->
 * resumeShift; Return to Menu -> leaveShift confirmation/save.
 *
 * Design rationale: Reuse the same model when resuming. Leaving creates ABANDONED,
 * so pause is different from completing an attempt.
 *
 * State: session is read for night and precise clock; the panel owns no database.
 *
 * Summary: Clock, drain, encounter and camera sweep stop. Resuming preserves memory; closing
 * the app does not create a checkpoint.
 * Reviewer (42-page edition): [p. 24: PausePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
 */
public final class PausePanel extends BackdropPanel {
    private final GameSession session;

    /*
     * Retain the existing GameSession for display and install Resume/Escape -> resumeShift plus
     * Return to Menu -> leaveShift. This screen does not create a new attempt.
     * Reviewer (42-page edition): [p. 24: PausePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     */
    public PausePanel(AssetManager assets, Navigation navigation, GameSession session) {
        super(assets);
        this.session = session;
        place(new GameButton("Resume", GameButton.Style.TEXT, navigation::resumeShift), 423, 350, 450, 60);
        place(new GameButton("RETURN TO MENU", GameButton.Style.SECONDARY, navigation::leaveShift), 485, 439, 315, 46);
        bind("ESCAPE", "resume", navigation::resumeShift);
    }

    /*
     * Draw the frozen session night/precise clock and explain paused/abandoned behavior. Resume and
     * Return to Menu callbacks were installed by the constructor.
     * Reviewer (42-page edition): [p. 24: PausePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     */
    @Override protected void render(Graphics2D g) {
        backdrop(g);
        Theme.text(g, "Paused", 530, 265, Theme.pixel(76), Color.WHITE);
        Theme.text(g, "Night %d  /  %s".formatted(session.night(), session.preciseClock()), 510, 310, Theme.pixel(28), Color.WHITE);
        Theme.text(g, "Clock, power and Sir Gudo are stopped.", 451, 535, Theme.pixel(25), Theme.MUTED);
        Theme.text(g, "Leaving records an unfinished attempt.", 451, 572, Theme.pixel(24), Theme.MUTED);
    }
}
