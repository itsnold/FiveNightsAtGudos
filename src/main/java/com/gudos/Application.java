package com.gudos;

import com.gudos.assets.AssetManager;
import com.gudos.audio.AudioService;
import com.gudos.audio.Sound;
import com.gudos.game.GameSession;
import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;
import com.gudos.game.minigame.SecretGame;
import com.gudos.game.minigame.SecretLevel;
import com.gudos.game.minigame.SecretLevelBuilder;
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

/** Coordinates screens on the EDT; all disk/JDBC work runs on a single IO worker. Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17). */
/*
 * Application
 *
 * Purpose: Coordinates screens, active attempts, audio and asynchronous storage; implements the
 * Navigation facade contract.
 *
 * Execution flow: Panels call Navigation; Application checks guards, creates or reuses the required
 * model/view, changes the visible card, requests audio, and sends storage work to one IO worker.
 *
 * Design rationale: One coordinator keeps panels from opening database connections
 * or managing the window. busy/savingRun and pendingRun protect saves from conflicting navigation.
 * This is a practical application coordinator with a facade role, not a claim that every method is a
 * GoF pattern.
 *
 * State: current is the visible ScenePanel; session is the active simulation; gamePanel
 * and secretPanel are views. settings/progress are current loaded values. pendingRun/pendingProfile
 * preserve a result across retries. io owns disk work; timer owns EDT ticks. nextNightAfterSecret
 * actually stores the same night to restart.
 *
 * Summary: New Game calls navigation.startNight(1). Application.startNight checks
 * eligibility, resets pending state, captures the profile, creates GameSession and GamePanel, wires
 * callbacks, shows the panel and requests gameplay audio. That one request hides the startup
 * subsystem.
 * Reviewer (42-page edition): [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
 */
public final class Application implements Navigation {
    // One window hosts the current screen. Panels request navigation through the Navigation interface. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    private final JFrame frame;
    private final JPanel cards = new JPanel(new CardLayout());
    private final AssetManager assets;
    private final AppPaths paths;
    private final Database database;
    private final SettingsStore settingsStore;
    private final ShiftLogStore shiftLog;
    private final AudioService audio;
    // Serialize saves/loads on one worker so disk access does not freeze Swing input or painting. | Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> new Thread(task, "gudo-storage"));
    private final Timer timer;
    private final KeyEventDispatcher keyDispatcher = this::dispatchKey;
    private Settings settings;
    private Progress progress;
    private ScenePanel current;
    private GameSession session;
    private GamePanel gamePanel;
    private SecretPanel secretPanel;
    // Retained until the corresponding run and its text report have both been saved. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    private int nextNightAfterSecret;
    private ResultPanel resultPanel;
    // Capture the player when the shift starts so its result stays attached to that profile. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    private String activeProfile;
    // Retain the same result and ID after a failed save so the user can retry safely. | Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    private RunRecord pendingRun;
    private String pendingProfile;
    private boolean pendingSaved;
    private boolean busy;
    private boolean savingRun;
    private boolean closed;
    // Defer navigation/exit until both the database and text report have been saved. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    private boolean quitAfterSave;
    private boolean menuAfterSave;
    private boolean changingDisplay;
    private boolean displaySavePending;
    private boolean escapeDown;
    private boolean fullscreenDown;
    private Rectangle windowBounds;
    private long lastFrame = System.nanoTime();

    // Dependencies loaded by Main are supplied here instead of being reloaded by every screen. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Receive the dependencies loaded by Main, create the shift log/audio service and current-card
     * window, install orderly close/focus/global-key handling, create a coalesced 33 ms Swing Timer,
     * show the menu, apply saved display mode and start ticking. Storage remains on the serial IO
     * executor.
     * Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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
        // Route the window's X button through quit(), which can save an abandoned attempt first. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            /*
             * Route the window X button through quit, so an active attempt can be recorded rather
             * than silently discarded.
             * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
             */
            @Override public void windowClosing(WindowEvent event) { quit(); }
        });
        // Losing focus pauses gameplay; changing fullscreen also changes focus temporarily. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        frame.addWindowFocusListener(new WindowAdapter() {
            /*
             * Unless fullscreen is currently rebuilding the native window, clear global held-key
             * flags and pause the active normal or secret game. This prevents inactive-window
             * gameplay from continuing.
             * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
             */
            @Override public void windowLostFocus(WindowEvent event) {
                if (!changingDisplay) {
                    escapeDown = fullscreenDown = false;
                    if (current == gamePanel) pauseShift();
                    else if (current == secretPanel && secretPanel != null) secretPanel.pause();
                }
            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F11"), "fullscreen");
        frame.getRootPane().getActionMap().put("fullscreen", new AbstractAction() {
            /*
             * Request the coordinator fullscreen operation from the root-pane F11 action; the same
             * method performs persistence and window handling.
             * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
             */
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { toggleFullscreen(); }
        });
        // About 30 UI updates per second; actual elapsed time is measured inside tick(). | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        timer = new Timer(33, event -> tick());
        timer.setCoalesce(true);
        showMenu();
        if (settings.fullscreen()) applyDisplay(true);
        timer.start();
    }

    // Handle global keys first, then let the active panel resolve its own key bindings. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    // Held-key flags prevent repeated Escape/F11 presses from toggling screens continuously. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Ignore events from other windows/dialogs or a closed application. Debounce held F11 and
     * Escape; route global fullscreen first, then let the visible ScenePanel dispatch its own
     * bindings. This prevents Swing traversal/default-button handling from swallowing gameplay keys.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private boolean dispatchKey(KeyEvent event) {
        if (closed || current == null || !(event.getSource() instanceof Component component)) return false;
        Window origin = component instanceof Window window ? window : SwingUtilities.getWindowAncestor(component);
        if (origin != frame) return false; // Never steal keystrokes from confirmation dialogs. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
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

    // Main loop: advance gameplay, handle a finished shift, then update and repaint the screen. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Measure real elapsed time with nanoTime, clamp one frame to 0.25 seconds, and update the
     * normal model only when its GamePanel is visible. Handle a finished attempt before
     * updating/repainting the current view. Finally refresh scene audio for an ongoing normal shift.
     * The clamp intentionally discards excess stalled wall time.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void tick() {
        long now = System.nanoTime();
        // Cap a delayed frame at 0.25 seconds to avoid a large jump after a UI stall. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
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
        if (current == gamePanel && session != null && !session.finished()) updateGameAudio();
    }

    // Replace the visible panel and reset the frame clock before the new screen receives input. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Replace the active card with the requested ScenePanel, revalidate/repaint, reset lastFrame and
     * request focus later on the EDT. Resetting the clock avoids charging the new screen for time
     * spent on the old one.
     * Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    // Clear the previous shift's UI state; saved database progress is kept separately. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Stop transient effects and clear session, panel and pending-result references, then create the
     * title screen and request menu music. Saved Progress is retained; this does not delete database
     * history.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void showMenu() {
        audio.stopEffects();
        nextNightAfterSecret = 0;
        session = null;
        gamePanel = null;
        secretPanel = null;
        pendingRun = null;
        resultPanel = null;
        pendingSaved = false;
        show(new MenuPanel(assets, this, settings, progress));
        audio.menuMusic();
    }

    // A return to the menu must resolve an active shift or acknowledge an incomplete save. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Reject navigation during an IO/save operation. An active session goes through leaveShift. An
     * incompletely saved result requires an explicit warning and a background progress refresh
     * before the title menu; ordinary menu navigation uses showMenu.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    // Build the requested Extras screen. Records need an asynchronous database read first. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Reject conflicting requests and active-shift navigation. Switch on the Page enum to build a
     * screen; RECORDS instead shows loading, reads progress plus latest attempts on the IO worker,
     * then constructs RecordsPanel on the EDT.
     * Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    // Create a fresh simulation; Continue also uses this path rather than restoring a checkpoint. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Facade operation used by New Game, Continue, selection and replay: reject
     * busy/saving/active/locked/incomplete-save cases; clear old result state; capture playerName;
     * create a fresh GameSession; create GamePanel with settings/audio/pause callbacks; attach the
     * secret callback; show it and request gameplay audio. No checkpoint is restored and no old
     * records are deleted.
     * Reviewer (42-page edition): [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    @Override public void startNight(int night) {
        if (busy || savingRun || session != null && !session.finished() || !progress.unlocked(night)) return;
        if (pendingRun != null && !pendingSaved) return;
        pendingRun = null;
        pendingSaved = false;
        resultPanel = null;
        nextNightAfterSecret = 0;
        activeProfile = settings.playerName();
        session = new GameSession(night);
        // Supply current settings and callbacks, so GamePanel need not own storage or audio services. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        gamePanel = new GamePanel(session, assets, () -> settings, sound -> {
            float pan = switch (sound) {
                case GUDO_STEP, GUDO_WARNING, GUDO_POUND, GUDO_WHISPER,
                        GUDO_RUSH -> (float) (session.gudo().approach().pan() * 2 - 1);
                default -> 0;
            };
            audio.play(sound, pan);
        }, this::pauseShift);
        gamePanel.onSecret(this::openSecret);
        show(gamePanel);
        audio.stopEffects();
        updateGameAudio();
    }

    /*
     * Translate session state into audio-layer requests: monitor hiss, office breathing, tension at
     * power<=25 or shift progress>=0.65, approach pan and any watched-feed garble. AudioService owns
     * loop/clip operations.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void updateGameAudio() {
        audio.gameplay(session.monitorOpen(), session.gudo().atOffice(),
                session.power() <= 25 || session.progress() >= 0.65,
                (float) (session.gudo().approach().pan() * 2 - 1), gamePanel.cameraGarbleSound());
    }

    // Opening does not wait for storage; previous secret attempts never block another play. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Allow Nights 1-4 only while the normal GamePanel is active. Release controls, pause the
     * original session and stop audio; build SecretGame/SecretPanel and show it. If construction
     * fails, resume the existing night and restore gameplay audio. Previous wins do not block
     * another launch.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void openSecret() {
        // Night 5 is deliberately excluded. Replaying on earlier nights is always allowed. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        if (busy || session == null || session.night() >= 5 || current != gamePanel) return;
        // Freeze the existing shift before switching screens: its clock, power and encounter stop. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        gamePanel.releaseControls();
        session.pause();
        audio.stopAll();
        try {
            secretPanel = new SecretPanel(new SecretGame(createSecretLevel()), this::finishSecret, audio::play, audio::stopAll, () -> settings.reducedEffects());
            show(secretPanel);
            audio.ambience(Sound.SECRET_AMBIENCE);
        } catch (RuntimeException error) {
            session.resume();
            show(gamePanel);
            updateGameAudio();
            error("The minigame could not be opened. Your shift is still running.", error);
        }
    }

    // Builder pattern client: specify the level step by step and finish with build(). Keeping this | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    // separate from navigation allows the exact production level to be created without a window. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Use the Builder to create the exact production level: width 2560, player x80, bundle x240,
     * destination x2410/y510/125x130, three platforms, six cover rectangles, three patrols, and 60
     * seconds. This package-visible factory constructs the level without opening Application.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    static SecretLevel createSecretLevel() {
        return new SecretLevelBuilder().width(2560).player(80).bundle(240)
                    .destination(new Rectangle(2410, 510, 125, 130))
                    .platform(new Rectangle(420, 555, 100, 20))
                    .platform(new Rectangle(1120, 555, 100, 20))
                    .platform(new Rectangle(1820, 555, 100, 20))
                    .cover(new Rectangle(345, 500, 165, 140))
                    .cover(new Rectangle(700, 500, 165, 140))
                    .cover(new Rectangle(1050, 500, 165, 140))
                    .cover(new Rectangle(1400, 500, 165, 140))
                    .cover(new Rectangle(1750, 500, 165, 140))
                    .cover(new Rectangle(2100, 500, 165, 140))
                    .witness(530, 850).witness(1120, 1500).witness(1830, 2200).timeLimit(60).build();
    }

    // SecretPanel calls this after the frozen/glitch success or failure sequence. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    // Finish this exact attempt; automatic navigation waits for the normal, retryable save. | Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Accept the callback only for the currently visible secret and an unfinished session. Retain
     * the original night number, finish it as MINIGAME_COMPLETED or MINIGAME_FAILED, then use the
     * normal finish/save pipeline. Neither secret result completes a normal night.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void finishSecret(boolean won) {
        if (session == null || session.finished() || current != secretPanel) return;
        // Despite the field name, this is the same night: a secret does not complete/unlock a shift. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        nextNightAfterSecret = session.night();
        if (won) session.completeSecret(); else session.failSecret();
        finishShift();
    }

    // Release held controls before pausing so the flashlight cannot remain stuck on after resume. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Only pause an active visible normal GamePanel. Clear held inputs, pause the model, replace the
     * screen with PausePanel and switch ambience. Clearing held F prevents a stuck flashlight on
     * resume.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void pauseShift() {
        if (session == null || session.finished() || current != gamePanel) return;
        gamePanel.releaseControls();
        session.pause();
        show(new PausePanel(assets, this, session));
        audio.stopEffects();
        audio.ambience(Sound.HUM);
    }

    // Reuse the same session and game panel, preserving the paused shift's in-memory state. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Resume the same unfinished session if no conflicting load is running. Show its existing
     * GamePanel and restore gameplay audio. A new GameSession is not constructed here.
     * Reviewer (42-page edition): [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    @Override public void resumeShift() {
        if (session == null || session.finished() || busy) return;
        session.resume();
        show(gamePanel);
        updateGameAudio();
    }

    // Confirm an early departure, mark it ABANDONED, and save before returning to the menu. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Pause the normal shift, confirm early departure, set menuAfterSave, finish ABANDONED and run
     * finishShift. A canceled confirmation leaves the session paused so the player can resume.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    // Translate the simulation's final result into audio, a result screen, and a save request. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Release controls, capture the immutable result and the profile recorded at start, and
     * construct ResultPanel. Choose result sound/transition; CAUGHT shows JumpscarePanel while
     * saving begins. Secret outcomes retain their frozen minigame screen until saving completes.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void finishShift() {
        gamePanel.releaseControls();
        pendingRun = session.result();
        pendingProfile = activeProfile;
        pendingSaved = false;
        audio.stopEffects();
        if (nextNightAfterSecret == 0) audio.ambience(Sound.HUM);
        if (pendingRun.outcome() == Outcome.COMPLETED) audio.play(Sound.COMPLETE);
        else if (pendingRun.outcome() == Outcome.POWER_OUT) audio.play(Sound.POWER_DOWN);
        resultPanel = new ResultPanel(assets, this, pendingRun, this::savePendingRun);
        if (pendingRun.outcome() == Outcome.CAUGHT) {
            audio.ambience(null);
            // Facade: one request hides clip loading, volume adjustment, and playback details. | Reviewer (42-page edition): [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
            audio.play(Sound.JUMPSCARE);
            show(new JumpscarePanel(assets, settings.reducedEffects(), () -> {
                audio.stopEffects();
                show(resultPanel);
                audio.ambience(Sound.HUM);
            }));
        } else if (nextNightAfterSecret == 0) show(resultPanel);
        // For secret outcomes, keep the frozen minigame visible until the save finishes. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        // For CAUGHT, saving begins while the jumpscare is still playing. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
        savePendingRun();
    }

    // A save succeeds only when both storage operations finish; retry keeps the same run ID. | Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Ignore absent/already-saved/in-flight results. Capture the same pending UUID/profile, run
     * Database.saveRun followed by ShiftLogStore.append on IO, then enable result navigation. Honor
     * queued quit/menu or restart the same night after secret outcomes. On failure keep the result
     * and expose Retry Save; SQLite may already have committed.
     * Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void savePendingRun() {
        if (pendingRun == null || savingRun || pendingSaved) return;
        savingRun = true;
        resultPanel.saving();
        RunRecord run = pendingRun;
        String profile = pendingProfile;
        submit(() -> {
            // These are separate writes: SQLite may commit even if the text append fails afterward. | Reviewer (42-page edition): [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
            // Database and ShiftLogStore both recognize a repeated run ID on retry. | Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
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
            else if (nextNightAfterSecret != 0) {
                // Wait for both SQLite and the text report before creating the fresh attempt. | Reviewer (42-page edition): [p. 11: save files and schema](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-11); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
                int next = nextNightAfterSecret;
                nextNightAfterSecret = 0;
                secretPanel = null;
                startNight(next);
            }
        }, error -> {
            savingRun = false;
            quitAfterSave = menuAfterSave = false;
            resultPanel.failed();
            // Secret results are normally hidden; show the retry controls if saving failed. | Reviewer (42-page edition): [p. 12: save and retry](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-12); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
            if (nextNightAfterSecret != 0) show(resultPanel);
            error("Could not fully save this attempt. Retry from the result screen.\nRetries do not create duplicate records.", error);
        });
    }

    // Load/select the named profile and persist preferences before applying them to the UI. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Only accept Apply from the visible SettingsPanel and while storage/display work is idle. Load
     * or create the requested profile and save text preferences in the worker; on success replace
     * settings/progress, set volume, apply any display change and show the title menu. On failure
     * re-enable Apply and report the error.
     * Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    // F11 persists the new preference before changing the actual window mode. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Guard concurrent changes, create a replacement Settings with the flipped fullscreen value,
     * persist it on IO, and only then change the native window. Synchronize a visible SettingsPanel
     * checkbox after success.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    // Swing requires disposing the native window before changing its border decorations. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    // Keep its old bounds so leaving fullscreen restores the window's previous size/position. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Skip if already in the requested mode. Remember windowed bounds, dispose the native window,
     * change undecorated state, apply fullscreen or restored bounds, and show it again.
     * changingDisplay suppresses the temporary focus-loss pause caused by this operation.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    // Normal exit waits for storage; an active shift is saved as ABANDONED after confirmation. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Wait if a save/load/display preference is pending. For an active normal or secret attempt,
     * pause, confirm, mark ABANDONED and save before closing. An incompletely saved result has a
     * separate exit warning. Otherwise close immediately.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    @Override public void quit() {
        if (closed) return;
        if (busy || savingRun || displaySavePending) {
            JOptionPane.showMessageDialog(frame, "Please wait for the current save or load to finish.", "One moment", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (session != null && !session.finished()) {
            pauseShift();
            if (current == secretPanel && secretPanel != null) secretPanel.pause();
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

    // Stop timers/listeners and release audio/worker resources before disposing the window. | Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Mark closed, stop the Swing timer, unregister the global key dispatcher, queue audio cleanup,
     * shut down the storage executor and dispose the frame. Background completion callbacks check
     * closed before touching UI.
     * Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void close() {
        closed = true;
        timer.stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
        audio.close();
        io.shutdown();
        frame.dispose();
    }

    // Run blocking work on the storage thread, then deliver success/failure on Swing's EDT. | Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    // T lets the same helper return settings, progress, or a combined records result. | Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Generic asynchronous bridge: execute Callable<T> on the one IO worker, then schedule success
     * or failure Consumer callbacks on Swing EDT. Disk/JDBC work belongs inside work; screen
     * mutation belongs inside the delivered callback.
     * Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
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

    /*
     * Print the exception for diagnosis, request the ERROR cue, and show a storage-error dialog
     * containing the human message plus exception message.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private void error(String message, Exception error) {
        error.printStackTrace(System.err);
        audio.play(Sound.ERROR);
        JOptionPane.showMessageDialog(frame, message + "\n\n" + error.getMessage(), "Gudo's — storage error", JOptionPane.ERROR_MESSAGE);
    }

    // One worker result contains both the summary counters and the records-table rows. | Reviewer (42-page edition): [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
    /*
     * Register
     *
     * One IO result combining profile Progress and the latest RunRecord list for the records screen.
     * Reviewer (42-page edition): [p. 17: Application](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-17).
     */
    private record Register(Progress progress, List<RunRecord> runs) { }
}
