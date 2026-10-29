package com.gudos.ui;

import com.gudos.assets.AssetManager;

import java.awt.*;

/** Links the Extras menu's labels to the corresponding Navigation.Page requests. */
public final class ExtrasPanel extends BackdropPanel {
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

    @Override protected void render(Graphics2D g) {
        pageChrome(g, "Extras", "");
    }
}
