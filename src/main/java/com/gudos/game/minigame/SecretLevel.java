package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.util.List;

/** Level configuration produced by the builder, independent of gameplay and Swing rendering. [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). Related: [game/minigame/SecretLevelBuilder.java:134 (SecretLevel call)](SecretLevelBuilder.java#L134) (caller of SecretLevel); [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production level recipe). */
/*
 * SecretLevel
 *
 * Related: [game/minigame/SecretLevelBuilder.java:132 (build)](SecretLevelBuilder.java#L132) (constructs this configuration);
 * [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production level recipe);
 * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the configuration);
 * [ui/SecretPanel.java:315 (drawWorld)](../../ui/SecretPanel.java#L315) (draws configured level geometry).
 *
 * Purpose: Stores the level configuration produced by SecretLevelBuilder.
 *
 * Execution flow: Application creates the product; SecretGame reads dimensions, spawn points,
 * cover, platforms, destination and patrols.
 *
 * Design rationale: The constructor copies incoming Rectangle objects and lists, so
 * later changes to builder inputs do not change this product. Returned Rectangles remain mutable;
 * this is not deep immutability after access.
 *
 * State: playerX, bundleX, destination, platforms, cover, patrols, seconds and width.
 * Nested Patrol holds left/right world-coordinate endpoints.
 *
 * Summary: SecretLevel is configuration, SecretGame is changing runtime state, and
 * SecretPanel is the view/controller.
 * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
 */
public record SecretLevel(double playerX, double bundleX, Rectangle destination,
                          List<Rectangle> platforms, List<Rectangle> cover,
                          List<Patrol> patrols, double seconds, int width) {
    // Patrol endpoints and all rectangles use world coordinates, not the scrolling screen position. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [game/minigame/SecretLevelBuilder.java:103 (Patrol call)](SecretLevelBuilder.java#L103) (caller of Patrol); [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production level recipe).
    /*
     * Patrol
     *
     * Immutable left/right world-coordinate endpoints for one witness patrol.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [game/minigame/SecretLevelBuilder.java:103 (Patrol call)](SecretLevelBuilder.java#L103) (caller of Patrol);
     * [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production level recipe);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the configuration).
     */
    public record Patrol(double left, double right) { }
    /*
     * Copy destination and every platform/cover Rectangle, and copy the patrol list during
     * construction. This protects against later mutation of input objects; returned Rectangle
     * accessors still expose mutable objects and should be treated read-only.
     * [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     * Related: [game/minigame/SecretLevelBuilder.java:134 (SecretLevel call)](SecretLevelBuilder.java#L134) (caller of SecretLevel);
     * [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production level recipe);
     * [game/minigame/SecretGame.java:98 (SecretGame)](SecretGame.java#L98) (consumes the configuration).
     */
    public SecretLevel {
        // Copy construction inputs so later changes to the builder's rectangles do not change the level. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [game/minigame/SecretLevelBuilder.java:134 (SecretLevel call)](SecretLevelBuilder.java#L134) (caller of SecretLevel); [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production level recipe).
        // Rectangle itself is mutable, so callers should treat the returned configuration as read-only. | [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). | Related: [game/minigame/SecretLevelBuilder.java:134 (SecretLevel call)](SecretLevelBuilder.java#L134) (caller of SecretLevel); [Application.java:438 (createSecretLevel)](../../Application.java#L438) (production level recipe).
        destination = new Rectangle(destination);
        platforms = platforms.stream().map(Rectangle::new).toList();
        cover = cover.stream().map(Rectangle::new).toList();
        patrols = List.copyOf(patrols);
    }
}
