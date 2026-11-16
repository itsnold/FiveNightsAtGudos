package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * Platformer rules without Swing or audio. Coordinates are world coordinates: x can extend
 * beyond the 1280-wide viewport, and SecretPanel chooses which part of the world is visible.
 * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
 */
/*
 * SecretGame
 *
 * Purpose: Runs the hidden platformer rules without owning Swing or audio.
 *
 * Execution flow: SecretPanel calls jump/action/update and draws read-only model values. The model
 * integrates motion, patrols, bundle visibility and the pickup-triggered timer.
 *
 * Design rationale: Twenty-millisecond substeps reduce missed collisions. Witnesses
 * detect the bundle center, not the player body. The same beam polygon is used for drawing and
 * detection.
 *
 * State: State is PLAYING/WON/LOST; x/y/vy are player position and vertical velocity;
 * started never resets after pickup; witnesses each have a patrol and direction. BEAM_LENGTH=140;
 * speed=190, gravity=1450, jump velocity=-600.
 *
 * Summary: Explore freely before first pickup. Then the timer keeps running even after
 * dropping or hiding the bundle. Carry it into the destination and press E to win.
 * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
 */
public final class SecretGame {
    /*
     * State
     *
     * Finite minigame outcome values PLAYING, WON and LOST. This is a state machine representation,
     * not the full GoF State object pattern.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public enum State { PLAYING, WON, LOST }
    // The same polygon is used for drawing and collision, so the visible light is the danger area. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    public static final int BEAM_LENGTH = 140;
    private final SecretLevel level;
    // x/y are Gudo's top-left position; vy is vertical speed in scene units per second. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    // The floor puts Gudo at y=570 and the dropped bundle at y=612. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    private double x, y = 570, vy, bundleX, bundleY = 612, remaining;
    // started remains true after the first pickup, even if the bundle is dropped again. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    private boolean carrying, started, grounded = true;
    private final List<Witness> witnesses = new ArrayList<>();
    // Each runtime witness has its own changing position/direction and a fixed builder-defined route. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    /*
     * Witness
     *
     * Private changing patrol state for one runtime witness: immutable Patrol reference, current x
     * and facing-right flag.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    private static final class Witness {
        final SecretLevel.Patrol patrol;
        double x;
        boolean right = true;
        /*
         * Retain this patrol and start the witness at its left endpoint facing right. Each created
         * Witness changes position independently during update.
         * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
         */
        Witness(SecretLevel.Patrol patrol) { this.patrol = patrol; x = patrol.left(); }
    }
    private State state = State.PLAYING;

    /*
     * Receive the completed SecretLevel product, initialize player/bundle positions and countdown,
     * and create separate runtime Witness objects for its patrols. started remains false until the
     * first successful pickup.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public SecretGame(SecretLevel level) {
        this.level = level; x = level.playerX(); bundleX = level.bundleX();
        for (SecretLevel.Patrol patrol : level.patrols()) witnesses.add(new Witness(patrol));
        remaining = level.seconds();
    }

    // A negative vertical velocity launches Gudo upward; only a grounded player may jump. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    /*
     * Accept only PLAYING and grounded; set vertical velocity to -600 and clear grounded. Negative y
     * velocity moves up in this coordinate system; holding Space is separately debounced by
     * SecretPanel.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public void jump() { if (state == State.PLAYING && grounded) { vy = -600; grounded = false; } }
    /** E-key action: deliver first, otherwise drop what is carried or pick up a nearby bundle. Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). */
    /*
     * Ignore ended games. If carrying and the player delivery point is inside destination, win
     * before considering drop. Otherwise drop the carried bundle at the player or pick up a nearby
     * one within the distance thresholds. Pickup sets started permanently, so re-pickup never resets
     * remaining.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public void action() {
        if (state != State.PLAYING) return;
        // Delivery takes precedence over dropping when Gudo is inside the stockroom. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
        if (carrying && level.destination().contains(x + 20, y + 40)) { state = State.WON; return; }
        // Dropping leaves the bundle in the world; re-picking it never resets the countdown. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
        if (carrying) { carrying = false; bundleX = x + 12; bundleY = y + 42; }
        else if (Math.abs(bundleX - x) < 55 && Math.abs(bundleY - (y + 42)) < 65) {
            // The first successful pickup starts the countdown; the same flag stays true on re-pickup. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            carrying = true; started = true;
        }
    }
    /** Advance active play by elapsed seconds; the UI skips this call while paused or transitioning. Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). */
    /*
     * While PLAYING, divide elapsed input into <=0.02-second steps. If started, subtract countdown
     * and lose at zero; update horizontal movement, gravity, ground/platform landings,
     * dropped-bundle falling and independent patrols. Then test the carried/dropped bundle center
     * against all beams unless cover hides it. The model assumes valid nonnegative finite deltas
     * from its caller.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public void update(double seconds, boolean left, boolean right) {
        if (state != State.PLAYING) return;
        // Short steps avoid falling through platforms and skipping a witness at low frame rates. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
        while (seconds > 0 && state == State.PLAYING) {
            double dt = Math.min(seconds, 0.02); seconds -= dt;
            // Exploring before pickup costs no time. The countdown continues while the bundle is hidden. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            if (started && (remaining -= dt) <= 0) { remaining = 0; state = State.LOST; break; }
            // Opposite inputs cancel, and the player's position stays within the game screen. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            x = Math.max(0, Math.min(level.width() - 40, x + ((right ? 1 : 0) - (left ? 1 : 0)) * 190 * dt));
            double oldBottom = y + 70;
            // Integrate gravity, then land only when crossing a platform's top while descending. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            vy += 1450 * dt; y += vy * dt; grounded = false;
            if (y >= 570) { y = 570; vy = 0; grounded = true; }
            for (Rectangle p : level.platforms()) {
                if (vy >= 0 && oldBottom <= p.y && y + 70 >= p.y && x + 35 > p.x && x < p.x + p.width) {
                    y = p.y - 70; vy = 0; grounded = true;
                }
            }
            // A bundle dropped in midair falls to the floor independently of Gudo. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            if (!carrying && bundleY < 612) bundleY = Math.min(612, bundleY + 300 * dt);
            // Each witness has an independent route and reverses at its own endpoints. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            for (Witness witness : witnesses) {
                witness.x += (witness.right ? 1 : -1) * 75 * dt;
                if (witness.x >= witness.patrol.right()) { witness.x = witness.patrol.right(); witness.right = false; }
                if (witness.x <= witness.patrol.left()) { witness.x = witness.patrol.left(); witness.right = true; }
            }
            if (started) {
                double bx = carrying ? x + 20 : bundleX + 12;
                double by = carrying ? y + 48 : bundleY + 10;
                // Only the bundle attracts attention: it can be carried or lying on the floor. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
                // Shelves hide its center point; the player alone never triggers the witness. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
                boolean hidden = bundleHidden();
                if (!hidden) for (int i = 0; i < witnesses.size(); i++) {
                    if (witnessBeam(i).contains(bx, by)) { state = State.LOST; break; }
                }
            }
        }
    }
    // Read-only accessors let the UI draw the current model without controlling its rules. | Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    /*
     * Read the configuration product used by this minigame. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public SecretLevel level() { return level; }
    /*
     * Read the player horizontal world position. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double x() { return x; }
    /*
     * Read the player top-left vertical position. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double y() { return y; }
    /*
     * Read the dropped bundle horizontal world anchor. This accessor does not advance time, save
     * data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double bundleX() { return bundleX; }
    /*
     * Read the dropped bundle vertical world anchor. This accessor does not advance time, save data
     * or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double bundleY() { return bundleY; }
    /*
     * Read the number of independently simulated patrol witnesses. This accessor does not advance
     * time, save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public int witnessCount() { return witnesses.size(); }
    /*
     * Read the stored/computed value witnessX(0). This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double witnessX() { return witnessX(0); }
    /*
     * Read the stored/computed value witnessRight(0). This accessor does not advance time, save data
     * or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean witnessRight() { return witnessRight(0); }
    /*
     * Read the selected witness horizontal world position. This accessor does not advance time, save
     * data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double witnessX(int index) { return witnesses.get(index).x; }
    /*
     * Read the selected witness facing direction. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean witnessRight(int index) { return witnesses.get(index).right; }
    /*
     * Read the remaining pickup-triggered countdown seconds. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double remaining() { return remaining; }
    /*
     * Read whether a first bundle pickup has happened. This accessor does not advance time, save
     * data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean started() { return started; }
    /*
     * Read whether the player currently carries the bundle. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean carrying() { return carrying; }
    /*
     * Read the current minigame outcome state. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public State state() { return state; }
    /*
     * Read whether a jump is currently allowed by ground/platform contact. This accessor does not
     * advance time, save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean grounded() { return grounded; }

    /** The bundle's center determines whether foreground cover hides it from witnesses. Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). */
    /*
     * Calculate the bundle center from the carrying or dropped position and test whether any cover
     * Rectangle contains it. The player alone is irrelevant to witness detection; cover works for
     * carried and dropped bundles.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean bundleHidden() {
        double bx = carrying ? x + 20 : bundleX + 12;
        double by = carrying ? y + 48 : bundleY + 10;
        return level.cover().stream().anyMatch(r -> r.contains(bx, by));
    }

    /** Triangle anchored at the witness's light, pointing in the current patrol direction.
     * SecretPanel draws this same polygon, so the shown light and the detection area agree.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    /*
     * The no-index overload forwards to witness zero. The indexed overload creates the same
     * triangular beam used by both collision and rendering: origin at witness x/y620, far edge 140
     * units in its facing direction, spanning y550..650. This avoids a visual/rule mismatch.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public Polygon witnessBeam() { return witnessBeam(0); }
    /*
     * The no-index overload forwards to witness zero. The indexed overload creates the same
     * triangular beam used by both collision and rendering: origin at witness x/y620, far edge 140
     * units in its facing direction, spanning y550..650. This avoids a visual/rule mismatch.
     * Reviewer (42-page edition): [p. 7: hidden game](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-7); [p. 33: SecretGame](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public Polygon witnessBeam(int index) {
        Witness witness = witnesses.get(index);
        int origin = (int) Math.round(witness.x);
        int end = origin + (witness.right ? BEAM_LENGTH : -BEAM_LENGTH);
        return new Polygon(new int[]{origin, end, end}, new int[]{620, 550, 650}, 3);
    }
}
