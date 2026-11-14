package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.audio.Sound;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
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
import java.util.ArrayList;

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

    @Test void soundsFollowAcceptedInputsAndViewingGudoDoesNotPlayASightingSting() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSession game = new GameSession(1);
            List<Sound> cues = new ArrayList<>();
            GamePanel view = new GamePanel(game, assets, () -> Settings.DEFAULT, cues::add, () -> { });
            action(view, "light-on");
            action(view, "light-on");
            action(view, "light-off");
            assertEquals(List.of(Sound.FLASHLIGHT_ON, Sound.FLASHLIGHT_OFF), cues);
            action(view, "monitor-space");
            cues.clear();
            action(view, "light-on");
            action(view, "light-off");
            action(view, "camera-1");
            assertTrue(cues.isEmpty(), "Blocked light and reselecting the current feed must be silent");
            game.update(18);
            view.tick(0.1);
            assertEquals(List.of(Sound.GUDO_STEP), cues);
            cues.clear();
            action(view, "camera-4");
            view.tick(0.1);
            assertEquals(List.of(Sound.CAMERA_SWITCH), cues);
            assertNull(view.cameraGarbleSound());
            for (int repeat = 0; repeat < 3; repeat++) {
                cues.clear();
                action(view, "camera-3");
                view.tick(0.1);
                action(view, "camera-4");
                view.tick(0.1);
                assertEquals(List.of(Sound.CAMERA_SWITCH, Sound.CAMERA_SWITCH), cues,
                        "Merely looking at Gudo or returning to his feed must not play a scare or garble cue");
                assertNull(view.cameraGarbleSound());
            }
            game.pause();
            cues.clear();
            view.tick(60);
            assertTrue(cues.isEmpty(), "Paused shifts must not schedule encounter or room cues");
        });
    }

    @Test void watchedDepartureDisruptsOnlyThatFeedForTwoActiveSeconds() throws Exception {
        Path output = Path.of("target", "ui-preview");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameSession game = new GameSession(1);
                Settings[] preferences = {Settings.DEFAULT};
                GamePanel view = new GamePanel(game, assets, () -> preferences[0], cue -> { }, () -> { });
                view.tick(3); // Finish the panel's night-intro overlay before inspecting the actual feed.
                game.toggleMonitor();
                game.selectCamera(4);
                game.update(18);
                view.tick(0.1);
                assertNotNull(view.cameraGarbleSound(), "A watched arrival also triggers garble");
                game.update(11);
                view.tick(0.1);
                assertEquals(Scene.CAM_03, game.gudo().location());
                Sound garble = view.cameraGarbleSound();
                assertNotNull(garble);
                assertTrue(List.of(Sound.GARBLE_1, Sound.GARBLE_2, Sound.GARBLE_3).contains(garble));
                render(view, output.resolve("camera-garbled.png"), 1280, 720);
                assertNotEquals(Color.BLACK.getRGB(),
                        ImageIO.read(output.resolve("camera-garbled.png").toFile()).getRGB(400, 400));
                preferences[0] = new Settings("Night Guard", 55, true, false);
                Path reduced = output.resolve("camera-garbled-reduced.png");
                render(view, reduced, 1280, 720);
                assertEquals(new Color(15, 17, 18).getRGB(), ImageIO.read(reduced.toFile()).getRGB(400, 400),
                        "Reduced effects must still conceal the room during a lost feed");

                game.update(0.75);
                view.tick(0.75);
                game.selectCamera(3);
                assertNull(view.cameraGarbleSound(), "The destination/other feeds remain clear");
                game.selectCamera(4);
                assertSame(garble, view.cameraGarbleSound(), "Returning must not start another variant/timer");
                game.toggleMonitor();
                assertNull(view.cameraGarbleSound(), "The office must not play garble audio");
                game.toggleMonitor();
                assertSame(garble, view.cameraGarbleSound());
                game.pause();
                game.update(60);
                view.tick(60);
                assertNull(view.cameraGarbleSound(), "Paused audio is silent");
                game.resume();
                assertSame(garble, view.cameraGarbleSound(), "Pause must preserve the remaining duration");
                game.update(1.249);
                view.tick(1.249);
                assertSame(garble, view.cameraGarbleSound(), "Still disrupted just before two seconds");
                game.update(0.002);
                view.tick(0.002);
                assertNull(view.cameraGarbleSound(), "Clear after two seconds, without camera switching extending it");
            } catch (Exception error) { throw new RuntimeException(error); }
        });
    }

    @Test void watchedArrivalsGarbleButUnwatchedMovesStayClearAndEveryMoveHasDeepSteps() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Monitor down, watching an unrelated feed, and selecting each destination before the move.
            for (int mode = 0; mode < 3; mode++) {
                GameSession game = new GameSession(1);
                List<Sound> cues = new ArrayList<>();
                GamePanel view = new GamePanel(game, assets, () -> Settings.DEFAULT, cues::add, () -> { });
                if (mode != 0) { game.toggleMonitor(); game.selectCamera(6); }
                int[] destinations = {4, 3, 2, 1};
                for (int step = 0; step < 4; step++) {
                    if (mode == 2) game.selectCamera(destinations[step]);
                    game.update(step == 0 ? 18 : 11);
                    view.tick(0.1);
                    if (mode == 2 && step < 3) {
                        assertNotNull(view.cameraGarbleSound(), "Watching him enter a camera must garble it");
                    } else {
                        assertNull(view.cameraGarbleSound(), "Unwatched move in mode " + mode);
                    }
                    assertEquals(step + 1, cues.stream().filter(cue -> cue == Sound.GUDO_STEP).count());
                }
                assertTrue(game.gudo().atOffice());
            }
        });
    }

    @Test void arrivalUrgencyAndRetreatSoundsPlayOncePerEncounter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSession game = new GameSession(1);
            List<Sound> cues = new ArrayList<>();
            GamePanel view = new GamePanel(game, assets, () -> Settings.DEFAULT, cues::add, () -> { });
            game.update(51);
            view.tick(0.1);
            view.tick(0.1);
            assertEquals(1, cues.stream().filter(cue -> cue == Sound.GUDO_WARNING).count());
            game.update(6.1);
            view.tick(0.1);
            view.tick(0.1);
            assertEquals(1, cues.stream().filter(cue -> cue == Sound.GUDO_RUSH).count());
            game.setOfficePan(game.gudo().approach().pan());
            game.setLight(true);
            game.update(game.gudo().flashSeconds());
            view.tick(0.1);
            view.tick(0.1);
            assertEquals(1, cues.stream().filter(cue -> cue == Sound.GUDO_RETREAT).count());
            assertFalse(game.finished());
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
