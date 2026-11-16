package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Scene;

import java.awt.*;

/** Common dim office background, page headings, and back navigation for secondary screens. [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22). Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (shared painting skeleton); [ui/ExtrasPanel.java:37 (ExtrasPanel)](ExtrasPanel.java#L37) (subclass using page chrome). */
/*
 * BackdropPanel
 *
 * Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (shared painting skeleton);
 * [ui/ExtrasPanel.java:37 (ExtrasPanel)](ExtrasPanel.java#L37) (subclass using page chrome);
 * [ui/Navigation.java:48 (menu)](Navigation.java#L48) (Back button request);
 * [ui/Theme.java:214 (cover)](Theme.java#L214) (background image drawing).
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
 * [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
 */
public abstract class BackdropPanel extends ScenePanel {
    protected final AssetManager assets;

    /*
     * Pass through ScenePanel initialization and retain the shared AssetManager for secondary-screen
     * backgrounds.
     * [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     * Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (shared painting skeleton);
     * [ui/ExtrasPanel.java:37 (ExtrasPanel)](ExtrasPanel.java#L37) (subclass using page chrome);
     * [ui/Navigation.java:48 (menu)](Navigation.java#L48) (Back button request).
     */
    protected BackdropPanel(AssetManager assets) { this.assets = assets; }

    /*
     * Fill black, draw the clean office as a cover image, then add a heavy black tint. It is a fixed
     * background for secondary screens, not a live GameSession view.
     * [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     * Related: [ui/Theme.java:214 (cover)](Theme.java#L214) (used by backdrop);
     * [assets/AssetManager.java:113 (scene)](../assets/AssetManager.java#L113) (used by backdrop);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (shared painting skeleton).
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
     * [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     * Related: [ui/Theme.java:131 (text)](Theme.java#L131) (used by pageChrome);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (shared painting skeleton);
     * [ui/ExtrasPanel.java:37 (ExtrasPanel)](ExtrasPanel.java#L37) (subclass using page chrome).
     */
    protected void pageChrome(Graphics2D g, String title, String subtitle) {
        backdrop(g);
        Theme.text(g, title, 80, 158, Theme.pixel(52), Theme.PAPER);
        Theme.text(g, subtitle, 82, 192, Theme.sans(16), Theme.MUTED);
        Theme.text(g, "v 0.1", 20, 706, Theme.pixel(21), Theme.MUTED);
    }

    // Both the Back button and Escape request the menu through the same navigation interface. | [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22). | Related: [ui/Navigation.java:48 (menu)](Navigation.java#L48) (used by backButton); [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (created by backButton).
    /*
     * Install the Back GameButton and Escape binding, both pointing to navigation.menu. They return
     * to the title menu rather than to the previous Extras card.
     * [p. 22: BackdropPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     * Related: [ui/Navigation.java:48 (menu)](Navigation.java#L48) (used by backButton);
     * [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (created by backButton);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (shared painting skeleton).
     */
    protected void backButton(Navigation navigation) {
        place(new GameButton("← BACK", GameButton.Style.QUIET, navigation::menu), 72, 38, 112, 38);
        bind("ESCAPE", "back", navigation::menu);
    }
}
