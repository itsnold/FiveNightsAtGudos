package com.gudos;

import com.gudos.game.Outcome;
import com.gudos.persistence.Database;
import com.gudos.persistence.SettingsStore;
import com.gudos.ui.GamePanel;
import com.gudos.ui.MenuPanel;
import com.gudos.ui.PausePanel;
import com.gudos.ui.RecordsPanel;
import com.gudos.ui.SettingsPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/** Optional real-window/keyboard smoke check. Not part of the headless JUnit suite. */
public final class DesktopSmoke {
    private static JFrame frame;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gudo.dataDir") == null) throw new IllegalArgumentException("Set -Dgudo.dataDir to an isolated test directory");
        Path output = Path.of("target", "desktop-preview");
        Files.createDirectories(output);
        try {
            Main.main(new String[0]);
            robot = new Robot();
            robot.setAutoDelay(70);
            await(() -> {
                for (Frame candidate : Frame.getFrames()) {
                    if (candidate instanceof JFrame window && window.isShowing() && find(window, MenuPanel.class) != null) { frame = window; return true; }
                }
                return false;
            }, "initial menu");
            onEdt(() -> { frame.toFront(); frame.requestFocus(); return null; });
            capture(output, "menu");
            click("Extras");
            await(() -> button(frame, "Settings") != null, "extras menu");
            click("Settings");
            await(() -> find(frame, SettingsPanel.class) != null, "settings screen");
            onEdt(() -> {
                find(frame, JTextField.class).setText("GUI Smoke ñ");
                find(frame, JSlider.class).setValue(0);
                return null;
            });
            click("APPLY SETTINGS    →");
            await(() -> find(frame, MenuPanel.class) != null, "saved settings");
            Path data = Path.of(System.getProperty("gudo.dataDir"));
            if (!new SettingsStore(data.resolve("settings.txt")).load().playerName().equals("GUI Smoke ñ")) throw new AssertionError("Text settings were not saved");
            click("New Game");
            await(() -> find(frame, GamePanel.class) != null, "office screen");
            capture(output, "office");
            key(KeyEvent.VK_SPACE);
            await(() -> button(frame, "LOWER MONITOR    [SPACE]") != null, "Space opens monitor");
            key(KeyEvent.VK_6);
            await(() -> button(frame, "CAM\n06") != null && button(frame, "CAM\n06").isSelected(), "6 selects back alley");
            capture(output, "camera");
            key(KeyEvent.VK_SPACE);
            await(() -> button(frame, "OPEN MONITOR    [SPACE]") != null, "Space lowers monitor");
            robot.keyPress(KeyEvent.VK_F);
            await(() -> button(frame, "HOLD LIGHT  [F]").isSelected(), "held F activates light");
            robot.keyRelease(KeyEvent.VK_F);
            await(() -> !button(frame, "HOLD LIGHT  [F]").isSelected(), "released F deactivates light");
            key(KeyEvent.VK_ESCAPE);
            await(() -> find(frame, PausePanel.class) != null, "Escape pauses");
            capture(output, "pause");
            key(KeyEvent.VK_ESCAPE);
            await(() -> find(frame, GamePanel.class) != null, "Escape resumes");
            key(KeyEvent.VK_F11);
            await(() -> frame.isUndecorated(), "fullscreen on");
            key(KeyEvent.VK_F11);
            await(() -> !frame.isUndecorated(), "fullscreen off");
            await(() -> find(frame, GamePanel.class) != null, "fullscreen preserves active shift");
            key(KeyEvent.VK_ESCAPE);
            await(() -> find(frame, PausePanel.class) != null, "pause before leaving");
            click("RETURN TO MENU");
            await(() -> showingDialog() != null, "leave confirmation");
            onEdt(() -> { JButton yes = button(showingDialog(), "Yes"); if (yes == null) throw new AssertionError("Missing Yes confirmation"); yes.doClick(); return null; });
            await(() -> find(frame, MenuPanel.class) != null, "save abandoned attempt and return");
            var runs = new Database(data.resolve("gudo.db")).loadRuns("GUI Smoke ñ");
            if (runs.isEmpty() || runs.get(0).outcome() != Outcome.ABANDONED) throw new AssertionError("JDBC attempt not saved");
            if (!Files.readString(data.resolve("shift-log.txt")).contains("result=Left shift")) throw new AssertionError("Text report not appended");
            click("Extras");
            await(() -> button(frame, "Shift Records") != null, "extras after saving");
            click("Shift Records");
            await(() -> find(frame, RecordsPanel.class) != null, "SQLite records screen");
            capture(output, "records");
            click("← BACK");
            await(() -> find(frame, MenuPanel.class) != null, "back to menu");
            click("Quit");
            await(() -> !frame.isDisplayable(), "clean shutdown");
            System.out.println("Desktop smoke PASSED: real window, keyboard, fullscreen, UTF-8 settings, SQLite, shift log, records, clean exit.");
        } catch (Throwable error) {
            error.printStackTrace();
            if (frame != null && frame.isShowing()) capture(output, "failure");
            System.exit(1);
        }
    }

    private static void key(int code) { robot.keyPress(code); robot.keyRelease(code); }

    private static void click(String label) throws Exception {
        Point point = onEdt(() -> {
            JButton target = button(frame, label);
            if (target == null) throw new AssertionError("Missing button: " + label);
            Point location = target.getLocationOnScreen();
            return new Point(location.x + target.getWidth() / 2, location.y + target.getHeight() / 2);
        });
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void clickPrefix(String... prefixes) {
        SwingUtilities.invokeLater(() -> {
            for (String prefix : prefixes) {
                JButton target = buttonPrefix(frame, prefix);
                if (target != null) { target.doClick(); return; }
            }
            throw new AssertionError("Missing start button");
        });
    }

    private static void await(Callable<Boolean> condition, String description) throws Exception {
        long deadline = System.nanoTime() + 15_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (onEdt(condition)) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for " + description);
    }

    private static <T> T onEdt(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void capture(Path directory, String name) throws Exception {
        // Paint our Swing tree, not the desktop: another window may cover this game.
        BufferedImage image = onEdt(() -> {
            Container content = frame.getContentPane();
            BufferedImage rendered = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rendered.createGraphics();
            content.printAll(graphics);
            graphics.dispose();
            return rendered;
        });
        ImageIO.write(image, "png", directory.resolve(name + ".png").toFile());
    }

    private static JDialog showingDialog() {
        for (Window window : Window.getWindows()) if (window instanceof JDialog dialog && dialog.isShowing()) return dialog;
        return null;
    }

    private static JButton button(Container parent, String label) {
        if (parent == null) return null;
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton candidate && candidate.isShowing() && candidate.getText().equals(label)) return candidate;
            if (child instanceof Container container) { JButton found = button(container, label); if (found != null) return found; }
        }
        return null;
    }

    private static JButton buttonPrefix(Container parent, String prefix) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton candidate && candidate.isShowing() && candidate.getText().startsWith(prefix)) return candidate;
            if (child instanceof Container container) { JButton found = buttonPrefix(container, prefix); if (found != null) return found; }
        }
        return null;
    }

    private static <T> T find(Container parent, Class<T> type) {
        if (type.isInstance(parent)) return type.cast(parent);
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) { T found = find(container, type); if (found != null) return found; }
        }
        return null;
    }
}
