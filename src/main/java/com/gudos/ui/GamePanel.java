package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.assets.PanoramaRenderer;
import com.gudos.audio.Sound;
import com.gudos.game.CameraMotion;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
import com.gudos.persistence.Settings;
import com.gudos.ui.rendering.CameraSceneRenderer;
import com.gudos.ui.rendering.DimmedRendererDecorator;
import com.gudos.ui.rendering.SceneRenderer;
import com.gudos.ui.rendering.VhsRendererDecorator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** The reference-style HUD is deliberately sparse: clock, power, usage, monitor. Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19). */
/*
 * GamePanel
 *
 * Purpose: Translates gameplay input, draws office/CCTV/HUD and detects model transitions for audio
 * cues.
 *
 * Execution flow: Application advances GameSession, then calls this tick; input callbacks request
 * model changes; render draws the current model.
 *
 * Design rationale: Keep rules in GameSession and visual/input feedback here. Use
 * held-key flags, callbacks and Supplier<Settings>. Compose camera rendering effects through the
 * custom Decorator objects.
 *
 * State: pan/targetPan ease office yaw; projections reuse buffers;
 * cameraRenderer/cameraTransitionRenderer are effect chains; lastGudoLocation/lastRepelled prevent
 * repeated cues; garbledCamera/garbleUntil bound disruption; paperClicks counts valid CAM 04 paper
 * clicks.
 *
 * Summary: The panel can request a monitor toggle, but the model decides whether it is legal.
 * Sound is played only when an accepted change or encounter transition occurs.
 * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
 */
public final class GamePanel extends ScenePanel {
    private static final double GARBLE_SECONDS = 2;
    private static final Sound[] GARBLES = {Sound.GARBLE_1, Sound.GARBLE_2, Sound.GARBLE_3};
    private final GameSession session;
    private final AssetManager assets;
    // Separate renderers keep their own reusable output images, allowing clean/occupied views to blend. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    private final PanoramaRenderer officeProjection = new PanoramaRenderer();
    private final PanoramaRenderer occupiedProjection = new PanoramaRenderer();
    // Match the camera's sweep when translating clicks back to the source-image paper. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    private final PanoramaRenderer cameraProjection = new PanoramaRenderer();
    private final SceneRenderer cameraRenderer;
    private final SceneRenderer cameraTransitionRenderer;
    // Callbacks let this screen read current preferences, request a sound, or pause through Application. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    private final Supplier<Settings> settings;
    private final Consumer<Sound> sound;
    private final Runnable pause;
    private final GameButton monitorButton;
    private final GameButton lightButton;
    private final List<GameButton> cameras = new ArrayList<>();
    // Track held inputs separately from the simulation; releaseControls clears them on pause/exit. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    private boolean leftHeld;
    private boolean rightHeld;
    private boolean keyboardLight;
    private boolean mouseLight;
    private boolean spaceHeld;
    private boolean tabHeld;
    private boolean pointerInside;
    private Point2D pointer = new Point2D.Double(640, 360);
    private double targetPan = 0.5;
    private double pan = 0.5;
    private double transition;
    private Scene lastGudoLocation;
    private int lastRepelled;
    private double retreatNotice;
    private Scene garbledCamera;
    private double garbleUntil;
    private Sound garbleSound;
    private int garbleCount;
    private boolean rushPlayed;
    private boolean lowPowerPlayed;
    private int roomNoiseIndex;
    private double nextRoomNoise = 31;
    private Runnable secret = () -> { };
    private int paperClicks;

    /*
     * Retain the model/assets and supplied settings/sound/pause callbacks; compose the normal and
     * switching SceneRenderer decorator chains. Create monitor/light/camera Swing controls,
     * press/release keyboard bindings, mouse-edge tracking and the reverse-projected five-click
     * CAM04 secret trigger. Finally synchronize controls to the model.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    public GamePanel(GameSession session, AssetManager assets, Supplier<Settings> settings,
                     Consumer<Sound> sound, Runnable pause) {
        this.session = session;
        this.assets = assets;
        this.settings = settings;
        this.sound = sound;
        this.pause = pause;
        // Our Decorator pattern: the same component interface supports stacked visual effects. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        cameraRenderer = new VhsRendererDecorator(new CameraSceneRenderer(session, assets), assets, 0.25f);
        cameraTransitionRenderer = new VhsRendererDecorator(
                new DimmedRendererDecorator(cameraRenderer, 115), assets, 0.85f, 3);
        setFocusTraversalKeysEnabled(false);
        monitorButton = place(button("OPEN MONITOR    [SPACE]", GameButton.Style.MONITOR, this::toggleMonitor), 335, 647, 650, 55);
        monitorButton.getAccessibleContext().setAccessibleDescription("Raise or lower the monitor with Space or Tab.");
        lightButton = place(button("HOLD LIGHT  [F]", GameButton.Style.LIGHT, () -> { }), 21, 326, 74, 127);
        lightButton.getAccessibleContext().setAccessibleDescription("Face Sir Gudo's doorway and hold F until he fades away.");
        lightButton.addMouseListener(new MouseAdapter() {
            /*
             * Accept a left press on the light control, mark mouseLight held and pass the combined
             * hold state through updateLight.
             * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
             */
            @Override public void mousePressed(MouseEvent event) { if (SwingUtilities.isLeftMouseButton(event)) { mouseLight = true; updateLight(); } }
            /*
             * Clear mouseLight on release and recompute accepted light state. Keyboard F may still
             * hold the light independently.
             * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
             */
            @Override public void mouseReleased(MouseEvent event) { mouseLight = false; updateLight(); }
            /*
             * For the light control, clear mouseLight and update it so dragging away cannot leave
             * light stuck. For the scene, clear pointerInside so an absent pointer does not keep
             * edge-panning.
             * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
             */
            @Override public void mouseExited(MouseEvent event) { mouseLight = false; updateLight(); }
        });
        // The six real Swing camera buttons are positioned over the floor-plan drawing. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        int[][] positions = {{993, 378}, {880, 450}, {880, 510}, {880, 576}, {1125, 450}, {1125, 513}};
        for (int camera = 1; camera <= 6; camera++) {
            int number = camera;
            GameButton control = button("CAM\n%02d".formatted(camera), GameButton.Style.CAMERA, () -> selectCamera(number));
            control.getAccessibleContext().setAccessibleName("CAM %02d — %s [%d]".formatted(camera, Scene.camera(camera).label(), camera));
            cameras.add(place(control, positions[camera - 1][0], positions[camera - 1][1], 64, 42));
            bind("pressed " + camera, "camera-" + camera, () -> selectCamera(number));
        }
        // Press/release pairs support held movement/light; monitor toggles only once per key press. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        bind("pressed SPACE", "monitor-space", () -> { if (!spaceHeld) { spaceHeld = true; toggleMonitor(); } });
        bind("released SPACE", "monitor-space-release", () -> spaceHeld = false);
        bind("pressed TAB", "monitor-tab", () -> { if (!tabHeld) { tabHeld = true; toggleMonitor(); } });
        bind("released TAB", "monitor-tab-release", () -> tabHeld = false);
        bind("ESCAPE", "pause", () -> { releaseControls(); pause.run(); });
        bind("pressed F", "light-on", () -> { keyboardLight = true; updateLight(); });
        bind("released F", "light-off", () -> { keyboardLight = false; updateLight(); });
        bind("pressed A", "left-a", () -> leftHeld = true);
        bind("released A", "left-a-off", () -> leftHeld = false);
        bind("pressed LEFT", "left-arrow", () -> leftHeld = true);
        bind("released LEFT", "left-arrow-off", () -> leftHeld = false);
        bind("pressed D", "right-d", () -> rightHeld = true);
        bind("released D", "right-d-off", () -> rightHeld = false);
        bind("pressed RIGHT", "right-arrow", () -> rightHeld = true);
        bind("released RIGHT", "right-arrow-off", () -> rightHeld = false);
        // Convert window pixels to the same 1280x720 coordinate system used by the scene. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        addMouseMotionListener(new MouseAdapter() {
            /*
             * Convert the event point through scenePoint and mark the pointer inside. This aligns
             * mouse-edge pan with letterboxed/resized coordinates.
             * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
             */
            @Override public void mouseMoved(MouseEvent event) { pointer = scenePoint(event.getPoint()); pointerInside = true; }
            /*
             * Use the same pointer update as mouse movement so dragging still tracks scene
             * coordinates.
             * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
             */
            @Override public void mouseDragged(MouseEvent event) { mouseMoved(event); }
        });
        addMouseListener(new MouseAdapter() {
            /*
             * For the light control, clear mouseLight and update it so dragging away cannot leave
             * light stuck. For the scene, clear pointerInside so an absent pointer does not keep
             * edge-panning.
             * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
             */
            @Override public void mouseExited(MouseEvent event) { pointerInside = false; }
            /*
             * For a left click on active CAM 04, reproject the current source/sweep, reverse-map the
             * click and accept only source fractions x=0.285..0.325 and y=0.24..0.40. The fifth
             * valid paper click resets the counter and invokes secret. Application separately
             * rejects Night 5. Invalid clicks do not increment or reset the count.
             * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
             */
            @Override public void mouseClicked(MouseEvent event) {
                // Only left-clicks on the active CAM 04 feed can count toward the hidden trigger. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
                if (!SwingUtilities.isLeftMouseButton(event) || !session.monitorOpen() || session.camera() != 4 || session.paused() || session.finished()) return;
                Point2D p = scenePoint(event.getPoint());
                // The projection is recomputed at the current sweep, then mapped back to the source image. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
                var source = assets.scene(Scene.CAM_04, session.sceneState(Scene.CAM_04));
                cameraProjection.render(source, CameraMotion.pan(Scene.CAM_04, session.elapsed()));
                Point pixel = cameraProjection.sourcePoint((int) (p.getX() * PanoramaRenderer.WIDTH / WIDTH), (int) (p.getY() * PanoramaRenderer.HEIGHT / HEIGHT));
                // The bounds are fractions of the original stockroom image, so the target follows | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
                // camera panning and window resizing. Only a click inside the paper increments the count. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
                if (pixel != null && pixel.x >= source.getWidth() * 0.285 && pixel.x <= source.getWidth() * 0.325
                        && pixel.y >= source.getHeight() * 0.24 && pixel.y <= source.getHeight() * 0.40 && ++paperClicks >= 5) {
                    paperClicks = 0;
                    secret.run();
                }
            }
        });
        syncControls();
    }

    // Application supplies the navigation callback; this panel does not construct the minigame. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Store the Runnable provided by Application. The panel detects a paper interaction; Application
     * alone constructs/navigates/saves the hidden game.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    public void onSecret(Runnable action) { secret = action; }

    /*
     * Create a styled GameButton with callback and make it nonfocusable so game keyboard actions
     * stay under scene-level routing.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private GameButton button(String label, GameButton.Style style, Runnable action) {
        GameButton button = new GameButton(label, style, action);
        button.setFocusable(false);
        return button;
    }

    // Clear physical input state before switching screens so keys cannot remain logically held. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Clear all held movement/light/pointer/monitor-key flags and force the model light off. Call
     * before pause, exit or a hidden-screen switch to avoid stuck inputs.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    public void releaseControls() {
        leftHeld = rightHeld = keyboardLight = mouseLight = pointerInside = spaceHeld = tabHeld = false;
        session.setLight(false);
    }

    /*
     * Expose the current smoothed visual pan; GameSession receives the same value for directional
     * flashlight rules. This differs from the automatic CCTV sweep.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    public double officePan() { return pan; }

    /** An arrival/departure affects only the feed watched at that moment, for two active shift seconds. Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19). */
    /*
     * Return the selected feed disruption cue only for an active unpaused monitor, the matching
     * garbledCamera, and elapsed before garbleUntil. Switching away silences it; returning before
     * expiry uses the original deadline/variant.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    public Sound cameraGarbleSound() {
        return !session.paused() && !session.finished() && session.monitorOpen()
                && Scene.camera(session.camera()) == garbledCamera && session.elapsed() < garbleUntil
                ? garbleSound : null;
    }

    // Either input can hold the light; GameSession still enforces monitor/pause restrictions. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Combine keyboard and mouse hold state, ask GameSession to enforce restrictions, then emit
     * ON/OFF only if the accepted model state changed. A blocked press must not produce misleading
     * audio.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void updateLight() {
        boolean before = session.lightOn();
        session.setLight(keyboardLight || mouseLight);
        if (before != session.lightOn()) sound.accept(session.lightOn() ? Sound.FLASHLIGHT_ON : Sound.FLASHLIGHT_OFF);
    }

    // Request the model change first, then play feedback only if it actually happened. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Clear physical light flags, ask the model to toggle, and if accepted set a 0.20-second visual
     * transition plus open/close cue. Then synchronize button labels/visibility. GameSession is
     * still the rule authority.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void toggleMonitor() {
        boolean before = session.monitorOpen();
        keyboardLight = mouseLight = false;
        session.toggleMonitor();
        if (session.monitorOpen() != before) {
            transition = 0.20;
            sound.accept(session.monitorOpen() ? Sound.MONITOR_OPEN : Sound.MONITOR_CLOSE);
        }
        syncControls();
    }

    // Camera selection is accepted by the model only while the monitor is open. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Remember the previous camera, request a validated model selection and emit a 0.17-second
     * transition/camera-switch cue only if it actually changed. Reselecting the current feed stays
     * silent.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void selectCamera(int number) {
        int before = session.camera();
        session.selectCamera(number);
        if (session.camera() != before) {
            transition = 0.17;
            sound.accept(Sound.CAMERA_SWITCH);
        }
        syncControls();
    }

    // Mirror the model in button visibility, selection, and labels; these controls do not own game state. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Mirror monitor/camera/light state into actual buttons: show camera buttons only with monitor
     * up, show light only in office, update selected states and accessible monitor label. These
     * controls display the state rather than own it.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void syncControls() {
        for (int i = 0; i < cameras.size(); i++) {
            cameras.get(i).setVisible(session.monitorOpen());
            cameras.get(i).setSelected(session.camera() == i + 1);
        }
        lightButton.setVisible(!session.monitorOpen());
        lightButton.setSelected(session.lightOn());
        String label = session.monitorOpen() ? "LOWER MONITOR    [SPACE]" : "OPEN MONITOR    [SPACE]";
        if (!monitorButton.getText().equals(label)) {
            monitorButton.setText(label);
            monitorButton.getAccessibleContext().setAccessibleName(label);
        }
    }

    // Update visual/input state after Application has advanced GameSession for this frame. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * After the model update, decrease transitions, derive office direction from keys or mouse
     * edges, ease pan using 1-exp(-9*dt) and send it back to the model. Compare encounter
     * location/repelled counters for one-time footsteps/arrival/retreat cues, mark watched-feed
     * garble for two active seconds, and schedule urgency, low-power and room creak/rattle cues.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    @Override public void tick(double seconds) {
        super.tick(seconds);
        transition = Math.max(0, transition - seconds);
        if (!session.monitorOpen() && !session.paused() && !session.finished()) {
            double direction = (rightHeld ? 1 : 0) - (leftHeld ? 1 : 0);
            if (direction == 0 && pointerInside && pointer.getY() >= 0 && pointer.getY() < 630) {
                if (pointer.getX() < 340) direction = -(340 - pointer.getX()) / 340;
                else if (pointer.getX() > 940) direction = (pointer.getX() - 940) / 340;
            }
            // Ease toward the requested direction rather than snapping the office image sideways. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
            targetPan = Math.max(0, Math.min(1, targetPan + direction * seconds * 0.72));
            pan += (targetPan - pan) * (1 - Math.exp(-seconds * 9));
            session.setOfficePan(pan);
        }
        if (!session.paused() && !session.finished()) {
            retreatNotice = Math.max(0, retreatNotice - seconds);
            Scene location = session.gudo().location();
            // Compare successive model states so footsteps/warnings play once per location change. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
            if (location != lastGudoLocation) {
                rushPlayed = false;
                // All camera/office moves use the same deep footsteps. A successful retreat has its own run cue. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
                if (location != null) sound.accept(Sound.GUDO_STEP);
                if (session.gudo().atOffice()) {
                    sound.accept(switch (session.gudo().approach()) {
                        case LEFT -> Sound.GUDO_WARNING;
                        case RIGHT -> Sound.GUDO_POUND;
                        case FRONT -> Sound.GUDO_WHISPER;
                    });
                }
                // Watching him enter or leave a feed disrupts that feed; unrelated cameras stay clear. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
                Scene watched = Scene.camera(session.camera());
                if (session.monitorOpen() && (lastGudoLocation == watched || location == watched)) {
                    garbledCamera = watched;
                    garbleUntil = session.elapsed() + GARBLE_SECONDS;
                    garbleSound = GARBLES[garbleCount++ % GARBLES.length];
                }
                lastGudoLocation = location;
            }
            if (session.gudo().atOffice() && session.gudo().attackRemaining() <= 3 && !rushPlayed) {
                rushPlayed = true;
                sound.accept(Sound.GUDO_RUSH);
            }
            if (session.power() <= 20 && !lowPowerPlayed) {
                lowPowerPlayed = true;
                sound.accept(Sound.LOW_POWER);
            }
            if (session.elapsed() >= nextRoomNoise && !session.gudo().atOffice()) {
                sound.accept(roomNoiseIndex++ % 2 == 0 ? Sound.METAL_CREAK : Sound.METAL_RATTLE);
                nextRoomNoise = session.elapsed() + 33 + roomNoiseIndex % 3 * 7;
            }
            if (session.gudo().repelled() != lastRepelled) {
                lastRepelled = session.gudo().repelled();
                retreatNotice = 2;
                sound.accept(Sound.GUDO_RETREAT);
            }
        }
        syncControls();
    }

    // Draw the current world view first, then the clock, power, and encounter messages on top. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Draw either office or CCTV based on model monitorOpen, then draw HUD. Rendering reads model
     * state; it does not advance encounter timers.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    @Override protected void render(Graphics2D g) {
        if (session.monitorOpen()) renderCamera(g);
        else renderOffice(g);
        renderHud(g);
    }

    // Flashlight exposure blends the occupied panorama toward the clean room as Gudo retreats. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Use clean and occupied projection buffers at the same pan. During defense blend occupied
     * alpha=1-fade over clean, then draw light/tint/vignette/HUD shading. Two renderer buffers
     * prevent the second render from overwriting the first blend input.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void renderOffice(Graphics2D g) {
        double fade = session.gudo().fadeProgress();
        if (session.gudo().atOffice() && fade > 0) {
            g.drawImage(officeProjection.render(assets.scene(Scene.OFFICE, "default"), pan), 0, 0, WIDTH, HEIGHT, null);
            g.setComposite(AlphaComposite.SrcOver.derive((float) (1 - fade)));
            g.drawImage(occupiedProjection.render(assets.scene(Scene.OFFICE, session.sceneState(Scene.OFFICE)), pan), 0, 0, WIDTH, HEIGHT, null);
            g.setComposite(AlphaComposite.SrcOver);
        } else {
            g.drawImage(officeProjection.render(assets.scene(Scene.OFFICE, session.sceneState(Scene.OFFICE)), pan), 0, 0, WIDTH, HEIGHT, null);
        }
        g.setColor(new Color(0, 0, 0, session.lightOn() ? 2 : 40));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        if (session.lightOn()) {
            g.setPaint(new RadialGradientPaint(new Point2D.Float(640, 350), 540,
                    new float[]{0, 0.4f, 1}, new Color[]{new Color(255, 245, 215, 65), new Color(255, 245, 215, 22), new Color(255, 245, 215, 0)}));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
        Theme.vignette(g, 235);
        shadeHud(g);
    }

    // Camera movement uses the session clock, not the mouse or time spent watching this feed. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * If the watched feed is disrupted, fully obscure it with animated noise or a still SIGNAL LOST
     * screen in reduced mode. Otherwise choose normal or switching Decorator chain. Draw flat CCTV
     * border/record dot/room label/map on top. It never moves the model based on pixels.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void renderCamera(Graphics2D g) {
        Scene scene = Scene.camera(session.camera());
        if (cameraGarbleSound() != null) {
            // Fully obscure the changed room, including with reduced visual effects enabled. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
            g.setColor(new Color(15, 17, 18));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            if (settings.get().reducedEffects()) {
                Theme.text(g, "SIGNAL LOST", 72, 280, Theme.pixel(42), Theme.MUTED);
            } else {
                VhsOverlay.paint(g, assets, session.elapsed() * 3, 1f, false);
            }
        } else {
            boolean reducedEffects = settings.get().reducedEffects();
            SceneRenderer renderer = transition > 0 && !reducedEffects ? cameraTransitionRenderer : cameraRenderer;
            renderer.render(g, age, reducedEffects);
        }
        shadeHud(g);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawRect(10, 9, 1259, 701);
        if (settings.get().reducedEffects() || (int) (age * 1.2) % 2 == 0) {
            g.setColor(Color.RED);
            g.fillOval(37, 31, 26, 26);
        }
        Theme.text(g, scene.label(), 854, 335, Theme.pixel(37), Color.WHITE);
        renderMap(g);
    }

    // Draw the static floor plan; interactive camera controls are separate Swing buttons above it. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Draw the room rectangles and connecting corridors as a floor-plan schematic. The interactive
     * numbered camera controls are separate real Swing buttons, not click logic embedded in the
     * drawn lines.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void renderMap(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(845, 349, 377, 290);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawRect(974, 360, 102, 79); // Exterior front entrance | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        g.drawRect(856, 440, 108, 55); // Counter | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        g.drawRect(856, 502, 108, 55); // Kitchen | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        g.drawRect(856, 564, 108, 67); // Stockroom | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        g.drawRect(1107, 439, 104, 56); // Side corridor | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        g.drawRect(1107, 502, 104, 66); // Back alley | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        g.drawRect(989, 586, 88, 47); // Office | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        // Narrow connecting halls keep the schematic readable without a dashboard card. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
        g.drawPolyline(new int[]{997, 997, 964}, new int[]{439, 466, 466}, 3);
        g.drawPolyline(new int[]{1007, 1007, 964}, new int[]{439, 476, 476}, 3);
        g.drawPolyline(new int[]{1040, 1040, 1107}, new int[]{439, 466, 466}, 3);
        g.drawPolyline(new int[]{1050, 1050, 1107}, new int[]{439, 476, 476}, 3);
        g.drawLine(904, 495, 904, 502);
        g.drawLine(916, 495, 916, 502);
        g.drawLine(904, 557, 904, 564);
        g.drawLine(916, 557, 916, 564);
        g.drawLine(1154, 495, 1154, 502);
        g.drawLine(1166, 495, 1166, 502);
        Theme.text(g, "YOU", 1009, 618, Theme.pixel(28), Color.WHITE);
    }

    // Dark gradients keep HUD text readable over bright room images. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Draw top/bottom dark gradients so white text remains readable over room art. This helper
     * changes presentation only.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void shadeHud(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 100), 0, 135, new Color(0, 0, 0, 0)));
        g.fillRect(0, 0, WIDTH, 135);
        g.setPaint(new GradientPaint(0, 559, new Color(0, 0, 0, 0), 0, 720, new Color(0, 0, 0, 175)));
        g.fillRect(0, 559, WIDTH, 161);
    }

    // Read model values for warnings, defense progress, fictional time, power, and usage bars. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * Read approach warnings, defense fade, retreat notice, fictional clock, night, power and usage
     * bars. The model supplies actual values; the HUD ceil power display is rounded feedback, not
     * stored power.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    private void renderHud(Graphics2D g) {
        if (session.gudo().atOffice() && !session.finished()) {
            String warning = "FOOTSTEPS: " + session.gudo().approach().label();
            Theme.text(g, warning, 28, 52, Theme.pixel(29), Theme.AMBER);
            String instruction = session.monitorOpen() ? "LOWER MONITOR. FACE THE DOOR. HOLD F."
                    : session.lightOn() && session.gudo().approach().faces(pan) ? "KEEP THE LIGHT ON HIM..."
                    : "FACE THE DOOR. HOLD F UNTIL HE FADES.";
            Theme.text(g, instruction, 28, 83, Theme.pixel(25), Color.WHITE);
            if (!session.monitorOpen() && session.gudo().fadeProgress() > 0) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(480, 597, 320, 7);
                g.setColor(Theme.PAPER);
                g.fillRect(480, 597, (int) (320 * session.gudo().fadeProgress()), 7);
            }
        } else if (retreatNotice > 0) {
            Theme.text(g, "HE'S GONE. SAVE YOUR POWER.", 28, 52, Theme.pixel(27), Theme.GREEN);
        } else if (session.elapsed() < 18) {
            Theme.text(g, "TRACK HIM ON CCTV. HOLD F AT THE OFFICE.", 28, 52, Theme.pixel(25), Color.WHITE);
            Theme.text(g, "A / D TO FACE THE LEFT, CENTER OR RIGHT DOOR.", 28, 80, Theme.pixel(23), Theme.MUTED);
        }
        g.setFont(Theme.pixel(75));
        Theme.text(g, session.clockLabel(), WIDTH - 28 - g.getFontMetrics().stringWidth(session.clockLabel()), 69, Theme.pixel(75), Color.WHITE);
        String night = "Night " + session.night();
        g.setFont(Theme.pixel(37));
        Theme.text(g, night, WIDTH - 31 - g.getFontMetrics().stringWidth(night), 111, Theme.pixel(37), Color.WHITE);
        Theme.text(g, "Power left: " + (int) Math.ceil(session.power()) + "%", 25, 626, Theme.pixel(32), Color.WHITE);
        Theme.text(g, "Usage:", 25, 678, Theme.pixel(32), Color.WHITE);
        for (int pip = 0; pip < session.usage(); pip++) {
            g.setColor(Theme.GREEN);
            g.fillRect(123 + pip * 22, 649, 17, 34);
            g.setColor(new Color(12, 81, 0));
            g.drawRect(123 + pip * 22, 649, 17, 34);
        }
    }

    // ScenePanel calls this after painting Swing children, allowing effects to cover the whole screen. | Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
    /*
     * After Swing children, add light CCTV noise and the first-two-seconds night intro.
     * reducedEffects bypasses these animations; normal session rules remain unchanged.
     * Reviewer (42-page edition): [p. 19: GamePanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-19).
     */
    @Override protected void renderOverlay(Graphics2D g) {
        if (session.monitorOpen()) VhsOverlay.paint(g, assets, age, 0.035f, settings.get().reducedEffects());
        if (age < 2 && !settings.get().reducedEffects()) {
            float alpha = (float) (1 - Math.max(0, (age - 1.2) / 0.8));
            g.setColor(new Color(0, 0, 0, (int) (alpha * 255)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            Theme.text(g, "12:00 AM", 524, 329, Theme.pixel(58), Color.WHITE);
            Theme.text(g, "Night " + session.night(), 579, 388, Theme.pixel(34), Color.WHITE);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }
}
