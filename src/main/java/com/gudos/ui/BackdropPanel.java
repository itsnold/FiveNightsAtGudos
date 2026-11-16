package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Scene;

import java.awt.*;

/** Common dim office background, page headings, and back navigation for secondary screens. Reviewer (42-page edition): [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22). */
/*
 * BackdropPanel
 *
 * Purpose: Shares the dim office background and navigation chrome for secondary pages.
 *
 * Execution flow: Extras, settings, guide/credits, selection, pause, records and result reuse
 * backdrop or pageChrome.
 *
 * Design rationale: Avoid duplicating the same drawing and Back binding. It is an
 * abstract base class; inheritance alone is not the custom Decorator pattern.
 *
 * State: assets is inherited by secondary screens; pageChrome draws heading, subtitle
 * and version.
 *
 * Summary: Back and Escape request navigation.menu. This implementation returns to the title
 * menu, even when the page was opened through Extras.
 * Reviewer (42-page edition): [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
 */
public abstract class BackdropPanel extends ScenePanel {
    protected final AssetManager assets;

    /*
     * Pass through ScenePanel initialization and retain the shared AssetManager for secondary-screen
     * backgrounds.
     * Reviewer (42-page edition): [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     */
    protected BackdropPanel(AssetManager assets) { this.assets = assets; }

    /*
     * Fill black, draw the clean office as a cover image, then add a heavy black tint. It is a fixed
     * background for secondary screens, not a live GameSession view.
     * Reviewer (42-page edition): [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     */
    protected void backdrop(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Theme.cover(g, assets.scene(Scene.OFFICE, "default"), 0, 0, WIDTH, HEIGHT, 0.64);
        g.setColor(new Color(0, 0, 0, 235));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    /*
     * Reuse the backdrop, then draw heading/subtitle/version at consistent logical coordinates.
     * Subclasses add their page-specific controls/content.
     * Reviewer (42-page edition): [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     */
    protected void pageChrome(Graphics2D g, String title, String subtitle) {
        backdrop(g);
        Theme.text(g, title, 80, 158, Theme.pixel(52), Theme.PAPER);
        Theme.text(g, subtitle, 82, 192, Theme.sans(16), Theme.MUTED);
        Theme.text(g, "v 0.1", 20, 706, Theme.pixel(21), Theme.MUTED);
    }

    // Both the Back button and Escape request the menu through the same navigation interface. | Reviewer (42-page edition): [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
    /*
     * Install the Back GameButton and Escape binding, both pointing to navigation.menu. They return
     * to the title menu rather than to the previous Extras card.
     * Reviewer (42-page edition): [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     */
    protected void backButton(Navigation navigation) {
        place(new GameButton("← BACK", GameButton.Style.QUIET, navigation::menu), 72, 38, 112, 38);
        bind("ESCAPE", "back", navigation::menu);
    }
}
