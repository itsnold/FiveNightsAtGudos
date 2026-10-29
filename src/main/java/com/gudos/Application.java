package com.gudos;

import com.gudos.assets.AssetManager;
import com.gudos.audio.AudioService;
import com.gudos.audio.Sound;
import com.gudos.game.GameSession;
import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;
import com.gudos.persistence.*;
import com.gudos.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Coordinates screens on the EDT; all disk/JDBC work runs on a single IO worker. */
public final class Application implements Navigation {
    private final JFrame frame;
    private final JPanel cards = new JPanel(new CardLayout());
    private final AssetManager assets;
    private final AppPaths paths;
    private final Database database;
    private final SettingsStore settingsStore;
    private final ShiftLogStore shiftLog;
    private final AudioService audio;
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> new Thread(task, "gudo-storage"));
    private final Timer timer;
    private final KeyEventDispatcher keyDispatcher = this::dispatchKey;
    private Settings settings;
    private Progress progress;
    private ScenePanel current;
    private GameSession session;
    private GamePanel gamePanel;
    private ResultPanel resultPanel;
    private String activeProfile;
    private RunRecord pendingRun;
    private String pendingProfile;
    private boolean pendingSaved;
    private boolean busy;
    private boolean savingRun;
    private boolean closed;
    private boolean quitAfterSave;
    private boolean menuAfterSave;
    private boolean changingDisplay;
    private boolean displaySavePending;
    private boolean escapeDown;
    private boolean fullscreenDown;
    private Rectangle windowBounds;
    private long lastFrame = System.nanoTime();

    public Application(JFrame frame, AssetManager assets, AppPaths paths, Database database,
                       SettingsStore settingsStore, Settings settings, Progress progress) {
        this.frame = frame;
        this.assets = assets;
        this.paths = paths;
        this.database = database;
        this.settingsStore = settingsStore;
        this.settings = settings;
        this.progress = progress;
        this.shiftLog = new ShiftLogStore(paths.shiftLog());
        audio = new AudioService(settings.volume());
        frame.setContentPane(cards);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { quit(); }
        });
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent event) {
                if (!changingDisplay) {
                    escapeDown = fullscreenDown = false;
                    if (current == gamePanel) pauseShift();
                }
            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F11"), "fullscreen");
        frame.getRootPane().getActionMap().put("fullscreen", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { toggleFullscreen(); }
        });
        timer = new Timer(33, event -> tick());
        timer.setCoalesce(true);
        showMenu();
        if (settings.fullscreen()) applyDisplay(true);
        timer.start();
    }

    private boolean dispatchKey(KeyEvent event) {
        if (closed || current == null || !(event.getSource() instanceof Component component)) return false;
        Window origin = component instanceof Window window ? window : SwingUtilities.getWindowAncestor(component);
        if (origin != frame) return false; // Never steal keystrokes from confirmation dialogs.
        if (event.getKeyCode() == KeyEvent.VK_F11) {
            if (event.getID() == KeyEvent.KEY_RELEASED) fullscreenDown = false;
            else if (event.getID() == KeyEvent.KEY_PRESSED && !fullscreenDown) { fullscreenDown = true; toggleFullscreen(); }
            return true;
        }
        if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (event.getID() == KeyEvent.KEY_RELEASED) escapeDown = false;
            else if (event.getID() == KeyEvent.KEY_PRESSED) {
                if (escapeDown) return true;
                escapeDown = true;
            }
        }
        return current.dispatchKey(event);
    }

    private void tick() {
        long now = System.nanoTime();
        double seconds = Math.min(0.25, Math.max(0, (now - lastFrame) / 1_000_000_000.0));
        lastFrame = now;
        if (current == gamePanel && session != null && !session.finished()) {
            session.update(seconds);
            if (session.finished()) finishShift();
        }
        if (current != null) {
            current.tick(seconds);
            current.repaint();
        }
    }

    private void show(ScenePanel panel) {
        current = panel;
        cards.removeAll();
        cards.add(panel, "active");
        ((CardLayout) cards.getLayout()).show(cards, "active");
        cards.revalidate();
        cards.repaint();
        lastFrame = System.nanoTime();
        SwingUtilities.invokeLater(panel::requestFocusInWindow);
    }

    private void showMenu() {
        session = null;
        gamePanel = null;
        pendingRun = null;
        resultPanel = null;
        pendingSaved = false;
        show(new MenuPanel(assets, this, settings, progress));
        audio.menuMusic();
    }

    @Override public void menu() {
        if (busy || savingRun) return;
        if (session != null && !session.finished()) { leaveShift(); return; }
        if (pendingRun != null && !pendingSaved) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    "This attempt has not been fully saved. Return to the menu anyway?\nExisting database entries will be preserved.",
                    "Incomplete save", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
            busy = true;
            show(new LoadingPanel("Opening the shift register…"));
            submit(() -> database.loadProgress(settings.playerName()), loaded -> {
                busy = false;
                progress = loaded;
                showMenu();
            }, error -> {
                busy = false;
                showMenu();
                error("Could not refresh progress from SQLite. Existing data was not reset.", error);
            });
            return;
        }
        showMenu();
    }

    @Override public void page(Page page) {
        if (busy || savingRun || session != null && !session.finished()) return;
        switch (page) {
            case EXTRAS -> show(new ExtrasPanel(assets, this));
            case NIGHTS -> show(new NightSelectPanel(assets, this, progress));
            case GUIDE -> show(new InfoPanel(assets, this, false));
            case CREDITS -> show(new InfoPanel(assets, this, true));
            case SETTINGS -> show(new SettingsPanel(assets, this, settings, paths));
            case RECORDS -> {
                busy = true;
                show(new LoadingPanel("Opening the shift register…"));
                submit(() -> new Register(database.loadProgress(settings.playerName()), database.loadRuns(settings.playerName())), register -> {
                    busy = false;
                    progress = register.progress();
                    show(new RecordsPanel(assets, this, settings.playerName(), progress, register.runs()));
                }, error -> {
                    busy = false;
                    showMenu();
                    error("The shift register could not be read.", error);
                });
            }
        }
    }

    @Override public void startNight(int night) {
        if (busy || savingRun || session != null && !session.finished() || !progress.unlocked(night)) return;
        if (pendingRun != null && !pendingSaved) return;
        pendingRun = null;
        pendingSaved = false;
        resultPanel = null;
        activeProfile = settings.playerName();
        session = new GameSession(night);
        gamePanel = new GamePanel(session, assets, () -> settings, audio::play, this::pauseShift);
        show(gamePanel);
        audio.ambience(Sound.FAN);
    }

    private void pauseShift() {
        if (session == null || session.finished() || current != gamePanel) return;
        gamePanel.releaseControls();
        session.pause();
        show(new PausePanel(assets, this, session));
        audio.ambience(Sound.HUM);
    }

    @Override public void resumeShift() {
        if (session == null || session.finished() || busy) return;
        session.resume();
        show(gamePanel);
        audio.ambience(Sound.FAN);
    }

    @Override public void leaveShift() {
        if (session == null || session.finished() || busy || savingRun) return;
        pauseShift();
        int answer = JOptionPane.showConfirmDialog(frame,
                "Leave this shift?\nYour attempt will be recorded, but the next night will not be unlocked.",
                "Clock out early", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        menuAfterSave = true;
        session.abandon();
        finishShift();
    }

    private void finishShift() {
        gamePanel.releaseControls();
        pendingRun = session.result();
        pendingProfile = activeProfile;
        pendingSaved = false;
        audio.ambience(Sound.HUM);
        if (pendingRun.outcome() == Outcome.COMPLETED) audio.play(Sound.COMPLETE);
        else if (pendingRun.outcome() == Outcome.POWER_OUT) audio.play(Sound.POWER_DOWN);
        resultPanel = new ResultPanel(assets, this, pendingRun, this::savePendingRun);
        if (pendingRun.outcome() == Outcome.CAUGHT) {
            audio.ambience(null);
            audio.play(Sound.JUMPSCARE);
            show(new JumpscarePanel(assets, settings.reducedEffects(), () -> {
                show(resultPanel);
                audio.ambience(Sound.HUM);
            }));
        } else show(resultPanel);
        savePendingRun();
    }

    private void savePendingRun() {
        if (pendingRun == null || savingRun || pendingSaved) return;
        savingRun = true;
        resultPanel.saving();
        RunRecord run = pendingRun;
        String profile = pendingProfile;
        submit(() -> {
            Progress loaded = database.saveRun(profile, run);
            shiftLog.append(profile, run);
            return loaded;
        }, loaded -> {
            savingRun = false;
            pendingSaved = true;
            progress = loaded;
            resultPanel.saved();
            if (quitAfterSave) close();
            else if (menuAfterSave) { menuAfterSave = false; showMenu(); }
        }, error -> {
            savingRun = false;
            quitAfterSave = menuAfterSave = false;
            resultPanel.failed();
            error("Could not fully save this attempt. Retry from the result screen.\nRetries do not create duplicate records.", error);
        });
    }

    @Override public void saveSettings(Settings updated) {
        if (busy || savingRun || displaySavePending || !(current instanceof SettingsPanel panel)) return;
        busy = true;
        panel.setSaving(true);
        submit(() -> {
            Progress loaded = database.loadProgress(updated.playerName());
            settingsStore.save(updated);
            return loaded;
        }, loaded -> {
            busy = false;
            boolean displayChanged = settings.fullscreen() != updated.fullscreen();
            settings = updated;
            progress = loaded;
            audio.setVolume(updated.volume());
            if (displayChanged) applyDisplay(updated.fullscreen());
            showMenu();
        }, error -> {
            busy = false;
            panel.setSaving(false);
            error("Preferences could not be saved to " + paths.settings(), error);
        });
    }

    private void toggleFullscreen() {
        if (busy || savingRun || displaySavePending || closed) return;
        displaySavePending = true;
        Settings updated = settings.withFullscreen(!settings.fullscreen());
        submit(() -> { settingsStore.save(updated); return updated; }, saved -> {
            displaySavePending = false;
            settings = saved;
            applyDisplay(saved.fullscreen());
            if (current instanceof SettingsPanel panel) panel.setFullscreen(saved.fullscreen());
        }, error -> {
            displaySavePending = false;
            error("Fullscreen preference could not be saved.", error);
        });
    }

    private void applyDisplay(boolean fullscreen) {
        if (frame.isUndecorated() == fullscreen) return;
        changingDisplay = true;
        if (fullscreen) windowBounds = frame.getBounds();
        frame.dispose();
        frame.setUndecorated(fullscreen);
        frame.setExtendedState(Frame.NORMAL);
        if (fullscreen) frame.setBounds(frame.getGraphicsConfiguration().getBounds());
        else if (windowBounds != null) frame.setBounds(windowBounds);
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> { changingDisplay = false; if (current != null) current.requestFocusInWindow(); });
    }

    @Override public void quit() {
        if (closed) return;
        if (busy || savingRun || displaySavePending) {
            JOptionPane.showMessageDialog(frame, "Please wait for the current save or load to finish.", "One moment", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (session != null && !session.finished()) {
            pauseShift();
            int answer = JOptionPane.showConfirmDialog(frame, "Clock out and exit?\nThis attempt will be saved before the game closes.",
                    "End your shift", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            quitAfterSave = true;
            session.abandon();
            finishShift();
            return;
        }
        if (pendingRun != null && !pendingSaved) {
            int answer = JOptionPane.showConfirmDialog(frame, "This attempt has not been fully saved. Exit anyway?", "Incomplete save", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        close();
    }

    private void close() {
        closed = true;
        timer.stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
        audio.close();
        io.shutdown();
        frame.dispose();
    }

    private <T> void submit(Callable<T> work, Consumer<T> success, Consumer<Exception> failure) {
        io.submit(() -> {
            try {
                T value = work.call();
                SwingUtilities.invokeLater(() -> { if (!closed) success.accept(value); });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> { if (!closed) failure.accept(error); });
            }
        });
    }

    private void error(String message, Exception error) {
        error.printStackTrace(System.err);
        audio.play(Sound.ERROR);
        JOptionPane.showMessageDialog(frame, message + "\n\n" + error.getMessage(), "Gudo's — storage error", JOptionPane.ERROR_MESSAGE);
    }

    private record Register(Progress progress, List<RunRecord> runs) { }
}
