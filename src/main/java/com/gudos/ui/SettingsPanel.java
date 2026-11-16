package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.persistence.AppPaths;
import com.gudos.persistence.Settings;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/** Edits a copy of the current preferences; Application owns saving and applying the submitted values. [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). Related: [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply); [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values). */
/*
 * SettingsPanel
 *
 * Related: [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
 * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
 * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source);
 * [persistence/SettingsStore.java:103 (save)](../persistence/SettingsStore.java#L103) (writes preferences).
 *
 * Purpose: Edits preferences in real Swing controls and submits an immutable Settings value.
 *
 * Execution flow: Extras -> Settings -> edit name/volume/effects/fullscreen -> Apply ->
 * navigation.saveSettings(values()).
 *
 * Design rationale: DocumentFilter sanitizes typed/pasted names, and Settings
 * validates again. Applying is explicit; Back does not save edits. The clipboard button copies the
 * resolved data folder.
 *
 * State: JTextField name, JSlider volume, JCheckBox reducedEffects/fullscreen,
 * GameButton apply, AppPaths paths and status. Nested CheckIcon customizes appearance while
 * JCheckBox retains behavior.
 *
 * Summary: The view gathers values; Application performs IO and switches profiles only after
 * the save path succeeds.
 * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
 */
public final class SettingsPanel extends BackdropPanel {
    private final JTextField name;
    private final JSlider volume;
    private final JCheckBox reducedEffects;
    private final JCheckBox fullscreen;
    private final GameButton apply;
    private final AppPaths paths;
    private String status = "Preferences are written to settings.txt when you apply.";

    /*
     * Build real name/volume/effects/fullscreen controls from current Settings. Install a sanitizing
     * DocumentFilter, custom slider/checkbox art, Apply -> navigation.saveSettings(values()), and
     * Copy Data Folder. Editing does not save until Apply.
     * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [Application.java:326 (SettingsPanel call)](../Application.java#L326) (caller of SettingsPanel);
     * [persistence/Settings.java:26 (playerName)](../persistence/Settings.java#L26) (used by SettingsPanel);
     * [ui/Theme.java:85 (sans)](Theme.java#L85) (used by SettingsPanel).
     */
    public SettingsPanel(AssetManager assets, Navigation navigation, Settings settings, AppPaths paths) {
        super(assets);
        this.paths = paths;
        backButton(navigation);
        name = new JTextField(settings.playerName());
        name.setFont(Theme.sans(20));
        name.setForeground(Theme.PAPER);
        name.setBackground(new Color(12, 12, 12));
        name.setCaretColor(Theme.AMBER);
        name.setSelectionColor(new Color(75, 75, 75));
        name.setBorder(new CompoundBorder(new LineBorder(Theme.LINE), new EmptyBorder(4, 13, 4, 13)));
        name.getAccessibleContext().setAccessibleName("Player name, up to 32 characters");
        // Filter pasted/typed names before display: remove control characters and cap at 32 characters. | [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [Application.java:326 (SettingsPanel call)](../Application.java#L326) (caller of SettingsPanel); [persistence/Settings.java:26 (playerName)](../persistence/Settings.java#L26) (used by SettingsPanel).
        ((AbstractDocument) name.getDocument()).setDocumentFilter(new DocumentFilter() {
            /*
             * Route inserted text through the same replace filter so typing and paste share
             * sanitization and the name-length cap.
             * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
             * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
             * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
             * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source).
             */
            @Override public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes) throws BadLocationException {
                replace(bypass, offset, 0, text, attributes);
            }
            /*
             * Remove control characters, compute capacity after replacing the selected span,
             * truncate inserted text to the remaining 32-character allowance and call the bypass.
             * Settings validates again at submission/load.
             * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
             * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
             * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
             * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source).
             */
            @Override public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
                String clean = text == null ? "" : text.replaceAll("\\p{Cntrl}", "");
                int remaining = Math.max(0, 32 - (bypass.getDocument().getLength() - length));
                bypass.replace(offset, length, clean.substring(0, Math.min(clean.length(), remaining)), attributes);
            }
        });
        place(name, 82, 254, 637, 49);
        volume = new JSlider(0, 100, settings.volume());
        volume.setOpaque(false);
        volume.setForeground(Theme.AMBER);
        volume.setUI(new BasicSliderUI(volume) {
            /*
             * Draw the slider base track and filled section up to the current value using a copied
             * Graphics2D. This replaces appearance, not the JSlider value model.
             * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
             * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
             * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
             * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source).
             */
            @Override public void paintTrack(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(new Color(75, 75, 75));
                g.fillRoundRect(trackRect.x, trackRect.y + trackRect.height / 2 - 2, trackRect.width, 4, 4, 4);
                g.setColor(Theme.AMBER);
                g.fillRoundRect(trackRect.x, trackRect.y + trackRect.height / 2 - 2, xPositionForValue(volume.getValue()) - trackRect.x, 4, 4, 4);
                g.dispose();
            }
            /*
             * Draw the custom slider handle with focus-aware color, then dispose the graphics copy.
             * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
             * Related: [ui/Theme.java:114 (quality)](Theme.java#L114) (used by paintThumb);
             * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
             * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply).
             */
            @Override public void paintThumb(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.quality(g);
                g.setColor(volume.isFocusOwner() ? Theme.PAPER : Theme.AMBER);
                g.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, 3, 3);
                g.dispose();
            }
            /*
             * Intentionally draw no default focus ring for this slider UI. The custom thumb color
             * still reflects focus.
             * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
             * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
             * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
             * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source).
             */
            @Override public void paintFocus(Graphics graphics) { }
        });
        volume.getAccessibleContext().setAccessibleName("Master volume");
        place(volume, 79, 378, 640, 36);
        reducedEffects = checkbox("Reduced visual effects", settings.reducedEffects());
        fullscreen = checkbox("Borderless fullscreen", settings.fullscreen());
        place(reducedEffects, 79, 451, 640, 36);
        place(fullscreen, 79, 524, 640, 36);
        // Editing controls alone does not save; Apply submits a new immutable Settings value. | [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [Application.java:326 (SettingsPanel call)](../Application.java#L326) (caller of SettingsPanel); [persistence/Settings.java:26 (playerName)](../persistence/Settings.java#L26) (used by SettingsPanel).
        apply = place(new GameButton("APPLY SETTINGS    →", GameButton.Style.PRIMARY, () -> navigation.saveSettings(values())), 82, 595, 280, 44);
        // Copy the actual resolved save directory, including any gudo.dataDir launch override. | [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [Application.java:326 (SettingsPanel call)](../Application.java#L326) (caller of SettingsPanel); [persistence/Settings.java:26 (playerName)](../persistence/Settings.java#L26) (used by SettingsPanel).
        place(new GameButton("COPY DATA FOLDER", GameButton.Style.SECONDARY, () -> {
            try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(paths.directory().toString()), null); }
            catch (IllegalStateException error) { JOptionPane.showMessageDialog(this, "Clipboard unavailable.\n" + paths.directory()); }
        }), 817, 551, 353, 42);
    }

    /*
     * Create a real JCheckBox with transparent background, the shared font and custom
     * checked/unchecked icons. Swing still manages selection and input.
     * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [ui/Theme.java:85 (sans)](Theme.java#L85) (used by checkbox);
     * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
     * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply).
     */
    private JCheckBox checkbox(String label, boolean selected) {
        JCheckBox box = new JCheckBox(label, selected);
        box.setOpaque(false);
        box.setForeground(Theme.PAPER);
        box.setFont(Theme.sans(18));
        box.setIcon(new CheckIcon(false));
        box.setSelectedIcon(new CheckIcon(true));
        box.setIconTextGap(15);
        box.setFocusPainted(false);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return box;
    }

    // Custom checkbox artwork; the JCheckBox still handles selection and input normally. | [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values); [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply).
    /*
     * CheckIcon
     *
     * A value carrying checked state while implementing Swing Icon drawing; JCheckBox owns
     * interaction.
     * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
     * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
     * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source).
     */
    private record CheckIcon(boolean checked) implements Icon {
        /*
         * Report the custom checkbox icon width of 22 logical pixels to Swing layout.
         * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
         * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
         * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
         * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source).
         */
        @Override public int getIconWidth() { return 22; }
        /*
         * Report the custom checkbox icon height of 22 logical pixels to Swing layout.
         * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
         * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
         * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply);
         * [persistence/AppPaths.java:32 (AppPaths)](../persistence/AppPaths.java#L32) (Copy Data Folder source).
         */
        @Override public int getIconHeight() { return 22; }
        /*
         * Draw the custom checkbox square/focus color and checkmark from the record checked flag.
         * Copy/dispose graphics so the icon does not alter later painting.
         * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
         * Related: [ui/Theme.java:114 (quality)](Theme.java#L114) (used by paintIcon);
         * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values);
         * [Application.java:621 (saveSettings)](../Application.java#L621) (handles Apply).
         */
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.quality(g);
            g.setColor(checked ? Theme.PAPER : new Color(15, 15, 15));
            g.fillRect(x, y, 21, 21);
            g.setColor(component.isFocusOwner() ? Theme.PAPER : checked ? Theme.AMBER : Theme.MUTED);
            g.drawRect(x, y, 21, 21);
            if (checked) {
                g.setColor(Theme.BACKGROUND);
                g.setStroke(new BasicStroke(2));
                g.drawLine(x + 5, y + 11, x + 9, y + 15);
                g.drawLine(x + 9, y + 15, x + 17, y + 6);
            }
            g.dispose();
        }
    }

    // Read the form as one value object; Settings validates/clamps the values again. | [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalizes edited values); [Application.java:621 (saveSettings)](../Application.java#L621) (receives the submitted form).
    /*
     * Read all four form controls and construct a new validated Settings value. Reading the form
     * alone does not persist it.
     * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalizes edited values);
     * [Application.java:621 (saveSettings)](../Application.java#L621) (receives the submitted form).
     */
    public Settings values() { return new Settings(name.getText(), volume.getValue(), reducedEffects.isSelected(), fullscreen.isSelected()); }

    /*
     * Update the checkbox after Application successfully persists/applies an F11 change while this
     * form is visible.
     * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [Application.java:654 (toggleFullscreen)](../Application.java#L654) (updates checkbox after F11 save);
     * [persistence/Settings.java:55 (withFullscreen)](../persistence/Settings.java#L55) (fullscreen value).
     */
    public void setFullscreen(boolean value) { fullscreen.setSelected(value); }

    // Prevent another Apply request while the storage worker is still processing this one. | [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [Application.java:621 (saveSettings)](../Application.java#L621) (disables Apply around IO); [persistence/SettingsStore.java:103 (save)](../persistence/SettingsStore.java#L103) (preference write being awaited).
    /*
     * Disable Apply while an async save is active and update status text. Application owns the busy
     * guard as well as this visual feedback.
     * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [Application.java:621 (saveSettings)](../Application.java#L621) (disables Apply around IO);
     * [persistence/SettingsStore.java:103 (save)](../persistence/SettingsStore.java#L103) (preference write being awaited).
     */
    public void setSaving(boolean saving) {
        apply.setEnabled(!saving);
        status = saving ? "Saving preferences…" : "Preferences are written to settings.txt when you apply.";
    }

    /*
     * Draw labels/current volume, explain the three save files, show the resolved folder and status.
     * Actual controls and Apply callback remain ordinary Swing children.
     * [p. 25: SettingsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [ui/Theme.java:150 (tracked)](Theme.java#L150) (used by render);
     * [persistence/AppPaths.java:32 (directory)](../persistence/AppPaths.java#L32) (used by render);
     * [persistence/Settings.java:26 (Settings)](../persistence/Settings.java#L26) (normalized form values).
     */
    @Override protected void render(Graphics2D g) {
        pageChrome(g, "Options", "");
        Theme.tracked(g, "PLAYER NAME", 82, 235, 11, Theme.AMBER);
        Theme.text(g, "Different names use separate local records and progress.", 83, 329, Theme.sans(14), Theme.MUTED);
        Theme.tracked(g, "MASTER VOLUME", 82, 370, 11, Theme.AMBER);
        Theme.text(g, volume.getValue() + "%", 666, 370, Theme.mono(17), Theme.PAPER);
        Theme.text(g, "Disables camera noise, scanlines, blinking and animated transitions.", 118, 508, Theme.sans(13), Theme.MUTED);
        Theme.text(g, "You can also toggle this with F11.", 118, 580, Theme.sans(13), Theme.MUTED);

        Theme.text(g, "Saved on this computer", 818, 260, Theme.pixel(29), Color.WHITE);
        Theme.paragraph(g, "settings.txt\nYour preferences.\n\nshift-log.txt\nA report after every attempt.\n\ngudo.db\nLocal profiles, progress and history.",
                818, 307, 350, 23, Theme.pixel(23), Theme.MUTED);
        g.setFont(Theme.mono(11));
        Theme.text(g, Theme.ellipsize(g, paths.directory().toString(), 350), 818, 530, Theme.mono(11), Theme.MUTED);
        Theme.text(g, status, 383, 622, Theme.sans(13), Theme.MUTED);
    }
}
