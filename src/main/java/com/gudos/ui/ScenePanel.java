package com.gudos.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** A letterboxed 1280×720 Java2D scene with real, proportionally placed Swing controls. */
public abstract class ScenePanel extends JPanel {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    private record Slot(JComponent component, Rectangle bounds, Font font) { }
    private final List<Slot> slots = new ArrayList<>();
    protected double age;

    protected ScenePanel() {
        setLayout(null);
        setBackground(Theme.BACKGROUND);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
    }

    protected <T extends JComponent> T place(T component, int x, int y, int w, int h) {
        slots.add(new Slot(component, new Rectangle(x, y, w, h), component.getFont()));
        add(component);
        return component;
    }

    public double sceneScale() { return Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT); }
    public int offsetX() { return (int) Math.round((getWidth() - WIDTH * sceneScale()) / 2); }
    public int offsetY() { return (int) Math.round((getHeight() - HEIGHT * sceneScale()) / 2); }

    public Point2D scenePoint(Point point) {
        double scale = sceneScale();
        return scale <= 0 ? new Point2D.Double() : new Point2D.Double((point.x - offsetX()) / scale, (point.y - offsetY()) / scale);
    }

    @Override public void doLayout() {
        double scale = sceneScale();
        for (Slot slot : slots) {
            Rectangle b = slot.bounds();
            slot.component().setBounds(offsetX() + (int) Math.round(b.x * scale), offsetY() + (int) Math.round(b.y * scale),
                    (int) Math.round(b.width * scale), (int) Math.round(b.height * scale));
            if (slot.font() != null) slot.component().setFont(slot.font().deriveFont((float) Math.max(9, slot.font().getSize2D() * scale)));
        }
    }

    @Override protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (sceneScale() <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.quality(g);
        g.translate(offsetX(), offsetY());
        g.scale(sceneScale(), sceneScale());
        g.clipRect(0, 0, WIDTH, HEIGHT);
        render(g);
        g.dispose();
    }

    protected abstract void render(Graphics2D g);

    @Override protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
        if (sceneScale() <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.quality(g);
        g.translate(offsetX(), offsetY());
        g.scale(sceneScale(), sceneScale());
        g.clipRect(0, 0, WIDTH, HEIGHT);
        renderOverlay(g);
        g.dispose();
    }

    protected void renderOverlay(Graphics2D g) { }

    public void tick(double seconds) { age += seconds; }

    /** Dispatch before Swing focus traversal/default-button bindings swallow game keys. */
    public boolean dispatchKey(KeyEvent event) {
        if (!isShowing()) return false;
        Object name = getInputMap(WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStrokeForEvent(event));
        Action action = name == null ? null : getActionMap().get(name);
        if (action == null || !action.isEnabled()) return false;
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, name.toString()));
        return true;
    }

    protected void bind(String stroke, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(stroke), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                action.run();
            }
        });
    }
}
