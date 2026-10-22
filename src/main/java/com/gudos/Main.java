package com.gudos;

import com.gudos.assets.AssetManager;
import com.gudos.persistence.*;
import com.gudos.ui.LoadingPanel;
import com.gudos.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { /* Platform fallback remains usable. */ }
        UIManager.put("OptionPane.background", new Color(18, 18, 18));
        UIManager.put("Panel.background", new Color(18, 18, 18));
        UIManager.put("OptionPane.messageForeground", Theme.PAPER);
        UIManager.put("OptionPane.messageFont", Theme.sans(15));
        UIManager.put("Button.font", Theme.sans(14));
        UIManager.put("Button.background", new Color(55, 55, 55));
        UIManager.put("Button.foreground", Theme.PAPER);
        JFrame frame = new JFrame("Five Nights at Gudo's");
        frame.setIconImage(icon());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        LoadingPanel loading = new LoadingPanel("Getting the carinderia ready…");
        frame.setContentPane(loading);
        frame.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1296, screen.width - 40), Math.min(759, screen.height - 40));
        frame.setMinimumSize(new Dimension(Math.min(960, screen.width - 40), Math.min(580, screen.height - 40)));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Timer loadingTimer = new Timer(33, event -> { loading.tick(0.033); loading.repaint(); });
        loadingTimer.start();
        new SwingWorker<Boot, Void>() {
            @Override protected Boot doInBackground() throws Exception {
                AppPaths paths = AppPaths.resolve();
                SettingsStore store = new SettingsStore(paths.settings());
                Settings settings = store.load();
                Database database = new Database(paths.database());
                Progress progress = database.loadProgress(settings.playerName());
                AssetManager assets = new AssetManager();
                return new Boot(paths, store, settings, database, progress, assets);
            }

            @Override protected void done() {
                loadingTimer.stop();
                if (!frame.isDisplayable()) return;
                try {
                    Boot boot = get();
                    new Application(frame, boot.assets(), boot.paths(), boot.database(), boot.store(), boot.settings(), boot.progress());
                } catch (Exception error) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    cause.printStackTrace(System.err);
                    JOptionPane.showMessageDialog(frame,
                            "The carinderia could not be opened.\nCheck that the data folder is writable and launch the built JAR or run.cmd.\n\n" + cause.getMessage(),
                            "Five Nights at Gudo's — startup error", JOptionPane.ERROR_MESSAGE);
                    frame.dispose();
                }
            }
        }.execute();
    }

    private static Image icon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Theme.quality(g);
        g.setColor(Theme.BACKGROUND);
        g.fillRoundRect(0, 0, 64, 64, 12, 12);
        g.setColor(Theme.AMBER);
        g.drawRect(9, 9, 45, 45);
        Theme.text(g, "G", 16, 46, Theme.heading(42), Theme.AMBER);
        g.dispose();
        return image;
    }

    private record Boot(AppPaths paths, SettingsStore store, Settings settings, Database database,
                        Progress progress, AssetManager assets) { }
}
