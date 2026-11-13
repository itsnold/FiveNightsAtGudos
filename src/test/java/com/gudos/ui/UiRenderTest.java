package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
import com.gudos.game.minigame.SecretGame;
import com.gudos.game.minigame.SecretLevelBuilder;
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
                GameSession completed = new GameSession(1, 2);
                completed.update(2);
                ResultPanel result = new ResultPanel(assets, NAVIGATION, completed.result(), () -> { });
                result.saved();
                render(result, output.resolve("result.png"), 1280, 720);
                render(new RecordsPanel(assets, NAVIGATION, "Night Guard", progress, List.of(completed.result())), output.resolve("records.png"), 1280, 720);
                render(new RecordsPanel(assets, NAVIGATION, "Night Guard", Progress.EMPTY, List.of()), output.resolve("records-empty.png"), 1280, 720);
                var secret = new SecretLevelBuilder().player(80).bundle(240)
                        .destination(new Rectangle(1135, 510, 125, 130))
                        .platform(new Rectangle(420, 555, 100, 20))
                        .cover(new Rectangle(345, 500, 115, 140)).witness(530, 960).build();
                SecretPanel secretPanel = new SecretPanel(new SecretGame(secret), won -> { }, sound -> { }, () -> { }, () -> false);
                secretPanel.tick(0.5);
                render(secretPanel, output.resolve("secret.png"), 1280, 720);
                render(new SecretPanel(new SecretGame(secret), won -> { }, sound -> { }, () -> { }, () -> true), output.resolve("secret-reduced.png"), 960, 600);
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
            assertNull(monitor.getToolTipText());
            monitor.doClick();
            assertTrue(game.monitorOpen());
            JButton camera = findButton(view, "CAM\n06");
            assertNull(camera.getToolTipText());
            assertTrue(camera.isVisible());
            camera.doClick();
            assertEquals(6, game.camera());
            monitor.doClick();
            assertFalse(game.monitorOpen());
            assertFalse(camera.isVisible());
        });
    }

    @Test void secretSoundsOnlyPlayForSuccessfulActions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var level = new SecretLevelBuilder().player(100).bundle(100)
                    .destination(new Rectangle(1000, 500, 100, 140)).witness(800, 900).build();
            var sounds = new java.util.ArrayList<com.gudos.audio.Sound>();
            SecretPanel panel = new SecretPanel(new SecretGame(level), won -> { }, sounds::add, () -> { }, () -> false);
            var jump = panel.getActionMap().get("jump");
            var jumpUp = panel.getActionMap().get("jump-up");
            var action = panel.getActionMap().get("action");
            var actionUp = panel.getActionMap().get("action-up");
            jump.actionPerformed(null);
            jumpUp.actionPerformed(null);
            jump.actionPerformed(null); // Still airborne: no second sound.
            action.actionPerformed(null); // Pick up.
            actionUp.actionPerformed(null);
            action.actionPerformed(null); // Drop: no pickup sound.
            assertEquals(List.of(com.gudos.audio.Sound.SECRET_JUMP, com.gudos.audio.Sound.SECRET_PICKUP), sounds);
        });
    }

    @Test void secretFailureAndWinBothWaitForFrozenTransition() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var level = new SecretLevelBuilder().player(100).bundle(100)
                    .destination(new Rectangle(200, 500, 100, 140)).witness(900, 1100).timeLimit(0.05).build();
            var results = new java.util.ArrayList<Boolean>();
            SecretGame failed = new SecretGame(level);
            var silences = new java.util.concurrent.atomic.AtomicInteger();
            SecretPanel panel = new SecretPanel(failed, results::add, sound -> { }, silences::incrementAndGet, () -> false);
            failed.action();
            panel.tick(0.1);
            assertEquals(SecretGame.State.LOST, failed.state());
            assertEquals(1, silences.get());
            double position = failed.x();
            panel.setSize(320, 180);
            BufferedImage firstStatic = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
            var firstGraphics = firstStatic.createGraphics();
            panel.paint(firstGraphics);
            firstGraphics.dispose();
            assertTrue(new Color(firstStatic.getRGB(0, 12)).getRed() > 200, "line starts at left edge immediately");
            assertTrue(new Color(firstStatic.getRGB(319, 12)).getRed() > 200, "line reaches right edge immediately");
            panel.tick(0.2);
            BufferedImage secondStatic = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
            var secondGraphics = secondStatic.createGraphics();
            panel.paint(secondGraphics);
            secondGraphics.dispose();
            assertArrayEquals(firstStatic.getRGB(0, 0, 320, 180, null, 0, 320), secondStatic.getRGB(0, 0, 320, 180, null, 0, 320), "signal static must not animate");
            assertEquals(position, failed.x());
            assertTrue(results.isEmpty(), "Failure should remain on screen for three seconds");
            panel.tick(2.8);
            assertEquals(List.of(false), results);
            panel.tick(1);
            assertEquals(1, results.size(), "The completion callback must run only once");
            assertEquals(1, silences.get());

            SecretGame won = new SecretGame(new SecretLevelBuilder().player(200).bundle(200)
                    .destination(new Rectangle(200, 500, 100, 140)).witness(900, 1100).build());
            var wins = new java.util.ArrayList<Boolean>();
            var winSilences = new java.util.concurrent.atomic.AtomicInteger();
            SecretPanel winningPanel = new SecretPanel(won, wins::add, sound -> { }, winSilences::incrementAndGet, () -> false);
            var use = winningPanel.getActionMap().get("action");
            use.actionPerformed(null);
            winningPanel.getActionMap().get("action-up").actionPerformed(null);
            use.actionPerformed(null);
            assertEquals(1, winSilences.get(), "winning should silence audio immediately");
            winningPanel.tick(0);
            assertEquals(1, winSilences.get());
            assertTrue(wins.isEmpty());
            winningPanel.tick(2.9);
            assertTrue(wins.isEmpty());
            winningPanel.tick(0.2);
            assertEquals(List.of(true), wins);
            assertEquals(1, winSilences.get());
        });
    }

    @Test void encounterRendersOccupiedFeedsOfficeFadingAndJumpscare() throws Exception {
        Path output = Path.of("target", "ui-preview");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameSession game = new GameSession(1);
                GamePanel view = new GamePanel(game, assets, () -> Settings.DEFAULT, sound -> { }, () -> { });
                view.tick(3);
                game.toggleMonitor();
                for (int camera = 1; camera <= 6; camera++) {
                    game.setSceneState(Scene.camera(camera), "occupied");
                    game.selectCamera(camera);
                    view.tick(0.3);
                    render(view, output.resolve("occupied-camera-%02d.png".formatted(camera)), 1280, 720);
                    game.setSceneState(Scene.camera(camera), "default");
                }
                game.toggleMonitor();
                game.update(51);
                action(view, "left-a");
                view.tick(1);
                action(view, "left-a-off");
                render(view, output.resolve("gudo-office-left.png"), 1280, 720);
                game.setLight(true);
                game.update(0.8);
                render(view, output.resolve("gudo-half-faded.png"), 1280, 720);
                game.update(0.8);
                game.setLight(false);
                view.tick(0.1);
                render(view, output.resolve("gudo-repelled.png"), 1280, 720);
                for (String side : new String[]{"right", "front"}) {
                    for (int i = 0; i < 3000 && !game.gudo().atOffice(); i++) game.update(0.05);
                    assertTrue(game.gudo().atOffice());
                    if (side.equals("right")) {
                        action(view, "right-d");
                        view.tick(2);
                        action(view, "right-d-off");
                    } else {
                        // Ease toward the center from the far right.
                        action(view, "left-a");
                        view.tick(0.7);
                        action(view, "left-a-off");
                    }
                    render(view, output.resolve("gudo-office-" + side + ".png"), 1280, 720);
                    game.setOfficePan(game.gudo().approach().pan());
                    game.setLight(true);
                    game.update(game.gudo().flashSeconds());
                    game.setLight(false);
                }
                int[] callbacks = {0};
                JumpscarePanel scare = new JumpscarePanel(assets, false, () -> callbacks[0]++);
                scare.tick(0.28);
                render(scare, output.resolve("gudo-jumpscare.png"), 1280, 720);
                scare.tick(0.11);
                render(scare, output.resolve("gudo-jumpscare-shake.png"), 1280, 720);
                scare.tick(JumpscarePanel.DURATION);
                scare.tick(JumpscarePanel.DURATION);
                assertEquals(1, callbacks[0]);
                JumpscarePanel reduced = new JumpscarePanel(assets, true, () -> { });
                reduced.tick(0.3);
                render(reduced, output.resolve("gudo-jumpscare-reduced.png"), 960, 600);
                GameSession caught = new GameSession(1);
                caught.update(1000);
                ResultPanel result = new ResultPanel(assets, NAVIGATION, caught.result(), () -> { });
                result.saved();
                render(result, output.resolve("gudo-caught-result.png"), 1280, 720);
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        assertTrue(Files.size(output.resolve("gudo-half-faded.png")) > 100_000);
        assertFalse(java.util.Arrays.equals(Files.readAllBytes(output.resolve("gudo-jumpscare.png")),
                Files.readAllBytes(output.resolve("gudo-jumpscare-shake.png"))));
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
