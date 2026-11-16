package com.gudos.ui;

import com.gudos.audio.Sound;
import com.gudos.game.minigame.SecretGame;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Swing view/controller for SecretGame. It translates input into model actions and draws
 * the model; Application owns the window, audio service, shift records and navigation.
 * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
 * Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks);
 * [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules);
 * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (geometry and patrol configuration).
 */
/*
 * SecretPanel
 *
 * Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks);
 * [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules);
 * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (geometry and patrol configuration);
 * [Application.java:464 (finishSecret)](../Application.java#L464) (handles the delayed outcome callback).
 *
 * Purpose: Controls and renders the scrolling pixel-art minigame.
 *
 * Execution flow: GamePanel paper trigger -> Application.openSecret -> SecretPanel; key events call
 * SecretGame; tick advances it; outcome freezes, silences and reports after three seconds.
 *
 * Design rationale: Use an offscreen 640x360 image enlarged nearest-neighbor. World
 * scrolling is isolated from fixed HUD. reported and outcomeSilenced prevent duplicate
 * callbacks/audio.
 *
 * State: left/right/jump/action are input flags; paused stops model and transition
 * time; pixels is the retro canvas; transitionTime reaches 3.0 before reporting; animation/walk
 * drive visuals.
 *
 * Summary: This panel pauses its own model by skipping update. The original night session is
 * separately paused by Application. Both success and failure save a secret outcome, then restart the
 * same normal night.
 * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
 */
public final class SecretPanel extends ScenePanel {
    private final SecretGame game;
    private final Consumer<Boolean> finished;
    private final Consumer<Sound> sound;
    private final Runnable silence;
    private final BooleanSupplier reducedEffects;
    // The off-screen image is enlarged using nearest-neighbor interpolation for crisp pixel art. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
    private final BufferedImage pixels = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
    // Held-key flags prevent auto-repeat from causing repeated jumps/actions while a key is down. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
    private boolean left, right, jump, action, paused, reported, outcomeSilenced, facingRight = true;
    private double animation, walk, transitionTime;
    private static final double TRANSITION_SECONDS = 3.0;

    /*
     * Retain model/outcome/audio/reduced-effect callbacks and bind A/D/arrows, Space, E and Escape.
     * Debounce jump/action so held keys do not auto-repeat accepted actions. View construction does
     * not start the pickup timer.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [Application.java:416 (SecretPanel call)](../Application.java#L416) (caller of SecretPanel);
     * [game/minigame/SecretGame.java:302 (state)](../game/minigame/SecretGame.java#L302) (used by SecretPanel);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (geometry and patrol configuration).
     */
    public SecretPanel(SecretGame game, Consumer<Boolean> finished, Consumer<Sound> sound, Runnable silence, BooleanSupplier reducedEffects) {
        this.game = game; this.finished = finished; this.sound = sound; this.silence = silence; this.reducedEffects = reducedEffects;
        bind("pressed A", "left", () -> left = true); bind("released A", "left-up", () -> left = false);
        bind("pressed LEFT", "arrow-left", () -> left = true); bind("released LEFT", "arrow-left-up", () -> left = false);
        bind("pressed D", "right", () -> right = true); bind("released D", "right-up", () -> right = false);
        bind("pressed RIGHT", "arrow-right", () -> right = true); bind("released RIGHT", "arrow-right-up", () -> right = false);
        // Play the jump sound only when the grounded model actually accepts a jump. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:416 (SecretPanel call)](../Application.java#L416) (caller of SecretPanel); [game/minigame/SecretGame.java:302 (state)](../game/minigame/SecretGame.java#L302) (used by SecretPanel).
        bind("pressed SPACE", "jump", () -> {
            if (!jump && !paused && game.state() == SecretGame.State.PLAYING && game.grounded()) { game.jump(); sound.accept(Sound.SECRET_JUMP); }
            jump = true;
        });
        bind("released SPACE", "jump-up", () -> jump = false);
        bind("pressed E", "action", () -> {
            if (!action && !paused) {
                // Comparing before/after distinguishes a pickup from a drop, delivery or missed press. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:416 (SecretPanel call)](../Application.java#L416) (caller of SecretPanel); [game/minigame/SecretGame.java:302 (state)](../game/minigame/SecretGame.java#L302) (used by SecretPanel).
                boolean wasCarrying = game.carrying();
                game.action();
                if (!wasCarrying && game.carrying()) sound.accept(Sound.SECRET_PICKUP);
                silenceOnOutcome();
            }
            action = true;
        });
        bind("released E", "action-up", () -> action = false);
        bind("pressed ESCAPE", "pause", () -> { paused = !paused; release(); });
    }

    // Also called on window focus loss; clearing held keys prevents movement after focus returns. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:110 (Application)](../Application.java#L110) (focus-loss pause callback); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (simulation held while paused).
    /*
     * Set paused and clear all held input flags. Application calls this on focus loss; Escape can
     * later unpause this panel.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [Application.java:110 (Application)](../Application.java#L110) (focus-loss pause callback);
     * [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (simulation held while paused);
     * [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (held-key bindings).
     */
    public void pause() { paused = true; release(); }
    /*
     * Clear movement/jump/action flags so old physical input cannot continue after a pause or focus
     * change.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [Application.java:110 (Application)](../Application.java#L110) (focus-loss pause callback);
     * [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (simulation held while paused);
     * [ui/ScenePanel.java:250 (bind)](ScenePanel.java#L250) (held-key bindings).
     */
    private void release() { left = right = jump = action = false; }

    /*
     * When the model first leaves PLAYING, set outcomeSilenced and run the audio-stop callback
     * immediately. Delivery can end inside an E callback; capture/timeout can end during tick.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer simulation);
     * [Application.java:464 (finishSecret)](../Application.java#L464) (delayed outcome callback);
     * [audio/AudioService.java:191 (stopAll)](../audio/AudioService.java#L191) (supplied silence callback).
     */
    private void silenceOnOutcome() {
        // Run once for both timeout/capture (detected during tick) and delivery (detected on E). | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer simulation); [Application.java:464 (finishSecret)](../Application.java#L464) (delayed outcome callback).
        if (!outcomeSilenced && game.state() != SecretGame.State.PLAYING) {
            outcomeSilenced = true;
            silence.run();
        }
    }

    /*
     * Skip model updates while paused or ended. During play update movement and visual walk
     * direction; on ending silence once, count three unpaused transition seconds, set reported and
     * send the won boolean once. Application handles persistence/restarting.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer simulation);
     * [Application.java:464 (finishSecret)](../Application.java#L464) (delayed outcome callback);
     * [audio/AudioService.java:191 (stopAll)](../audio/AudioService.java#L191) (supplied silence callback).
     */
    @Override public void tick(double seconds) {
        super.tick(seconds);
        if (!paused && game.state() == SecretGame.State.PLAYING) {
            double before = game.x();
            game.update(seconds, left, right);
            animation += seconds;
            // Animate walking only when Gudo actually moves; keep the last facing direction when idle. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer simulation); [Application.java:464 (finishSecret)](../Application.java#L464) (delayed outcome callback).
            if (game.x() != before) { facingRight = game.x() > before; walk += seconds; }
        }
        silenceOnOutcome();
        // Freeze the final frame with the disruption lines for three seconds; then restart. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer simulation); [Application.java:464 (finishSecret)](../Application.java#L464) (delayed outcome callback).
        if (!paused && game.state() != SecretGame.State.PLAYING) transitionTime += seconds;
        // Report only once so Application saves one shift attempt, even if more frames are painted. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer simulation); [Application.java:464 (finishSecret)](../Application.java#L464) (delayed outcome callback).
        if (!reported && game.state() != SecretGame.State.PLAYING && transitionTime >= TRANSITION_SECONDS) {
            reported = true;
            finished.accept(game.state() == SecretGame.State.WON);
        }
    }

    /*
     * Draw into the 640x360 pixel buffer with logical scaling, translate only a copied world context
     * to follow the player across the 2560-wide level, draw fixed HUD/signal lines, then enlarge
     * nearest-neighbor to the screen. This is 2D world scrolling.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:194 (level)](../game/minigame/SecretGame.java#L194) (used by render);
     * [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (geometry and patrol configuration).
     */
    @Override protected void render(Graphics2D screen) {
        Graphics2D g = pixels.createGraphics();
        // A 640x360 pixel canvas retains crisp retro scaling while allowing facial/hair details. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:194 (level)](../game/minigame/SecretGame.java#L194) (used by render); [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.scale(.5, .5);
        // The camera follows Gudo across the wider level; HUD and static stay fixed on screen. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:194 (level)](../game/minigame/SecretGame.java#L194) (used by render); [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks).
        int camera = (int) Math.max(0, Math.min(game.level().width() - WIDTH, game.x() - WIDTH / 2.0));
        // Use a separate graphics context so translating the world cannot move the HUD/white lines. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:194 (level)](../game/minigame/SecretGame.java#L194) (used by render); [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks).
        Graphics2D world = (Graphics2D) g.create();
        world.translate(-camera, 0);
        drawRoom(world);
        drawWorld(world);
        world.dispose();
        drawHud(g);
        if (game.state() != SecretGame.State.PLAYING) drawSignalLines(g);
        g.dispose();
        screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        screen.drawImage(pixels, 0, 0, WIDTH, HEIGHT, null);
    }

    /** Fixed white tracking lines over the frozen scene; the positions never change per frame. [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules). */
    /*
     * Draw fixed horizontal white bands over the frozen final minigame scene. Their positions do not
     * animate; reducedEffects lowers opacity. This is distinct from normal animated VHS noise.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks);
     * [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (geometry and patrol configuration).
     */
    private void drawSignalLines(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, reducedEffects.getAsBoolean() ? 150 : 230));
        int[] rows = {48, 112, 196, 276, 344, 424, 504, 596, 672};
        for (int y : rows) g.fillRect(0, y, WIDTH, 12);
    }

    /*
     * Draw background tiles, floor, stockroom doorway and platform tops in world coordinates. The
     * destination/platform shapes come from the model level configuration.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:194 (level)](../game/minigame/SecretGame.java#L194) (used by drawRoom);
     * [ui/Theme.java:131 (text)](Theme.java#L131) (used by drawRoom);
     * [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks).
     */
    private void drawRoom(Graphics2D g) {
        int width = game.level().width();
        g.setColor(new Color(10, 10, 19)); g.fillRect(0, 0, width, HEIGHT);
        g.setColor(new Color(43, 46, 39)); g.fillRect(0, 155, width, 485);
        // Faded green walls and worn lower tiles echo the game's dining room and kitchen. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:194 (level)](../game/minigame/SecretGame.java#L194) (used by drawRoom); [ui/Theme.java:131 (text)](Theme.java#L131) (used by drawRoom).
        for (int row = 0; row < 6; row++) for (int col = -1; col < width / 128 + 2; col++) {
            int x = col * 128 + (row % 2) * 64, y = 160 + row * 80;
            g.setColor(row < 3 ? new Color(64, 68, 55) : new Color(49, 58, 49)); g.fillRect(x + 4, y + 4, 120, 72);
            g.setColor(new Color(83, 82, 65)); g.fillRect(x + 8, y + 8, 112, 4);
        }
        g.setColor(new Color(20, 20, 29)); g.fillRect(0, 185, width, 12); g.fillRect(0, 212, width, 8);
        for (int x = 546; x < width; x += 800) {
            g.setColor(new Color(123, 109, 86)); g.fillRect(x, 205, 190, 12);
            g.setColor(new Color(198, 183, 132)); g.fillRect(x + 20, 217, 150, 8);
        }
        g.setColor(new Color(48, 46, 39)); g.fillRect(0, 640, width, 80);
        for (int x = 0; x < width; x += 64) { g.setColor(new Color(85, 79, 65)); g.fillRect(x, 640, 60, 8); g.fillRect(x + 24, 682, 4, 38); }
        drawCarinderiaDetails(g);
        // A small clue instead of an explicit explanation of the story. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:194 (level)](../game/minigame/SecretGame.java#L194) (used by drawRoom); [ui/Theme.java:131 (text)](Theme.java#L131) (used by drawRoom).
        g.setColor(new Color(163, 151, 118)); g.fillRect(112, 255, 68, 92);
        g.setColor(new Color(49, 39, 41)); g.fillRect(124, 270, 44, 5); g.fillRect(136, 282, 20, 24); g.fillRect(124, 314, 44, 4); g.fillRect(128, 326, 36, 4);
        g.setColor(new Color(18, 15, 24)); g.fill(game.level().destination());
        g.setColor(new Color(110, 82, 65)); g.draw(game.level().destination());
        Rectangle door = game.level().destination();
        g.setColor(new Color(172, 146, 99)); g.fillRect(door.x + 15, door.y + 23, 70, 8);
        Theme.text(g, "STOCKROOM", door.x - 32, door.y - 5, Theme.pixel(25), new Color(207, 185, 143));
        for (Rectangle p : game.level().platforms()) {
            g.setColor(new Color(85, 65, 62)); g.fill(p);
            g.setColor(new Color(158, 120, 87)); g.fillRect(p.x, p.y, p.width, 5);
        }
    }

    /** Background-only landmarks inspired by the dining room, serving counter and kitchen feeds. [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules). */
    /*
     * Draw visual-only dining/counter/kitchen landmarks across the side-view level. These shapes do
     * not create physics obstacles; only level platforms/cover/destination have the corresponding
     * model rules.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks);
     * [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (geometry and patrol configuration).
     */
    private void drawCarinderiaDetails(Graphics2D g) {
        // The side-view connects the existing dining room, counter, kitchen and stockroom feeds. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        // Partitions mark the transitions without blocking the player's one-dimensional path. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        for (int x : new int[]{800, 1600, 2280}) {
            g.setColor(new Color(31, 35, 30)); g.fillRect(x, 220, 32, 360);
            g.setColor(new Color(100, 102, 80)); g.fillRect(x + 4, 225, 8, 330);
        }
        // Dining area: green tablecloth, plastic chairs, menu board and outside window. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        g.setColor(new Color(41, 39, 33)); g.fillRect(180, 230, 260, 90);
        g.setColor(new Color(157, 130, 76)); g.fillRect(188, 238, 244, 74);
        for (int x = 204; x < 420; x += 48) { g.setColor(new Color(98, 70, 49)); g.fillOval(x, 250, 28, 28); g.setColor(new Color(217, 190, 130)); g.fillRect(x + 4, 286, 20, 4); }
        g.setColor(new Color(38, 44, 36)); g.fillRect(70, 490, 210, 16);
        g.setColor(new Color(88, 116, 75)); g.fillRect(78, 478, 194, 20);
        g.setColor(new Color(143, 158, 112)); for (int x = 86; x < 260; x += 32) g.fillRect(x, 482, 8, 8);
        g.setColor(new Color(126, 122, 103)); g.fillRect(82, 506, 12, 74); g.fillRect(246, 506, 12, 74);
        g.setColor(new Color(23, 26, 27)); g.fillRect(505, 250, 190, 165);
        g.setColor(new Color(66, 71, 60)); g.fillRect(512, 257, 176, 151);
        g.setColor(new Color(20, 28, 31)); g.fillRect(520, 265, 160, 135);
        g.setColor(new Color(151, 117, 70)); g.fillRect(606, 290, 4, 20); g.fillRect(570, 340, 12, 4);
        g.setColor(new Color(69, 75, 61)); g.fillRect(594, 265, 8, 135); g.fillRect(520, 325, 160, 8);
        // Plastic chairs from the dining camera sit around the cloth-covered table. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        g.setColor(new Color(140, 145, 122)); g.fillRect(40, 505, 28, 8); g.fillRect(40, 505, 8, 60); g.fillRect(40, 545, 40, 8);
        g.fillRect(270, 505, 28, 8); g.fillRect(290, 505, 8, 60); g.fillRect(258, 545, 40, 8);
        // The glass-front serving counter has lit metal trays, like the counter camera. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        g.setColor(new Color(36, 35, 29)); g.fillRect(850, 348, 320, 182);
        g.setColor(new Color(125, 110, 81)); g.fillRect(858, 356, 304, 8); g.fillRect(858, 430, 304, 8);
        g.setColor(new Color(66, 80, 69)); g.fillRect(864, 364, 292, 60);
        for (int x = 874; x < 1150; x += 60) {
            g.setColor(new Color(163, 155, 130)); g.fillRect(x, 398, 50, 16);
            g.setColor(new Color(151, 106, 55)); g.fillRect(x + 4, 398, 42, 8);
        }
        g.setColor(new Color(124, 118, 98)); g.fillRect(850, 530, 320, 22);
        // Counter CCTV monitor and condiment bottles. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        g.setColor(new Color(22, 24, 24)); g.fillRect(1250, 390, 94, 70);
        g.setColor(new Color(79, 107, 95)); g.fillRect(1258, 398, 78, 52);
        g.setColor(new Color(151, 162, 137)); g.fillRect(1292, 405, 4, 38); g.fillRect(1264, 424, 64, 4);
        g.setColor(new Color(108, 53, 43)); g.fillRect(1370, 440, 12, 28);
        g.setColor(new Color(166, 137, 80)); g.fillRect(1388, 440, 12, 28);
        // Kitchen hood, pots and prep counter lead into the stockroom. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        g.setColor(new Color(37, 39, 35)); g.fillRect(1640, 270, 360, 80);
        g.setColor(new Color(116, 115, 99)); g.fillRect(1652, 338, 336, 12);
        for (int x = 1680; x < 1960; x += 60) {
            g.setColor(new Color(142, 139, 116)); g.fillRect(x, 352, 4, 34); g.fillRect(x - 8, 384, 24, 28);
            g.setColor(new Color(60, 64, 55)); g.fillRect(x - 4, 388, 16, 20);
        }
        g.setColor(new Color(91, 95, 84)); g.fillRect(1650, 480, 330, 22);
        g.setColor(new Color(46, 48, 42)); g.fillRect(1660, 502, 310, 64);
        // Stacked rice sacks and bottles at the stockroom end of the route. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [Application.java:408 (openSecret)](../Application.java#L408) (creates model/view and supplies callbacks); [game/minigame/SecretGame.java:147 (update)](../game/minigame/SecretGame.java#L147) (platformer rules).
        g.setColor(new Color(137, 132, 110));
        for (int row = 0; row < 3; row++) g.fillRect(2310 + row * 8, 462 + row * 28, 100, 24);
        g.setColor(new Color(76, 88, 75));
        for (int x = 2140; x < 2240; x += 20) { g.fillRect(x + 4, 405, 8, 8); g.fillRect(x, 413, 16, 28); }
    }

    /*
     * Draw model beam polygons, Gudo, the carried/dropped bundle, cover shelves and witnesses in
     * that depth order. The view uses the same model polygons as collision; foreground cover visibly
     * explains why a bundle can be hidden.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection);
     * [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (world geometry).
     */
    private void drawWorld(Graphics2D g) {
        // Drawing order provides depth: beams, Gudo/bundle, foreground cover, then the witnesses. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        // Every light uses the exact polygon that the simulation checks for detection. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        for (int i = 0; i < game.witnessCount(); i++) {
            g.setColor(new Color(218, 196, 120, 75)); g.fill(game.witnessBeam(i));
        }
        person(g, (int) game.x(), (int) game.y(), facingRight, (int) (walk * 7) % 2, true, !game.grounded());
        int bx = game.carrying() ? (int) game.x() + 20 : (int) game.bundleX();
        int by = game.carrying() ? (int) game.y() + 42 : (int) game.bundleY();
        bundle(g, bx, by);
        // Shelves are in the foreground: Gudo and the bundle visibly pass behind them. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        // The tinted openings leave them visible enough to navigate while communicating cover. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        int coverIndex = 0;
        for (Rectangle c : game.level().cover()) {
            g.setColor(new Color(32, 34, 28, 150)); g.fill(c);
            // The three appearances share the same rectangular hiding rule in SecretGame. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
            int kind = coverIndex++ % 3;
            g.setColor(kind == 0 ? new Color(107, 98, 75) : kind == 1 ? new Color(121, 121, 104) : new Color(112, 82, 58));
            g.fillRect(c.x, c.y, c.width, 8);
            g.fillRect(c.x, c.y, 8, c.height); g.fillRect(c.x + c.width - 8, c.y, 8, c.height);
            g.fillRect(c.x, c.y + 45, c.width, 8); g.fillRect(c.x, c.y + c.height - 14, c.width, 14);
            if (kind == 0) { // A stocked metal rack. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
                g.setColor(new Color(157, 146, 110));
                for (int x = c.x + 16; x < c.x + c.width - 24; x += 32) g.fillRect(x, c.y + 14, 20, 28);
            } else if (kind == 1) { // A kitchen prep cart with pots and utensils. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
                g.setColor(new Color(183, 179, 148));
                g.fillRect(c.x + 20, c.y + 24, 40, 18); g.fillRect(c.x + 28, c.y + 16, 24, 8);
                g.fillRect(c.x + 94, c.y + 16, 24, 26);
            } else { // Wooden delivery crates behind the counter. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
                g.setColor(new Color(151, 112, 75));
                g.fillRect(c.x + 12, c.y + 10, 56, 32); g.fillRect(c.x + 82, c.y + 10, 56, 32);
                g.setColor(new Color(75, 54, 45));
                g.drawLine(c.x + 12, c.y + 10, c.x + 68, c.y + 42);
                g.drawLine(c.x + 82, c.y + 42, c.x + 138, c.y + 10);
            }
        }
        // Witnesses patrol in front of the cover while Gudo can stand behind it. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        for (int i = 0; i < game.witnessCount(); i++) {
            person(g, (int) game.witnessX(i), 570, game.witnessRight(i), (int) (animation * 5 + i) % 2, false, false);
        }
    }

    /*
     * Draw one pixel-art figure using a local transformed graphics copy. Mirror for left-facing
     * direction, alternate limbs for walking and use an airborne pose for jumps. These are drawn
     * shapes, not an external sprite-sheet animation.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection);
     * [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (world geometry).
     */
    private void person(Graphics2D g, int x, int y, boolean right, int step, boolean gudo, boolean airborne) {
        Graphics2D s = (Graphics2D) g.create();
        // Mirror the same pixel art when the character faces left instead of maintaining two sprites. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        s.translate(x + (right ? 0 : 40), y);
        if (!right) s.scale(-1, 1);
        // The legs and arms alternate independently; an airborne pose clearly reads as a jump. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        Color dark = new Color(20, 18, 27), skin = new Color(193, 147, 112);
        Color clothing = gudo ? new Color(57, 111, 62) : new Color(71, 112, 112);
        s.setColor(dark); s.fillRect(4, 0, 32, 28); s.fillRect(8, 28, 24, 24);
        s.setColor(skin); s.fillRect(8, 8, 24, 20);
        // Reference: short, tapered sides and a textured, mostly flat top, not a rounded cap. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        s.setColor(gudo ? new Color(26, 22, 23) : new Color(40, 37, 38));
        if (gudo) {
            // Short tapered sides and a fuller, textured top inspired by the supplied photo. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
            s.fillRect(4, 1, 30, 7); s.fillRect(7, -3, 26, 5);
            s.fillRect(10, -6, 19, 4); s.fillRect(14, -8, 5, 3);
            s.fillRect(25, -7, 5, 4); s.fillRect(32, 2, 3, 6);
            s.setColor(new Color(54, 45, 42));
            s.fillRect(4, 8, 4, 5); s.fillRect(6, 13, 3, 4); s.fillRect(29, 8, 3, 3);
            s.setColor(new Color(78, 65, 58));
            s.fillRect(10, -2, 3, 2); s.fillRect(17, -4, 4, 2); s.fillRect(26, -2, 4, 2);
        }
        else s.fillRect(8, 4, 24, 8);
        s.setColor(dark); s.fillRect(16, 16, 4, 4); s.fillRect(26, 16, 4, 4);
        if (gudo) { s.setColor(new Color(211, 203, 186)); s.fillRect(19, 24, 10, 3); }
        s.setColor(clothing); s.fillRect(8, 28, 24, 24); s.fillRect(step == 0 ? 4 : 8, 32, 6, 18);
        // Dark shirt under an open green jacket, with a narrow zipper/lapel on each side. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        s.setColor(gudo ? new Color(29, 40, 32) : new Color(109, 144, 143)); s.fillRect(12, 32, 16, 16);
        if (gudo) { s.setColor(new Color(105, 151, 88)); s.fillRect(8, 32, 4, 16); s.fillRect(28, 32, 4, 16); }
        s.setColor(dark);
        int stride = airborne ? 4 : (step == 0 ? 3 : -3);
        s.fillRect(9 + stride, 52, 8, 13); s.fillRect(23 - stride, 52, 8, 13);
        s.setColor(new Color(5, 5, 9)); s.fillRect(9 + stride, 64, 9, 6); s.fillRect(23 - stride, 64, 9, 6);
        s.dispose();
    }

    /*
     * Draw the shrouded bundle around the existing model anchor. The larger visual silhouette does
     * not change center-based cover/detection or pickup rules.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection);
     * [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](../game/minigame/SecretLevel.java#L31) (world geometry).
     */
    private void bundle(Graphics2D g, int x, int y) {
        // The 72-unit horizontal silhouette is about as long as a standing person's 70-unit | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        // height. Keep the model anchor unchanged so drawing does not change cover detection. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        x -= 24; y -= 8;
        // A continuous shroud surrounds the head, shoulders and tapered legs. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        g.setColor(new Color(31, 27, 29));
        g.fillRect(x, y + 8, 16, 20); g.fillRect(x + 16, y + 8, 44, 24);
        g.fillRect(x + 24, y + 4, 24, 28); g.fillRect(x + 60, y + 12, 12, 16);
        g.setColor(new Color(180, 173, 155));
        g.fillRect(x + 4, y + 8, 12, 16); g.fillRect(x + 16, y + 12, 44, 16);
        g.fillRect(x + 24, y + 8, 24, 20); g.fillRect(x + 60, y + 16, 8, 8);
        // Broad diagonal folds and a rope bind the cloth rather than looking like separate blocks. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:341 (witnessBeam)](../game/minigame/SecretGame.java#L341) (same polygon used by detection); [game/minigame/SecretGame.java:321 (bundleHidden)](../game/minigame/SecretGame.java#L321) (bundle cover test).
        g.setColor(new Color(225, 220, 202));
        for (int fold : new int[]{4, 20, 36, 52}) {
            g.fillRect(x + fold, y + 12, 8, 4);
            g.fillRect(x + fold + 4, y + 16, 8, 4);
            g.fillRect(x + fold + 8, y + 20, 4, 4);
        }
        g.setColor(new Color(100, 83, 72));
        g.fillRect(x + 20, y + 12, 4, 16); g.fillRect(x + 40, y + 8, 4, 20);
        g.fillRect(x + 60, y + 16, 4, 8);
    }

    /*
     * Draw instructions and countdown (TIME -- before first pickup), optional retro noise and the
     * paused overlay. Only the model decrements remaining; the view ceil is display rounding.
     * [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     * Related: [game/minigame/SecretGame.java:276 (remaining)](../game/minigame/SecretGame.java#L276) (countdown value);
     * [game/minigame/SecretGame.java:293 (carrying)](../game/minigame/SecretGame.java#L293) (bundle action hint);
     * [ui/Theme.java:131 (text)](Theme.java#L131) (HUD typography).
     */
    private void drawHud(Graphics2D g) {
        g.setColor(new Color(7, 7, 12)); g.fillRect(0, 0, WIDTH, 145);
        Color white = new Color(220, 209, 174), red = new Color(204, 86, 76);
        Theme.text(g, "AFTER HOURS", 36, 50, Theme.pixel(40), white);
        Theme.text(g, game.started() ? "TIME " + (int) Math.ceil(game.remaining()) : "TIME --", 1030, 50, Theme.pixel(36), red);
        Theme.text(g, "A/D MOVE   SPACE JUMP   E PICK UP / DROP   ESC PAUSE", 36, 92, Theme.pixel(26), white);
        Theme.text(g, "HIDE THE BUNDLE BEHIND COVER; E AT STOCKROOM TO DELIVER", 36, 125, Theme.pixel(24), new Color(198, 190, 165));
        if (!reducedEffects.getAsBoolean()) {
            // A reproducible frame-seeded noise field and rolling band evoke an unstable CRT. | [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). | Related: [game/minigame/SecretGame.java:276 (remaining)](../game/minigame/SecretGame.java#L276) (countdown value); [game/minigame/SecretGame.java:293 (carrying)](../game/minigame/SecretGame.java#L293) (bundle action hint).
            Random noise = new Random((long) (animation * 12));
            g.setColor(new Color(210, 212, 195, 24));
            for (int i = 0; i < 190; i++) g.fillRect(noise.nextInt(WIDTH / 4) * 4, noise.nextInt(HEIGHT / 4) * 4, 4, 4);
            g.setColor(new Color(220, 224, 210, 18)); g.fillRect(0, (int) (animation * 70) % HEIGHT, WIDTH, 12);
            g.setColor(new Color(0, 0, 0, 40));
            for (int y = 0; y < HEIGHT; y += 8) g.fillRect(0, y, WIDTH, 4);
        }
        if (paused) {
            g.setColor(new Color(0, 0, 0, 190)); g.fillRect(360, 285, 560, 100);
            Theme.text(g, "PAUSED - PRESS ESC", 425, 350, Theme.pixel(48), white);
        }
    }
}
