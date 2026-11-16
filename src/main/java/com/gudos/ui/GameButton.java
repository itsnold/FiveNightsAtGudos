package com.gudos.ui;

import javax.swing.*;
import java.awt.*;

/** A normal Swing button with custom painting and a caller-supplied action. [p. 22: GameButton](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22). Related: [ui/MenuPanel.java:132 (entry)](MenuPanel.java#L132) (text-style button client); [ui/GamePanel.java:108 (GamePanel)](GamePanel.java#L108) (monitor, light and camera button clients). */
/*
 * GameButton
 *
 * Related: [ui/MenuPanel.java:132 (entry)](MenuPanel.java#L132) (text-style button client);
 * [ui/GamePanel.java:108 (GamePanel)](GamePanel.java#L108) (monitor, light and camera button clients);
 * [ui/ScenePanel.java:79 (place)](ScenePanel.java#L79) (positions and scales controls);
 * [ui/Theme.java:76 (pixel)](Theme.java#L76) (shared typography).
 *
 * Purpose: Uses a real JButton with custom visual styles and a supplied action.
 *
 * Execution flow: Screens construct the button with label/style/callback; Swing fires
 * ActionListener; the callback requests navigation or model input.
 *
 * Design rationale: Preserve enabled state, keyboard/button behavior and
 * accessibility while drawing the game style. Painting only changes appearance, not game rules.
 *
 * State: Style: PRIMARY, SECONDARY, QUIET, CAMERA, TEXT, MONITOR, LIGHT. The inherited
 * button model owns selected/pressed/hover states.
 *
 * Summary: A label is not the implementation. Follow the callback passed into the GameButton
 * constructor to find the actual feature.
 * [p. 22: GameButton](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
 */
public final class GameButton extends JButton {
    /*
     * Style
     *
     * Named button appearances; input behavior remains in JButton and the supplied callback.
     * [p. 22: GameButton](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     * Related: [ui/MenuPanel.java:132 (entry)](MenuPanel.java#L132) (text-style button client);
     * [ui/GamePanel.java:108 (GamePanel)](GamePanel.java#L108) (monitor, light and camera button clients);
     * [ui/ScenePanel.java:79 (place)](ScenePanel.java#L79) (positions and scales controls).
     */
    public enum Style { PRIMARY, SECONDARY, QUIET, CAMERA, TEXT, MONITOR, LIGHT }
    private final Style style;

    /*
     * Initialize a real JButton, configure its font/style/accessibility/cursor/transparent painting,
     * and install one ActionListener that invokes the supplied Runnable. This retains Swing
     * enabled/selected/input semantics while custom paintComponent supplies appearance.
     * [p. 22: GameButton](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     * Related: [ui/BackdropPanel.java:86 (GameButton call)](BackdropPanel.java#L86) (caller of GameButton);
     * [ui/ExtrasPanel.java:44 (GameButton call)](ExtrasPanel.java#L44) (caller of GameButton);
     * [ui/Theme.java:76 (pixel)](Theme.java#L76) (used by GameButton).
     */
    public GameButton(String text, Style style, Runnable action) {
        super(text);
        this.style = style;
        setFont(Theme.pixel(style == Style.TEXT ? 52 : style == Style.CAMERA ? 22 : 26));
        setForeground(Theme.PAPER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        getAccessibleContext().setAccessibleName(text);
        // Swing delivers the click event; the callback decides which game/navigation action to request. | [p. 22: GameButton](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22). | Related: [ui/BackdropPanel.java:86 (GameButton call)](BackdropPanel.java#L86) (caller of GameButton); [ui/ExtrasPanel.java:44 (GameButton call)](ExtrasPanel.java#L44) (caller of GameButton).
        addActionListener(event -> action.run());
    }

    // Style selects the appearance; the inherited button model still owns pressed/selected states. | [p. 22: GameButton](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22). | Related: [ui/Theme.java:114 (quality)](Theme.java#L114) (used by paintComponent); [ui/MenuPanel.java:132 (entry)](MenuPanel.java#L132) (text-style button client).
    /*
     * Read the inherited Swing model enabled/hover/selected/pressed states, then draw the chosen
     * style. Monitor is a chevron tab, light is a hold indicator, camera is a two-line label, and
     * normal/text buttons use measured clipping/selection arrows. Copy/dispose Graphics2D and leave
     * click delivery to JButton; painting never starts a night or changes game rules.
     * [p. 22: GameButton](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-22).
     * Related: [ui/Theme.java:114 (quality)](Theme.java#L114) (used by paintComponent);
     * [ui/MenuPanel.java:132 (entry)](MenuPanel.java#L132) (text-style button client);
     * [ui/GamePanel.java:108 (GamePanel)](GamePanel.java#L108) (monitor, light and camera button clients).
     */
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.quality(g);
        boolean active = getModel().isRollover() || isFocusOwner() || isSelected();
        boolean pressed = getModel().isPressed();
        Color foreground = isEnabled() ? Theme.PAPER : new Color(95, 95, 95);
        int w = getWidth();
        int h = getHeight();
        if (style == Style.MONITOR) {
            g.setColor(new Color(0, 0, 0, active ? 115 : 50));
            g.fillRoundRect(2, 2, w - 5, h - 5, 7, 7);
            g.setColor(new Color(205, 205, 205, active ? 220 : 125));
            g.setStroke(new BasicStroke(Math.max(2, h / 16f)));
            g.drawRoundRect(2, 2, w - 5, h - 5, 7, 7);
            int half = Math.max(22, w / 10);
            int middle = w / 2;
            for (int row = 0; row < 2; row++) {
                int y = h / 4 + row * h / 4;
                g.drawPolyline(new int[]{middle - half, middle, middle + half}, new int[]{y, y + h / 6, y}, 3);
            }
            g.dispose();
            return;
        }
        if (style == Style.LIGHT) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(125, 125, 125));
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(isSelected() || pressed ? new Color(235, 245, 255) : new Color(69, 91, 135));
            g.fillRect(w / 5, h / 9, w * 3 / 5, h / 2);
            g.setColor(Color.WHITE);
            g.drawRect(w / 5, h / 9, w * 3 / 5, h / 2);
            g.setFont(getFont());
            g.drawString("LIGHT", (w - g.getFontMetrics().stringWidth("LIGHT")) / 2, h * 4 / 5);
            g.dispose();
            return;
        }
        if (style == Style.CAMERA || style == Style.PRIMARY || style == Style.SECONDARY) {
            g.setColor(style == Style.CAMERA && isSelected() ? new Color(103, 158, 39, 240)
                    : new Color(active ? 100 : 40, active ? 100 : 40, active ? 100 : 40, style == Style.CAMERA ? 235 : 155));
            g.fillRect(0, 0, w, h);
            g.setColor(isEnabled() ? Color.WHITE : new Color(100, 100, 100));
            g.setStroke(new BasicStroke(style == Style.CAMERA ? 2 : 1));
            g.drawRect(1, 1, w - 3, h - 3);
        }
        g.setFont(getFont());
        g.setColor(foreground);
        FontMetrics metrics = g.getFontMetrics();
        if (style == Style.CAMERA && getText().contains("\n")) {
            String[] lines = getText().split("\n");
            int lineHeight = Math.max(12, (int) (h * 0.4));
            int y = (h - lines.length * lineHeight) / 2 + metrics.getAscent() - 2;
            for (String line : lines) {
                g.drawString(line, (w - metrics.stringWidth(line)) / 2, y);
                y += lineHeight;
            }
        } else {
            boolean centered = style == Style.CAMERA || style == Style.QUIET;
            int padding = style == Style.TEXT ? (int) (getFont().getSize2D() * 1.23) : 17;
            String label = Theme.ellipsize(g, getText(), w - (centered ? 8 : padding + 8));
            int x = centered ? (w - metrics.stringWidth(label)) / 2 : padding;
            int y = (h - metrics.getHeight()) / 2 + metrics.getAscent();
            if (style == Style.TEXT && active && isEnabled()) g.drawString(">>", 0, y);
            g.drawString(label, x, y);
        }
        g.dispose();
    }
}
