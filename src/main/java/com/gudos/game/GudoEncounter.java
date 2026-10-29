package com.gudos.game;

/** One enemy, three connected approaches. All timers use the active shift clock. */
public final class GudoEncounter {
    // Each approach defines the CCTV route followed before reaching its office doorway.
    public enum Approach {
        LEFT("left", "LEFT / SERVICE DOOR", 0.12, new Scene[]{Scene.CAM_04, Scene.CAM_03, Scene.CAM_02}),
        RIGHT("right", "RIGHT / CORRIDOR", 0.88, new Scene[]{Scene.CAM_06, Scene.CAM_05}),
        FRONT("front", "CENTER / DINING ROOM", 0.50, new Scene[]{Scene.CAM_01});

        private final String state;
        private final String label;
        private final double pan;
        private final Scene[] route;

        Approach(String state, String label, double pan, Scene[] route) {
            this.state = state;
            this.label = label;
            this.pan = pan;
            this.route = route;
        }

        public String label() { return label; }
        public double pan() { return pan; }
        public String officeState() { return state + "-occupied"; }
        // The flashlight is effective only inside the doorway's normalized viewing range.
        public boolean faces(double officePan) {
            return this == LEFT ? officePan <= 0.30 : this == RIGHT ? officePan >= 0.70 : Math.abs(officePan - 0.5) <= 0.22;
        }
    }

    private final int night;
    private Approach approach = Approach.LEFT;
    // -1 is the waiting period before the first camera on the current route.
    private int routeIndex = -1;
    private int repelled;
    // Countdown to the next route step or attack; exposure accumulates successful flashlight time.
    private double timer;
    private double exposure;
    private boolean atOffice;
    private boolean caught;

    GudoEncounter(int night) {
        this.night = night;
        timer = 18 - (night - 1) * 2;
    }

    /** Split at state boundaries so a long frame cannot skip an attack or a retreat. */
    double untilEvent(boolean illuminated) {
        return atOffice && illuminated ? Math.min(timer, Math.max(0, flashSeconds() - exposure)) : timer;
    }

    // GameSession supplies steps ending at the next event. No independent wall clock is used here.
    void advance(double seconds, boolean illuminated) {
        timer = Math.max(0, timer - seconds);
        if (atOffice) {
            // Looking away/releasing the light lets Gudo recover at 65% of the exposure rate.
            exposure = illuminated ? Math.min(flashSeconds(), exposure + seconds) : Math.max(0, exposure - seconds * 0.65);
            // The small tolerance avoids missing a deadline due to floating-point rounding.
            if (exposure >= flashSeconds() - 1e-9) {
                atOffice = false;
                routeIndex = -1;
                exposure = 0;
                repelled++;
                // Successful defenses cycle LEFT -> RIGHT -> FRONT -> LEFT, followed by a new wait.
                approach = Approach.values()[repelled % Approach.values().length];
                timer = Math.max(5, 13 - (night - 1) - Math.min(4, repelled));
            } else if (timer <= 1e-9) caught = true;
        } else if (timer <= 1e-9) {
            // Move through camera positions, then start the office's defense countdown.
            routeIndex++;
            if (routeIndex == approach.route.length) {
                atOffice = true;
                exposure = 0;
                timer = attackSeconds();
            } else timer = Math.max(4, 11 - (night - 1) * 1.3 - Math.min(3, repelled * 0.3));
        }
    }

    // Null means Gudo is between encounters and should not appear on any room render.
    public Scene location() { return atOffice ? Scene.OFFICE : routeIndex < 0 ? null : approach.route[routeIndex]; }
    public Approach approach() { return approach; }
    public boolean atOffice() { return atOffice; }
    public boolean caught() { return caught; }
    public int repelled() { return repelled; }
    // Higher nights require longer flashlight exposure and allow less time before an attack.
    public double flashSeconds() { return 1.6 + (night - 1) * 0.2; }
    public double attackSeconds() { return 9 - (night - 1) * 0.8; }
    public double fadeProgress() { return atOffice ? exposure / flashSeconds() : 0; }
    public double attackRemaining() { return atOffice ? timer : 0; }
}
