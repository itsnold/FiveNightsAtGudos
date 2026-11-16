package com.gudos.game;

/** One enemy, three connected approaches. All timers use the active shift clock. Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). */
/*
 * GudoEncounter
 *
 * Purpose: Implements one deterministic enemy encounter state machine and three approach routes.
 *
 * Execution flow: GameSession asks untilEvent, advances that exact step, then checks caught. The UI
 * reads location, approach, exposure/fade and attack countdown.
 *
 * Design rationale: routeIndex=-1 represents waiting, camera indices represent
 * travel, atOffice represents defense, and caught is terminal. Successful flashlight exposure repels
 * Gudo; interrupted exposure decays rather than instantly resetting.
 *
 * State: timer is active seconds to the next event; exposure is effective flashlight
 * time; repelled counts successful defenses. Approach stores route, source-state name, label and
 * target pan.
 *
 * Summary: Gudo is not moving randomly or using machine learning. Successful defenses cycle
 * left, right, center, then left again; timing becomes harder by night.
 * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
 */
public final class GudoEncounter {
    // Each approach defines the CCTV route followed before reaching its office doorway. | Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    /*
     * Approach
     *
     * Enemy route metadata: state key, displayed direction, target normalized pan and camera path.
     * This enum is configuration, not separate polymorphic State classes.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public enum Approach {
        LEFT("left", "LEFT / SERVICE DOOR", 0.12, new Scene[]{Scene.CAM_04, Scene.CAM_03, Scene.CAM_02}),
        RIGHT("right", "RIGHT / CORRIDOR", 0.88, new Scene[]{Scene.CAM_06, Scene.CAM_05}),
        FRONT("front", "CENTER / FRONT ENTRANCE", 0.50, new Scene[]{Scene.CAM_01});

        private final String state;
        private final String label;
        private final double pan;
        private final Scene[] route;

        /*
         * Retain the approach state key, readable label, normalized pan and camera route. Each enum
         * entry supplies its own fixed metadata for the same encounter algorithm.
         * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
         */
        Approach(String state, String label, double pan, Scene[] route) {
            this.state = state;
            this.label = label;
            this.pan = pan;
            this.route = route;
        }

        /*
         * Read the readable UI label. This accessor does not advance time, save data or change the
         * model; callers use it to draw or coordinate behavior.
         * Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
         */
        public String label() { return label; }
        /*
         * Read the normalized direction associated with this value. This accessor does not advance
         * time, save data or change the model; callers use it to draw or coordinate behavior.
         * Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
         */
        public double pan() { return pan; }
        /*
         * Append -occupied to the approach state: left-occupied, right-occupied or front-occupied.
         * These keys match the office manifest entries.
         * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
         */
        public String officeState() { return state + "-occupied"; }
        // The flashlight is effective only inside the doorway's normalized viewing range. | Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
        /*
         * Use an approach-specific normalized viewing range: LEFT pan<=0.30, RIGHT pan>=0.70, FRONT
         * within 0.22 of 0.50. Correct direction is required as well as holding the light.
         * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
         */
        public boolean faces(double officePan) {
            return this == LEFT ? officePan <= 0.30 : this == RIGHT ? officePan >= 0.70 : Math.abs(officePan - 0.5) <= 0.22;
        }
    }

    private final int night;
    private Approach approach = Approach.LEFT;
    // -1 is the waiting period before the first camera on the current route. | Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    private int routeIndex = -1;
    private int repelled;
    // Countdown to the next route step or attack; exposure accumulates successful flashlight time. | Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    private double timer;
    private double exposure;
    private boolean atOffice;
    private boolean caught;

    /*
     * Store the difficulty night and begin the first wait at 18 - (night-1)*2 seconds. Initial
     * approach is LEFT and routeIndex=-1 means no occupied room yet.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    GudoEncounter(int night) {
        this.night = night;
        timer = 18 - (night - 1) * 2;
    }

    /** Split at state boundaries so a long frame cannot skip an attack or a retreat. Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33). */
    /*
     * For an illuminated office encounter return the earlier of remaining attack time and exposure
     * needed to repel; otherwise return timer. GameSession uses this to split its frame at the exact
     * next state boundary.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    double untilEvent(boolean illuminated) {
        return atOffice && illuminated ? Math.min(timer, Math.max(0, flashSeconds() - exposure)) : timer;
    }

    // GameSession supplies steps ending at the next event. No independent wall clock is used here. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    /*
     * Subtract the supplied event-bounded step from timer. At the office, add effective exposure or
     * decay it at 0.65 per second; enough exposure repels, resets travel and cycles approach,
     * otherwise an expired timer catches. During travel, an expired timer enters the next camera or
     * the office. Small epsilons handle floating-point rounding.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    void advance(double seconds, boolean illuminated) {
        timer = Math.max(0, timer - seconds);
        if (atOffice) {
            // Looking away/releasing the light lets Gudo recover at 65% of the exposure rate. | Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            exposure = illuminated ? Math.min(flashSeconds(), exposure + seconds) : Math.max(0, exposure - seconds * 0.65);
            // The small tolerance avoids missing a deadline due to floating-point rounding. | Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            if (exposure >= flashSeconds() - 1e-9) {
                atOffice = false;
                routeIndex = -1;
                exposure = 0;
                repelled++;
                // Successful defenses cycle LEFT -> RIGHT -> FRONT -> LEFT, followed by a new wait. | Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
                approach = Approach.values()[repelled % Approach.values().length];
                timer = Math.max(5, 13 - (night - 1) - Math.min(4, repelled));
            } else if (timer <= 1e-9) caught = true;
        } else if (timer <= 1e-9) {
            // Move through camera positions, then start the office's defense countdown. | Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
            routeIndex++;
            if (routeIndex == approach.route.length) {
                atOffice = true;
                exposure = 0;
                timer = attackSeconds();
            } else timer = Math.max(4, 11 - (night - 1) * 1.3 - Math.min(3, repelled * 0.3));
        }
    }

    // Null means Gudo is between encounters and should not appear on any room render. | Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    /*
     * Return OFFICE during an office encounter, null during the wait between encounters, or the
     * current route camera. A single location makes the previous room clean automatically.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public Scene location() { return atOffice ? Scene.OFFICE : routeIndex < 0 ? null : approach.route[routeIndex]; }
    /*
     * Read the current LEFT/RIGHT/FRONT approach. This accessor does not advance time, save data or
     * change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public Approach approach() { return approach; }
    /*
     * Read whether the enemy is currently in an office defense encounter. This accessor does not
     * advance time, save data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean atOffice() { return atOffice; }
    /*
     * Read whether the attack deadline has expired. This accessor does not advance time, save data
     * or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 8: clock and power](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-8); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public boolean caught() { return caught; }
    /*
     * Read the number of successful flashlight defenses. This accessor does not advance time, save
     * data or change the model; callers use it to draw or coordinate behavior.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public int repelled() { return repelled; }
    // Higher nights require longer flashlight exposure and allow less time before an attack. | Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
    /*
     * Require 1.6 + (night-1)*0.2 seconds of effective light: 1.6 on Night 1 through 2.4 on Night 5.
     * Interrupted light reduces accumulated exposure.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double flashSeconds() { return 1.6 + (night - 1) * 0.2; }
    /*
     * Allow 9 - (night-1)*0.8 seconds at the office: 9.0 on Night 1 through 5.8 on Night 5. This
     * timer continues during ineffective or interrupted defense.
     * Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double attackSeconds() { return 9 - (night - 1) * 0.8; }
    /*
     * While at the office, return exposure/required exposure; otherwise return zero. GamePanel uses
     * this fraction to blend the occupied room toward the clean room.
     * Reviewer (42-page edition): [p. 9: Gudo and effective light](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-9); [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double fadeProgress() { return atOffice ? exposure / flashSeconds() : 0; }
    /*
     * Expose timer only for the office encounter. GamePanel uses <=3 seconds to emit one
     * urgency/rush cue; zero outside the office is not an active attack.
     * Reviewer (42-page edition): [p. 33: GudoEncounter](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-33).
     */
    public double attackRemaining() { return atOffice ? timer : 0; }
}
