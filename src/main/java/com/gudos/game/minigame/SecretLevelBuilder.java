package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern: collects the pieces needed for a SecretLevel before creating it.
 * Application.createSecretLevel() is the client that chooses the parts; SecretLevel
 * is the product, and SecretGame later uses that product to run the simulation.
 */
public final class SecretLevelBuilder {
    // Defaults apply only when the client does not explicitly configure these values.
    private double playerX, bundleX, seconds = 60;
    private int width = 1280;
    private Rectangle destination;
    private final List<Rectangle> platforms = new ArrayList<>(), cover = new ArrayList<>();
    private final List<SecretLevel.Patrol> patrols = new ArrayList<>();

    // Each method returns this builder so the client can chain construction steps.
    // Rectangle copies prevent later changes to the caller's objects from altering the builder.
    public SecretLevelBuilder player(double x) { playerX = x; return this; }
    public SecretLevelBuilder bundle(double x) { bundleX = x; return this; }
    public SecretLevelBuilder destination(Rectangle r) { destination = new Rectangle(r); return this; }
    public SecretLevelBuilder platform(Rectangle r) { platforms.add(new Rectangle(r)); return this; }
    public SecretLevelBuilder cover(Rectangle r) { cover.add(new Rectangle(r)); return this; }
    // Calling witness repeatedly adds independent patrols instead of replacing the previous one.
    public SecretLevelBuilder witness(double from, double to) { patrols.add(new SecretLevel.Patrol(from, to)); return this; }
    public SecretLevelBuilder timeLimit(double value) { seconds = value; return this; }
    public SecretLevelBuilder width(int value) { width = value; return this; }
    // Reject an incomplete level before it reaches the game; construct the product only here.
    public SecretLevel build() {
        if (destination == null || patrols.isEmpty() || patrols.stream().anyMatch(p -> p.left() < 0 || p.right() <= p.left() || p.right() > width) || seconds <= 0 || width < 1280 || destination.x + destination.width > width) throw new IllegalStateException("Incomplete secret level");
        return new SecretLevel(playerX, bundleX, destination, platforms, cover, patrols, seconds, width);
    }
}
