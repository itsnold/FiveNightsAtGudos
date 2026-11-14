package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * Platformer rules without Swing or audio. Coordinates are world coordinates: x can extend
 * beyond the 1280-wide viewport, and SecretPanel chooses which part of the world is visible.
 */
public final class SecretGame {
    public enum State { PLAYING, WON, LOST }
    // The same polygon is used for drawing and collision, so the visible light is the danger area.
    public static final int BEAM_LENGTH = 140;
    private final SecretLevel level;
    // x/y are Gudo's top-left position; vy is vertical speed in scene units per second.
    // The floor puts Gudo at y=570 and the dropped bundle at y=612.
    private double x, y = 570, vy, bundleX, bundleY = 612, remaining;
    // started remains true after the first pickup, even if the bundle is dropped again.
    private boolean carrying, started, grounded = true;
    private final List<Witness> witnesses = new ArrayList<>();
    // Each runtime witness has its own changing position/direction and a fixed builder-defined route.
    private static final class Witness {
        final SecretLevel.Patrol patrol;
        double x;
        boolean right = true;
        Witness(SecretLevel.Patrol patrol) { this.patrol = patrol; x = patrol.left(); }
    }
    private State state = State.PLAYING;

    public SecretGame(SecretLevel level) {
        this.level = level; x = level.playerX(); bundleX = level.bundleX();
        for (SecretLevel.Patrol patrol : level.patrols()) witnesses.add(new Witness(patrol));
        remaining = level.seconds();
    }

    // A negative vertical velocity launches Gudo upward; only a grounded player may jump.
    public void jump() { if (state == State.PLAYING && grounded) { vy = -600; grounded = false; } }
    /** E-key action: deliver first, otherwise drop what is carried or pick up a nearby bundle. */
    public void action() {
        if (state != State.PLAYING) return;
        // Delivery takes precedence over dropping when Gudo is inside the stockroom.
        if (carrying && level.destination().contains(x + 20, y + 40)) { state = State.WON; return; }
        // Dropping leaves the bundle in the world; re-picking it never resets the countdown.
        if (carrying) { carrying = false; bundleX = x + 12; bundleY = y + 42; }
        else if (Math.abs(bundleX - x) < 55 && Math.abs(bundleY - (y + 42)) < 65) {
            // The first successful pickup starts the countdown; the same flag stays true on re-pickup.
            carrying = true; started = true;
        }
    }
    /** Advance active play by elapsed seconds; the UI skips this call while paused or transitioning. */
    public void update(double seconds, boolean left, boolean right) {
        if (state != State.PLAYING) return;
        // Short steps avoid falling through platforms and skipping a witness at low frame rates.
        while (seconds > 0 && state == State.PLAYING) {
            double dt = Math.min(seconds, 0.02); seconds -= dt;
            // Exploring before pickup costs no time. The countdown continues while the bundle is hidden.
            if (started && (remaining -= dt) <= 0) { remaining = 0; state = State.LOST; break; }
            // Opposite inputs cancel, and the player's position stays within the game screen.
            x = Math.max(0, Math.min(level.width() - 40, x + ((right ? 1 : 0) - (left ? 1 : 0)) * 190 * dt));
            double oldBottom = y + 70;
            // Integrate gravity, then land only when crossing a platform's top while descending.
            vy += 1450 * dt; y += vy * dt; grounded = false;
            if (y >= 570) { y = 570; vy = 0; grounded = true; }
            for (Rectangle p : level.platforms()) {
                if (vy >= 0 && oldBottom <= p.y && y + 70 >= p.y && x + 35 > p.x && x < p.x + p.width) {
                    y = p.y - 70; vy = 0; grounded = true;
                }
            }
            // A bundle dropped in midair falls to the floor independently of Gudo.
            if (!carrying && bundleY < 612) bundleY = Math.min(612, bundleY + 300 * dt);
            // Each witness has an independent route and reverses at its own endpoints.
            for (Witness witness : witnesses) {
                witness.x += (witness.right ? 1 : -1) * 75 * dt;
                if (witness.x >= witness.patrol.right()) { witness.x = witness.patrol.right(); witness.right = false; }
                if (witness.x <= witness.patrol.left()) { witness.x = witness.patrol.left(); witness.right = true; }
            }
            if (started) {
                double bx = carrying ? x + 20 : bundleX + 12;
                double by = carrying ? y + 48 : bundleY + 10;
                // Only the bundle attracts attention: it can be carried or lying on the floor.
                // Shelves hide its center point; the player alone never triggers the witness.
                boolean hidden = bundleHidden();
                if (!hidden) for (int i = 0; i < witnesses.size(); i++) {
                    if (witnessBeam(i).contains(bx, by)) { state = State.LOST; break; }
                }
            }
        }
    }
    // Read-only accessors let the UI draw the current model without controlling its rules.
    public SecretLevel level() { return level; }
    public double x() { return x; } public double y() { return y; }
    public double bundleX() { return bundleX; } public double bundleY() { return bundleY; }
    public int witnessCount() { return witnesses.size(); }
    public double witnessX() { return witnessX(0); } public boolean witnessRight() { return witnessRight(0); }
    public double witnessX(int index) { return witnesses.get(index).x; }
    public boolean witnessRight(int index) { return witnesses.get(index).right; }
    public double remaining() { return remaining; } public boolean started() { return started; }
    public boolean carrying() { return carrying; } public State state() { return state; }
    public boolean grounded() { return grounded; }

    /** The bundle's center determines whether foreground cover hides it from witnesses. */
    public boolean bundleHidden() {
        double bx = carrying ? x + 20 : bundleX + 12;
        double by = carrying ? y + 48 : bundleY + 10;
        return level.cover().stream().anyMatch(r -> r.contains(bx, by));
    }

    /** Triangle anchored at the witness's light, pointing in the current patrol direction.
     * SecretPanel draws this same polygon, so the shown light and the detection area agree. */
    public Polygon witnessBeam() { return witnessBeam(0); }
    public Polygon witnessBeam(int index) {
        Witness witness = witnesses.get(index);
        int origin = (int) Math.round(witness.x);
        int end = origin + (witness.right ? BEAM_LENGTH : -BEAM_LENGTH);
        return new Polygon(new int[]{origin, end, end}, new int[]{620, 550, 650}, 3);
    }
}
