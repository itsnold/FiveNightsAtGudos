package com.gudos.ui;

import com.gudos.assets.AssetManager;

import java.awt.*;

/** Links the Extras menu's labels to the corresponding Navigation.Page requests. [p. 21: ExtrasPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21). Related: [ui/Navigation.java:39 (Page)](Navigation.java#L39) (page identifiers); [Application.java:319 (page)](../Application.java#L319) (handles secondary-page requests). */
/*
 * ExtrasPanel
 *
 * Related: [Application.java:319 (page)](../Application.java#L319) (handles secondary-page requests);
 * [ui/Navigation.java:39 (Page)](Navigation.java#L39) (page identifiers);
 * [ui/BackdropPanel.java:85 (backButton)](BackdropPanel.java#L85) (shared background and Back action).
 *
 * Purpose: Routes five text entries to secondary pages.
 *
 * Execution flow: Select Night, How to Play, Shift Records, Settings and Credits map in order to
 * Navigation.Page values.
 *
 * Design rationale: Keep a small routing screen without placing database or
 * gameplay logic here. Capturing index per loop keeps each callback attached to the correct page.
 *
 * State: Labels and Page arrays must stay in the same order.
 *
 * Summary: Extras is an entry menu; Application.page chooses and loads the actual panel.
 * [p. 21: ExtrasPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21).
 */
public final class ExtrasPanel extends BackdropPanel {
    /*
     * Install shared Back and build the five label/Page pairs. Capture each iteration index for its
     * callback so buttons navigate to the intended page.
     * [p. 21: ExtrasPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21).
     * Related: [Application.java:322 (ExtrasPanel call)](../Application.java#L322) (caller of ExtrasPanel);
     * [ui/Navigation.java:57 (page)](Navigation.java#L57) (used by ExtrasPanel);
     * [ui/GameButton.java:52 (GameButton)](GameButton.java#L52) (button API used by ExtrasPanel).
     */
    public ExtrasPanel(AssetManager assets, Navigation navigation) {
        super(assets);
        backButton(navigation);
        String[] labels = {"Select Night", "How to Play", "Shift Records", "Settings", "Credits"};
        Navigation.Page[] pages = {Navigation.Page.NIGHTS, Navigation.Page.GUIDE, Navigation.Page.RECORDS, Navigation.Page.SETTINGS, Navigation.Page.CREDITS};
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            GameButton button = new GameButton(labels[i], GameButton.Style.TEXT, () -> navigation.page(pages[index]));
            button.setFont(Theme.pixel(42));
            place(button, 104, 236 + i * 66, 620, 55);
        }
    }

    /*
     * Draw the shared Extras page chrome. The five real navigation buttons were constructed earlier
     * and Swing paints them as children.
     * [p. 21: ExtrasPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-21).
     * Related: [Application.java:319 (page)](../Application.java#L319) (handles secondary-page requests);
     * [ui/Navigation.java:39 (Page)](Navigation.java#L39) (page identifiers);
     * [ui/BackdropPanel.java:85 (backButton)](BackdropPanel.java#L85) (shared background and Back action).
     */
    @Override protected void render(Graphics2D g) {
        pageChrome(g, "Extras", "");
    }
}
