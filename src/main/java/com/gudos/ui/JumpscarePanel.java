package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Scene;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/** A generated still animated with a lunge, shake and cached directional blur. */
public final class JumpscarePanel extends ScenePanel {
    public static final double DURATION = 2.1;
    private final BufferedImage still;
    private final BufferedImage blurred;
    private final boolean reducedEffects;
    private final Runnable complete;
    private boolean delivered;

    public JumpscarePanel(AssetManager assets, boolean reducedEffects, Runnable complete) {
        this.reducedEffects = reducedEffects;
        this.complete = complete;
        still = assets.scene(Scene.OFFICE, "jumpscare");
        BufferedImage frame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.drawImage(still, 0, 0, WIDTH, HEIGHT, null);
        g.dispose();
        float[] streak = {0.06f, 0.09f, 0.12f, 0.15f, 0.16f, 0.15f, 0.12f, 0.09f, 0.06f};
        blurred = new ConvolveOp(new Kernel(9, 1, streak), ConvolveOp.EDGE_NO_OP, null).filter(frame, null);
    }

    @Override public void tick(double seconds) {
        super.tick(seconds);
        if (age >= DURATION && !delivered) {
            delivered = true;
            complete.run();
        }
    }

    @Override protected void render(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        double zoom = reducedEffects ? 1 : 1.06 + Math.min(0.14, age * 0.18);
        int w = (int) Math.ceil(WIDTH * zoom);
        int h = (int) Math.ceil(HEIGHT * zoom);
        int shakeX = reducedEffects ? 0 : (int) (Math.sin(age * 109) * 18 + Math.cos(age * 73) * 8);
        int shakeY = reducedEffects ? 0 : (int) (Math.cos(age * 97) * 13);
        int x = (WIDTH - w) / 2 + shakeX;
        int y = (HEIGHT - h) / 2 + shakeY;
        g.drawImage(still, x, y, w, h, null);
        if (!reducedEffects) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.42f));
            g.drawImage(blurred, x + (int) (Math.sin(age * 89) * 18), y, w, h, null);
            g.setComposite(AlphaComposite.SrcOver);
        }
        // A single fade to black, never a flashing sequence.
        if (age > DURATION - 0.35) {
            g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, (age - DURATION + 0.35) / 0.35))));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }
}
