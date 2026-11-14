package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.util.List;

/** Level configuration produced by the builder, independent of gameplay and Swing rendering. */
public record SecretLevel(double playerX, double bundleX, Rectangle destination,
                          List<Rectangle> platforms, List<Rectangle> cover,
                          List<Patrol> patrols, double seconds, int width) {
    // Patrol endpoints and all rectangles use world coordinates, not the scrolling screen position.
    public record Patrol(double left, double right) { }
    public SecretLevel {
        // Copy construction inputs so later changes to the builder's rectangles do not change the level.
        // Rectangle itself is mutable, so callers should treat the returned configuration as read-only.
        destination = new Rectangle(destination);
        platforms = platforms.stream().map(Rectangle::new).toList();
        cover = cover.stream().map(Rectangle::new).toList();
        patrols = List.copyOf(patrols);
    }
}
