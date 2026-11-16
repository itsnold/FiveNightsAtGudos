package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern: collects the pieces needed for a SecretLevel before creating it.
 * Application.createSecretLevel() is the client that chooses the parts; SecretLevel
 * is the product, and SecretGame later uses that product to run the simulation.
 * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
 * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (chains the production build steps);
 * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built product);
 * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (uses the built level).
 */
/*
 * SecretLevelBuilder
 *
 * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (chains the production build steps);
 * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built product);
 * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (uses the built level).
 *
 * Purpose: Constructs a SecretLevel step by step through a fluent Builder.
 *
 * Execution flow: Application.createSecretLevel chains
 * width/player/bundle/destination/platform/cover/witness/timeLimit, then build.
 *
 * Design rationale: Optional repeated parts are easier to read than a large
 * positional constructor. Each setter returns this; build validates completeness and creates the
 * product. Validation covers selected constraints, not every possible numeric input.
 *
 * State: Default width=1280 and seconds=60. Lists accumulate rectangles/patrols;
 * destination is required and at least one valid patrol is required.
 *
 * Summary: Builder is creational. The builder collects parts; build creates SecretLevel. This
 * is separate from the structural facade and decorator examples.
 * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
 */
public final class SecretLevelBuilder {
    // Defaults apply only when the client does not explicitly configure these values. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built product); [Application.java:438 (createSecretLevel)](../../Application.java#L438) (chains the production build steps).
    private double playerX, bundleX, seconds = 60;
    private int width = 1280;
    private Rectangle destination;
    private final List<Rectangle> platforms = new ArrayList<>(), cover = new ArrayList<>();
    private final List<SecretLevel.Patrol> patrols = new ArrayList<>();

    // Each method returns this builder so the client can chain construction steps. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller); [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration).
    // Rectangle copies prevent later changes to the caller's objects from altering the builder. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller); [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration).
    /*
     * Store the player spawn x-coordinate and return this same builder for chaining. No running
     * SecretGame is changed.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder player(double x) { playerX = x; return this; }
    /*
     * Store the bundle spawn x-coordinate and return this builder. The countdown starts later in
     * SecretGame.action, not during construction.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder bundle(double x) { bundleX = x; return this; }
    /*
     * Copy the supplied Rectangle into the builder and return this. The copy prevents the caller
     * from moving the destination by mutating its original input.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder destination(Rectangle r) { destination = new Rectangle(r); return this; }
    /*
     * Append a copied platform Rectangle and return this. Repeated calls add parts rather than
     * replacing previous platforms.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder platform(Rectangle r) { platforms.add(new Rectangle(r)); return this; }
    /*
     * Append a copied hiding Rectangle and return this. SecretGame later tests the bundle center
     * against these world-coordinate regions.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder cover(Rectangle r) { cover.add(new Rectangle(r)); return this; }
    // Calling witness repeatedly adds independent patrols instead of replacing the previous one. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller); [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration).
    /*
     * Append one Patrol(from,to) and return this. Three calls in Application create three
     * independently moving runtime witnesses.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder witness(double from, double to) { patrols.add(new SecretLevel.Patrol(from, to)); return this; }
    /*
     * Store the countdown seconds and return this. The production client sets 60; the timer does not
     * run until the first bundle pickup.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder timeLimit(double value) { seconds = value; return this; }
    /*
     * Store world width and return this. A 2560-wide production level scrolls beyond the 1280-wide
     * logical viewport.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevelBuilder width(int value) { width = value; return this; }
    // Reject an incomplete level before it reaches the game; construct the product only here. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller); [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration).
    /*
     * Reject a missing destination, absent/invalid patrols, nonpositive time, too-small world, or a
     * destination extending past the right edge. Construct SecretLevel only after these checks. It
     * does not comprehensively validate NaN, all rectangle dimensions or all spawn bounds.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevelBuilder](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production builder-chain caller);
     * [game/minigame/SecretLevel.java:31 (SecretLevel)](SecretLevel.java#L31) (built configuration);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the built level).
     */
    public SecretLevel build() {
        if (destination == null || patrols.isEmpty() || patrols.stream().anyMatch(p -> p.left() < 0 || p.right() <= p.left() || p.right() > width) || seconds <= 0 || width < 1280 || destination.x + destination.width > width) throw new IllegalStateException("Incomplete secret level");
        return new SecretLevel(playerX, bundleX, destination, platforms, cover, patrols, seconds, width);
    }
}
