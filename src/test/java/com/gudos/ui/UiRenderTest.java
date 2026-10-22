package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.GameSession;
import com.gudos.persistence.AppPaths;
import com.gudos.persistence.Progress;
import com.gudos.persistence.Settings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Headless, real Swing/Java2D renders; previews are build artifacts, not game assets. */
class UiRenderTest {
    private static AssetManager assets;
    private static final Navigation NAVIGATION = new Navigation() {
        @Override public void menu() { }
        @Override public void page(Page page) { }
        @Override public void startNight(int night) { }
        @Override public void saveSettings(Settings settings) { }
        @Override public void resumeShift() { }
        @Override public void leaveShift() { }
        @Override public void quit() { }
    };

    @BeforeAll static void load() throws Exception { assets = new AssetManager(); }

    @Test void allScreensRenderAtDesktopAndSmallWindowSizes() throws Exception {
        Path output = Path.of("target", "ui-preview");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Settings settings = Settings.DEFAULT;
                Progress progress = new Progress(3, 2, 4, 2);
                render(new MenuPanel(assets, NAVIGATION, settings, progress), output.resolve("menu.png"), 1280, 720);
                render(new MenuPanel(assets, NAVIGATION, settings, progress), output.resolve("menu-small.png"), 960, 600);
                render(new ExtrasPanel(assets, NAVIGATION), output.resolve("extras.png"), 1280, 720);
                GameSession game = new GameSession(1);
                GamePanel view = new GamePanel(game, assets, () -> settings, sound -> { }, () -> { });
                view.tick(3);
                render(view, output.resolve("office.png"), 1280, 720);
                game.toggleMonitor();
                for (int camera = 1; camera <= 6; camera++) {
                    game.selectCamera(camera);
                    view.tick(1);
                    render(view, output.resolve("camera-%02d.png".formatted(camera)), 1280, 720);
                }
                render(new SettingsPanel(assets, NAVIGATION, settings, new AppPaths(Path.of("target", "test-data"))), output.resolve("settings.png"), 1280, 720);
                render(new InfoPanel(assets, NAVIGATION, false), output.resolve("guide.png"), 1280, 720);
                render(new InfoPanel(assets, NAVIGATION, true), output.resolve("credits.png"), 1280, 720);
                render(new NightSelectPanel(assets, NAVIGATION, progress), output.resolve("nights.png"), 1280, 720);
                game.pause();
                render(new PausePanel(assets, NAVIGATION, game), output.resolve("pause.png"), 1280, 720);
                GameSession completed = new GameSession(1);
                completed.update(360);
                ResultPanel result = new ResultPanel(assets, NAVIGATION, completed.result(), () -> { });
                result.saved();
                render(result, output.resolve("result.png"), 1280, 720);
                render(new RecordsPanel(assets, NAVIGATION, "Night Guard", progress, List.of(completed.result())), output.resolve("records.png"), 1280, 720);
                render(new RecordsPanel(assets, NAVIGATION, "Night Guard", Progress.EMPTY, List.of()), output.resolve("records-empty.png"), 1280, 720);
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        assertTrue(Files.size(output.resolve("office.png")) > 100_000);
        assertTrue(Files.size(output.resolve("menu.png")) > 100_000);
    }

    @Test void actualSwingButtonsControlTheMonitorAndCameras() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSession game = new GameSession(1);
            GamePanel view = new GamePanel(game, assets, () -> Settings.DEFAULT, sound -> { }, () -> { });
            JButton monitor = findButton(view, "OPEN MONITOR    [SPACE]");
            assertNotNull(monitor);
            monitor.doClick();
            assertTrue(game.monitorOpen());
            JButton camera = findButton(view, "CAM\n06");
            assertTrue(camera.isVisible());
            camera.doClick();
            assertEquals(6, game.camera());
            monitor.doClick();
            assertFalse(game.monitorOpen());
            assertFalse(camera.isVisible());
        });
    }

    @Test void keyboardActionsDebounceMonitorAndControlLightAndOfficeYaw() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSession game = new GameSession(1);
            GamePanel view = new GamePanel(game, assets, () -> Settings.DEFAULT, sound -> { }, () -> { });
            action(view, "monitor-space");
            action(view, "monitor-space");
            assertTrue(game.monitorOpen(), "Key repeat must not repeatedly flip the monitor");
            action(view, "camera-6");
            assertEquals(6, game.camera());
            action(view, "monitor-space-release");
            action(view, "monitor-space");
            assertFalse(game.monitorOpen());
            action(view, "light-on");
            assertTrue(game.lightOn());
            action(view, "light-off");
            assertFalse(game.lightOn());
            action(view, "left-a");
            view.tick(1);
            assertTrue(view.officePan() < 0.1);
            action(view, "left-a-off");
            action(view, "right-d");
            view.tick(2);
            assertTrue(view.officePan() > 0.9);
            view.releaseControls();
        });
    }

    private static void action(ScenePanel view, String name) {
        Action action = view.getActionMap().get(name);
        assertNotNull(action, "Missing keyboard action: " + name);
        action.actionPerformed(new java.awt.event.ActionEvent(view, 0, name));
    }

    private static JButton findButton(Container panel, String label) {
        for (Component component : panel.getComponents()) if (component instanceof JButton button && button.getText().equals(label)) return button;
        return null;
    }

    private static void render(ScenePanel panel, Path file, int width, int height) throws Exception {
        panel.setSize(width, height);
        layout(panel);
        for (Component child : panel.getComponents()) {
            assertTrue(child.getX() >= 0 && child.getY() >= 0);
            assertTrue(child.getX() + child.getWidth() <= width);
            assertTrue(child.getY() + child.getHeight() <= height);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container);
    }
}
