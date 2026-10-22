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

public final class SettingsPanel extends BackdropPanel {
    private final JTextField name;
    private final JSlider volume;
    private final JCheckBox reducedEffects;
    private final JCheckBox fullscreen;
    private final GameButton apply;
    private final AppPaths paths;
    private String status = "Preferences are written to settings.txt when you apply.";

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
        ((AbstractDocument) name.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes) throws BadLocationException {
                replace(bypass, offset, 0, text, attributes);
            }
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
            @Override public void paintTrack(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(new Color(75, 75, 75));
                g.fillRoundRect(trackRect.x, trackRect.y + trackRect.height / 2 - 2, trackRect.width, 4, 4, 4);
                g.setColor(Theme.AMBER);
                g.fillRoundRect(trackRect.x, trackRect.y + trackRect.height / 2 - 2, xPositionForValue(volume.getValue()) - trackRect.x, 4, 4, 4);
                g.dispose();
            }
            @Override public void paintThumb(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.quality(g);
                g.setColor(volume.isFocusOwner() ? Theme.PAPER : Theme.AMBER);
                g.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, 3, 3);
                g.dispose();
            }
            @Override public void paintFocus(Graphics graphics) { }
        });
        volume.getAccessibleContext().setAccessibleName("Master volume");
        place(volume, 79, 378, 640, 36);
        reducedEffects = checkbox("Reduced visual effects", settings.reducedEffects());
        fullscreen = checkbox("Borderless fullscreen", settings.fullscreen());
        place(reducedEffects, 79, 451, 640, 36);
        place(fullscreen, 79, 524, 640, 36);
        apply = place(new GameButton("APPLY SETTINGS    →", GameButton.Style.PRIMARY, () -> navigation.saveSettings(values())), 82, 595, 280, 44);
        place(new GameButton("COPY DATA FOLDER", GameButton.Style.SECONDARY, () -> {
            try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(paths.directory().toString()), null); }
            catch (IllegalStateException error) { JOptionPane.showMessageDialog(this, "Clipboard unavailable.\n" + paths.directory()); }
        }), 817, 551, 353, 42);
    }

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

    private record CheckIcon(boolean checked) implements Icon {
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
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

    public Settings values() { return new Settings(name.getText(), volume.getValue(), reducedEffects.isSelected(), fullscreen.isSelected()); }

    public void setFullscreen(boolean value) { fullscreen.setSelected(value); }

    public void setSaving(boolean saving) {
        apply.setEnabled(!saving);
        status = saving ? "Saving preferences…" : "Preferences are written to settings.txt when you apply.";
    }

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
