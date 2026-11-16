package com.gudos.assets;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Rectilinear window onto a cylindrical panorama, not a sliding flat crop.
 * atan maps horizontal viewing rays; cos shortens their vertical source span.
 * The image bends at the screen edges while yaw reveals either side of the room.
 * Only the room is projected: HUD/text never pass through this transformation.
 * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
 */
/*
 * PanoramaRenderer
 *
 * Purpose: Projects a wide room image into a curved-looking view and maps clicks back to source
 * pixels.
 *
 * Execution flow: render clamps/quantizes pan, prepares sampling maps when the source changes,
 * fills a reused 854x480 image, and returns it. sourcePoint reverses the latest sampling for a hit
 * test.
 *
 * Design rationale: atan maps horizontal rays and cos changes vertical sampling
 * near the edges. Cached pixel maps and one output buffer reduce allocation. A separate renderer is
 * needed when two outputs must coexist for a blend.
 *
 * State: output/pixels are reused; sourceRows and sourceColumns locate input samples;
 * lastSource/lastYaw skip unchanged work. HALF_FOV is 40 degrees, so the horizontal field is 80
 * degrees.
 *
 * Summary: This is a Java2D image projection, not a 3D world or a plain sliding crop. Only
 * the room is transformed; HUD text stays flat.
 * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
 */
public final class PanoramaRenderer {
    public static final int WIDTH = 854;
    public static final int HEIGHT = 480;
    private static final double HALF_FOV = Math.toRadians(40);
    // Reuse this renderer's output and lookup arrays instead of allocating a frame on every tick. | Reviewer (42-page edition): [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
    private final BufferedImage output = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
    private final double[] columnOffsets = new double[WIDTH];
    private final int[] sourceColumns = new int[WIDTH];
    private final int[] sourceRows = new int[WIDTH * HEIGHT];
    private BufferedImage lastSource;
    private int[] sourcePixels;
    private double span;
    private int lastYaw = Integer.MIN_VALUE;

    /**
     * Source pixel displayed at a projected output position after render(). The paper's hitbox
     * lives in the source image, so this reverse lookup keeps clicks aligned during camera sweeps.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    /*
     * For a valid projected coordinate after render, return the source image column and row used for
     * that output pixel. Return null if there is no prepared source or the point is outside 854x480.
     * This keeps the CAM 04 paper hitbox attached to the moving image.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public java.awt.Point sourcePoint(int x, int y) {
        if (lastSource == null || x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return null;
        return new java.awt.Point(sourceColumns[x], sourceRows[y * WIDTH + x] / lastSource.getWidth());
    }

    /*
     * Reject null/nonfinite inputs; clamp pan and quantize to 0..2048. Reuse output when source/yaw
     * are unchanged; prepare new lookup data when source identity changes. Compute columns for
     * current yaw, sample the source into the shared output pixel array, remember yaw and return the
     * same mutable output image.
     * Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public BufferedImage render(BufferedImage source, double pan) {
        if (source == null || !Double.isFinite(pan)) throw new IllegalArgumentException("Invalid panorama");
        // Quantize the view to 2049 positions; unchanged source/view pairs can reuse the last output. | Reviewer (42-page edition): [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
        int yaw = (int) Math.round(Math.max(0, Math.min(1, pan)) * 2048);
        if (source == lastSource && yaw == lastYaw) return output;
        if (source != lastSource) prepare(source);
        double center = span / 2 + (source.getWidth() - span) * (yaw / 2048.0);
        for (int x = 0; x < WIDTH; x++) sourceColumns[x] = Math.max(0, Math.min(source.getWidth() - 1, (int) (center + columnOffsets[x])));
        // Each output pixel samples a precomputed source row and the current view's source column. | Reviewer (42-page edition): [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
        for (int y = 0, index = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++, index++) pixels[index] = sourcePixels[sourceRows[index] + sourceColumns[x]];
        }
        lastYaw = yaw;
        return output;
    }

    // Precompute the curved projection whenever the source image changes. | Reviewer (42-page edition): [p. 10: input and projection](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-10); [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
    // Horizontal rays use atan; cos changes the vertical span toward the sides of the view. | Reviewer (42-page edition): [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
    /*
     * Cache source pixels and construct curved sampling lookups for this source image. atan converts
     * screen column to ray angle; cos varies vertical span. Clamp every sample into the source
     * bounds. This work is reused until the source image changes.
     * Reviewer (42-page edition): [p. 28: PanoramaRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
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
