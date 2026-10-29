package com.gudos.assets;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Rectilinear window onto a cylindrical panorama, not a sliding flat crop.
 * atan maps horizontal viewing rays; cos shortens their vertical source span.
 * The image bends at the screen edges while yaw reveals either side of the room.
 * Only the room is projected: HUD/text never pass through this transformation.
 */
public final class PanoramaRenderer {
    public static final int WIDTH = 854;
    public static final int HEIGHT = 480;
    private static final double HALF_FOV = Math.toRadians(40);
    // Reuse this renderer's output and lookup arrays instead of allocating a frame on every tick.
    private final BufferedImage output = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
    private final double[] columnOffsets = new double[WIDTH];
    private final int[] sourceColumns = new int[WIDTH];
    private final int[] sourceRows = new int[WIDTH * HEIGHT];
    private BufferedImage lastSource;
    private int[] sourcePixels;
    private double span;
    private int lastYaw = Integer.MIN_VALUE;

    public BufferedImage render(BufferedImage source, double pan) {
        if (source == null || !Double.isFinite(pan)) throw new IllegalArgumentException("Invalid panorama");
        // Quantize the view to 2049 positions; unchanged source/view pairs can reuse the last output.
        int yaw = (int) Math.round(Math.max(0, Math.min(1, pan)) * 2048);
        if (source == lastSource && yaw == lastYaw) return output;
        if (source != lastSource) prepare(source);
        double center = span / 2 + (source.getWidth() - span) * (yaw / 2048.0);
        for (int x = 0; x < WIDTH; x++) sourceColumns[x] = Math.max(0, Math.min(source.getWidth() - 1, (int) (center + columnOffsets[x])));
        // Each output pixel samples a precomputed source row and the current view's source column.
        for (int y = 0, index = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++, index++) pixels[index] = sourcePixels[sourceRows[index] + sourceColumns[x]];
        }
        lastYaw = yaw;
        return output;
    }

    // Precompute the curved projection whenever the source image changes.
    // Horizontal rays use atan; cos changes the vertical span toward the sides of the view.
    private void prepare(BufferedImage source) {
        lastSource = source;
        lastYaw = Integer.MIN_VALUE;
        int width = source.getWidth();
        int height = source.getHeight();
        sourcePixels = source.getRGB(0, 0, width, height, null, 0, width);
        span = Math.min(width * 0.88, height * (16.0 / 9) * 1.02);
        double focal = WIDTH / 2.0 / Math.tan(HALF_FOV);
        for (int x = 0; x < WIDTH; x++) {
            double angle = Math.atan((x + 0.5 - WIDTH / 2.0) / focal);
            columnOffsets[x] = angle / HALF_FOV * span / 2;
            double verticalScale = Math.cos(angle) * 0.985;
            for (int y = 0; y < HEIGHT; y++) {
                int sample = (int) (height / 2.0 + (y + 0.5 - HEIGHT / 2.0) * height / HEIGHT * verticalScale);
                sourceRows[y * WIDTH + x] = Math.max(0, Math.min(height - 1, sample)) * width;
            }
        }
    }
}
