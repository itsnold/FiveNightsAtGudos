package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.util.List;

/** Level configuration produced by the builder, independent of gameplay and Swing rendering. Reviewer (42-page edition): [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32). */
/*
 * SecretLevel
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
 * Reviewer (42-page edition): [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
 */
public record SecretLevel(double playerX, double bundleX, Rectangle destination,
                          List<Rectangle> platforms, List<Rectangle> cover,
                          List<Patrol> patrols, double seconds, int width) {
    // Patrol endpoints and all rectangles use world coordinates, not the scrolling screen position. | Reviewer (42-page edition): [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
    /*
     * Patrol
     *
     * Immutable left/right world-coordinate endpoints for one witness patrol.
     * Reviewer (42-page edition): [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     */
    public record Patrol(double left, double right) { }
    /*
     * Copy destination and every platform/cover Rectangle, and copy the patrol list during
     * construction. This protects against later mutation of input objects; returned Rectangle
     * accessors still expose mutable objects and should be treated read-only.
     * Reviewer (42-page edition): [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
     */
    public SecretLevel {
        // Copy construction inputs so later changes to the builder's rectangles do not change the level. | Reviewer (42-page edition): [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
        // Rectangle itself is mutable, so callers should treat the returned configuration as read-only. | Reviewer (42-page edition): [p. 15: Builder and OOP](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-15); [p. 32: SecretLevel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-32).
        destination = new Rectangle(destination);
        platforms = platforms.stream().map(Rectangle::new).toList();
        cover = cover.stream().map(Rectangle::new).toList();
        patrols = List.copyOf(patrols);
    }
}
