package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.assets.PanoramaRenderer;
import com.gudos.audio.Sound;
import com.gudos.game.CameraMotion;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
import com.gudos.persistence.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** The reference-style HUD is deliberately sparse: clock, power, usage, monitor. */
public final class GamePanel extends ScenePanel {
    private final GameSession session;
    private final AssetManager assets;
    private final PanoramaRenderer officeProjection = new PanoramaRenderer();
    private final PanoramaRenderer cameraProjection = new PanoramaRenderer();
    private final Supplier<Settings> settings;
    private final Consumer<Sound> sound;
    private final Runnable pause;
    private final GameButton monitorButton;
    private final GameButton lightButton;
    private final List<GameButton> cameras = new ArrayList<>();
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

    public GamePanel(GameSession session, AssetManager assets, Supplier<Settings> settings,
                     Consumer<Sound> sound, Runnable pause) {
        this.session = session;
        this.assets = assets;
        this.settings = settings;
        this.sound = sound;
        this.pause = pause;
        setFocusTraversalKeysEnabled(false);
        monitorButton = place(button("OPEN MONITOR    [SPACE]", GameButton.Style.MONITOR, this::toggleMonitor), 335, 647, 650, 55);
        monitorButton.setToolTipText("Raise/lower monitor [Space / Tab]");
        lightButton = place(button("HOLD LIGHT  [F]", GameButton.Style.LIGHT, () -> { }), 21, 326, 74, 127);
        lightButton.setToolTipText("Hold this button or F to use the office light.");
        lightButton.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { if (SwingUtilities.isLeftMouseButton(event)) { mouseLight = true; updateLight(); } }
            @Override public void mouseReleased(MouseEvent event) { mouseLight = false; updateLight(); }
            @Override public void mouseExited(MouseEvent event) { mouseLight = false; updateLight(); }
        });
        int[][] positions = {{993, 378}, {880, 450}, {880, 510}, {880, 576}, {1125, 450}, {1125, 513}};
        for (int camera = 1; camera <= 6; camera++) {
            int number = camera;
            GameButton control = button("CAM\n%02d".formatted(camera), GameButton.Style.CAMERA, () -> selectCamera(number));
            control.setToolTipText("CAM %02d — %s [%d]".formatted(camera, Scene.camera(camera).label(), camera));
            control.getAccessibleContext().setAccessibleName(control.getToolTipText());
            cameras.add(place(control, positions[camera - 1][0], positions[camera - 1][1], 64, 42));
            bind("pressed " + camera, "camera-" + camera, () -> selectCamera(number));
        }
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
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { pointer = scenePoint(event.getPoint()); pointerInside = true; }
            @Override public void mouseDragged(MouseEvent event) { mouseMoved(event); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent event) { pointerInside = false; }
        });
        syncControls();
    }

    private GameButton button(String label, GameButton.Style style, Runnable action) {
        GameButton button = new GameButton(label, style, action);
        button.setFocusable(false);
        return button;
    }

    public void releaseControls() {
        leftHeld = rightHeld = keyboardLight = mouseLight = pointerInside = spaceHeld = tabHeld = false;
        session.setLight(false);
    }

    public double officePan() { return pan; }

    private void updateLight() { session.setLight(keyboardLight || mouseLight); }

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

    private void selectCamera(int number) {
        int before = session.camera();
        session.selectCamera(number);
        if (session.camera() != before) {
            transition = 0.17;
            sound.accept(Sound.CAMERA_SWITCH);
        }
        syncControls();
    }

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

    @Override public void tick(double seconds) {
        super.tick(seconds);
        transition = Math.max(0, transition - seconds);
        if (!session.monitorOpen() && !session.paused()) {
            double direction = (rightHeld ? 1 : 0) - (leftHeld ? 1 : 0);
            if (direction == 0 && pointerInside && pointer.getY() >= 0 && pointer.getY() < 630) {
                if (pointer.getX() < 340) direction = -(340 - pointer.getX()) / 340;
                else if (pointer.getX() > 940) direction = (pointer.getX() - 940) / 340;
            }
            targetPan = Math.max(0, Math.min(1, targetPan + direction * seconds * 0.72));
            pan += (targetPan - pan) * (1 - Math.exp(-seconds * 9));
        }
        syncControls();
    }

    @Override protected void render(Graphics2D g) {
        if (session.monitorOpen()) renderCamera(g);
        else renderOffice(g);
        renderHud(g);
    }

    private void renderOffice(Graphics2D g) {
        g.drawImage(officeProjection.render(assets.scene(Scene.OFFICE, session.sceneState(Scene.OFFICE)), pan), 0, 0, WIDTH, HEIGHT, null);
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

    private void renderCamera(Graphics2D g) {
        Scene scene = Scene.camera(session.camera());
        double sweep = CameraMotion.pan(scene, session.elapsed());
        g.drawImage(cameraProjection.render(assets.scene(scene, session.sceneState(scene)), sweep), 0, 0, WIDTH, HEIGHT, null);
        g.setColor(new Color(0, 0, 0, 32));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Theme.vignette(g, 205);
        VhsOverlay.paint(g, assets, age, 0.25f, settings.get().reducedEffects());
        if (transition > 0 && !settings.get().reducedEffects()) {
            g.setColor(new Color(0, 0, 0, 115));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            VhsOverlay.paint(g, assets, age * 3, 0.85f, false);
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

    private void renderMap(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(845, 349, 377, 290);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawRect(974, 360, 102, 79); // Dining room
        g.drawRect(856, 440, 108, 55); // Counter
        g.drawRect(856, 502, 108, 55); // Kitchen
        g.drawRect(856, 564, 108, 67); // Stockroom
        g.drawRect(1107, 439, 104, 56); // Side corridor
        g.drawRect(1107, 502, 104, 66); // Back alley
        g.drawRect(989, 586, 88, 47); // Office
        // Narrow connecting halls keep the schematic readable without a dashboard card.
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

    private void shadeHud(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 100), 0, 135, new Color(0, 0, 0, 0)));
        g.fillRect(0, 0, WIDTH, 135);
        g.setPaint(new GradientPaint(0, 559, new Color(0, 0, 0, 0), 0, 720, new Color(0, 0, 0, 175)));
        g.fillRect(0, 559, WIDTH, 161);
    }

    private void renderHud(Graphics2D g) {
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
