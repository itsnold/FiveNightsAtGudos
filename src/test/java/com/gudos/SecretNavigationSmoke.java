package com.gudos;

import com.gudos.assets.AssetManager;
import com.gudos.assets.PanoramaRenderer;
import com.gudos.game.CameraMotion;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
import com.gudos.persistence.*;
import com.gudos.ui.GamePanel;
import com.gudos.ui.SecretPanel;
import javax.swing.*;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/** Optional integration check with an undisplayed JFrame and isolated database; no Robot input. */
public final class SecretNavigationSmoke {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory(Path.of("target"), "secret-nav-");
        AppPaths paths = new AppPaths(dir);
        Database database = new Database(paths.database());
        Settings settings = new Settings("Navigation test", 0, false, false);
        database.loadProgress(settings.playerName());
        GameSession previousWin = new GameSession(1);
        previousWin.completeSecret();
        database.saveRun(settings.playerName(), previousWin.result());
        // Simulate locks left in an existing save by earlier versions of the game.
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + paths.database());
             var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE secret_night_completions (profile_id INTEGER NOT NULL, night INTEGER NOT NULL)");
            sql.execute("INSERT INTO secret_night_completions SELECT id, 1 FROM profiles");
        }
        var progress = database.loadProgress(settings.playerName());
        var app = new AtomicReference<Application>();
        Method close = Application.class.getDeclaredMethod("close");
        close.setAccessible(true);
        Field current = Application.class.getDeclaredField("current");
        current.setAccessible(true);
        Field sessionField = Application.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var assets = new AssetManager();
                    var instance = new Application(new JFrame(), assets, paths, database,
                            new SettingsStore(paths.settings()), settings, progress);
                    app.set(instance);
                    instance.startNight(1);
                    GamePanel gamePanel = (GamePanel) current.get(instance);
                    gamePanel.setSize(1280, 720);
                    GameSession shift = (GameSession) sessionField.get(instance);
                    shift.toggleMonitor();
                    shift.selectCamera(4);
                    var image = assets.scene(Scene.CAM_04, shift.sceneState(Scene.CAM_04));
                    var projection = new PanoramaRenderer();
                    projection.render(image, CameraMotion.pan(Scene.CAM_04, shift.elapsed()));
                    Point hit = null;
                    for (int y = 0; y < PanoramaRenderer.HEIGHT && hit == null; y++) {
                        for (int x = 0; x < PanoramaRenderer.WIDTH; x++) {
                            Point source = projection.sourcePoint(x, y);
                            if (source.x > image.getWidth() * .29 && source.x < image.getWidth() * .32
                                    && source.y > image.getHeight() * .27 && source.y < image.getHeight() * .37) {
                                hit = new Point((int) Math.ceil((x + .5) * 1280 / PanoramaRenderer.WIDTH),
                                        (int) Math.ceil((y + .5) * 720 / PanoramaRenderer.HEIGHT));
                                break;
                            }
                        }
                    }
                    if (hit == null) throw new AssertionError("Paper not visible at initial camera angle");
                    for (int i = 0; i < 5; i++) {
                        var event = new MouseEvent(gamePanel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                                hit.x, hit.y, 1, false, MouseEvent.BUTTON1);
                        for (var listener : gamePanel.getMouseListeners()) listener.mouseClicked(event);
                    }
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            long deadline = System.nanoTime() + 10_000_000_000L;
            AtomicReference<Object> panel = new AtomicReference<>();
            while (System.nanoTime() < deadline) {
                SwingUtilities.invokeAndWait(() -> {
                    try { panel.set(current.get(app.get())); } catch (Exception ex) { throw new RuntimeException(ex); }
                });
                if (panel.get() instanceof SecretPanel) {
                    SwingUtilities.invokeAndWait(() -> {
                        SecretPanel secret = (SecretPanel) panel.get();
                        if (!secret.isVisible() || secret.getParent() == null) {
                            throw new AssertionError("Minigame panel was created but not selected in CardLayout");
                        }
                    });
                    // A fast save must not briefly replace the minigame with a result panel.
                    verifySecretResult(app.get(), current, sessionField, true);
                    Method openAgain = Application.class.getDeclaredMethod("openSecret");
                    openAgain.setAccessible(true);
                    SwingUtilities.invokeAndWait(() -> {
                        try { openAgain.invoke(app.get()); } catch (Exception ex) { throw new RuntimeException(ex); }
                    });
                    verifySecretResult(app.get(), current, sessionField, false);
                    var runs = database.loadRuns(settings.playerName());
                    if (runs.stream().noneMatch(r -> r.outcome() == com.gudos.game.Outcome.MINIGAME_FAILED)
                            || runs.stream().filter(r -> r.outcome() == com.gudos.game.Outcome.MINIGAME_COMPLETED).count() != 2) {
                        throw new AssertionError("Secret outcomes were not saved as expected");
                    }
                    System.out.println("Secret navigation passed: " + panel.get().getClass().getSimpleName());
                    return;
                }
                Thread.sleep(50);
            }
            throw new AssertionError("Secret navigation stuck at " + panel.get());
        } finally {
            if (app.get() != null) SwingUtilities.invokeAndWait(() -> {
                try { close.invoke(app.get()); } catch (Exception ex) { throw new RuntimeException(ex); }
            });
        }
    }

    private static void verifySecretResult(Application app, Field current, Field sessionField, boolean won) throws Exception {
        Method finish = Application.class.getDeclaredMethod("finishSecret", boolean.class);
        finish.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (!(current.get(app) instanceof SecretPanel)) throw new AssertionError("Secret panel missing");
                finish.invoke(app, won);
                if (!(current.get(app) instanceof SecretPanel)) throw new AssertionError("Result screen flashed before saving");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
        long deadline = System.nanoTime() + 10_000_000_000L;
        AtomicReference<Boolean> restarted = new AtomicReference<>(false);
        while (System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    restarted.set(current.get(app) instanceof GamePanel && ((GameSession) sessionField.get(app)).night() == 1);
                } catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
            });
            if (restarted.get()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Same night did not restart after secret result");
    }
}
