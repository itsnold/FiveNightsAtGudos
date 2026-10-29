package com.gudos.ui;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Shared drawing helpers, colors, and bundled font used to keep all screens visually consistent. */
public final class Theme {
    public static final Color BACKGROUND = Color.BLACK;
    public static final Color PAPER = Color.WHITE;
    public static final Color MUTED = new Color(180, 180, 180);
    public static final Color AMBER = Color.WHITE;
    public static final Color GREEN = new Color(43, 226, 18);
    public static final Color RED = new Color(241, 24, 24);
    public static final Color LINE = new Color(255, 255, 255, 115);
    private static final Font PIXEL = loadPixelFont();

    private Theme() { }

    // Load from the classpath so packaged/offline runs use the same font as development runs.
    private static Font loadPixelFont() {
        try (InputStream stream = Theme.class.getResourceAsStream("/fonts/VT323-Regular.ttf")) {
            if (stream == null) throw new IllegalStateException("Missing bundled VT323 font");
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception error) {
            throw new IllegalStateException("Could not load the bundled pixel font", error);
        }
    }

    public static Font pixel(int size) { return PIXEL.deriveFont((float) size); }
    public static Font sans(int size) { return PIXEL.deriveFont(size * 1.25f); }
    public static Font heading(int size) { return pixel(size); }
    public static Font mono(int size) { return PIXEL.deriveFont(size * 1.25f); }

    // Keep pixel text sharp while using bilinear sampling for scaled room images.
    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // Draw a small black shadow behind opaque light text to improve readability over backgrounds.
    public static void text(Graphics2D g, String text, int x, int y, Font font, Color color) {
        g.setFont(font);
        if (color.getAlpha() > 200 && !color.equals(BACKGROUND)) {
            g.setColor(Color.BLACK);
            g.drawString(text, x + 2, y + 2);
        }
        g.setColor(color);
        g.drawString(text, x, y);
    }

    // Increase character spacing for small section labels.
    public static void tracked(Graphics2D g, String text, int x, int y, int size, Color color) {
        g.setFont(mono(size));
        g.setColor(color);
        float cursor = x;
        for (char character : text.toCharArray()) {
            String value = String.valueOf(character);
            g.drawString(value, cursor, y);
            cursor += g.getFontMetrics().stringWidth(value) + 1.8f;
        }
    }

    // Java2D drawString does not wrap text, so break paragraphs into lines using font measurements.
    public static void paragraph(Graphics2D g, String text, int x, int y, int width, int lineHeight, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        int baseline = y;
        for (String line : text.split("\n", -1)) {
            StringBuilder current = new StringBuilder();
            for (String word : line.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (g.getFontMetrics().stringWidth(candidate) > width && !current.isEmpty()) {
                    g.drawString(current.toString(), x, baseline);
                    baseline += lineHeight;
                    current = new StringBuilder(word);
                } else current = new StringBuilder(candidate);
            }
            g.drawString(current.toString(), x, baseline);
            baseline += lineHeight;
        }
    }

    // Shorten text to fit its allocated width while indicating that characters were omitted.
    public static String ellipsize(Graphics2D g, String text, int width) {
        if (g.getFontMetrics().stringWidth(text) <= width) return text;
        int length = text.length();
        while (length > 0 && g.getFontMetrics().stringWidth(text.substring(0, length) + "…") > width) length--;
        return text.substring(0, length) + "…";
    }

    // Fill a rectangle completely, cropping overflow; pan chooses which horizontal part remains visible.
    public static void cover(Graphics2D g, BufferedImage image, int x, int y, int w, int h, double pan) {
        double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
        int width = (int) Math.ceil(image.getWidth() * scale);
        int height = (int) Math.ceil(image.getHeight() * scale);
        Shape clip = g.getClip();
        g.clipRect(x, y, w, h);
        g.drawImage(image, x - (int) ((width - w) * Math.max(0, Math.min(1, pan))), y - (height - h) / 2, width, height, null);
        g.setClip(clip);
    }

    // Fit the entire image inside a rectangle without cropping, preserving its aspect ratio.
    public static void contain(Graphics2D g, BufferedImage image, int x, int y, int w, int h) {
        double scale = Math.min((double) w / image.getWidth(), (double) h / image.getHeight());
        int width = (int) Math.round(image.getWidth() * scale);
        int height = (int) Math.round(image.getHeight() * scale);
        g.drawImage(image, x + (w - width) / 2, y + (h - height) / 2, width, height, null);
    }

    // A radial gradient darkens the screen edges without modifying the source image.
    public static void vignette(Graphics2D g, int strength) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(640, 340), 810,
                new float[]{0, 0.5f, 1}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 15), new Color(0, 0, 0, strength)}));
        g.fillRect(0, 0, 1280, 720);
    }

    // Small decorative drawing helpers used by screens; they do not manage Swing components.
    public static void cornerFrame(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1));
        int l = 18;
        for (int xx : new int[]{x, x + w}) for (int yy : new int[]{y, y + h}) {
            g.drawLine(xx, yy, xx + (xx == x ? l : -l), yy);
            g.drawLine(xx, yy, xx, yy + (yy == y ? l : -l));
        }
    }

    public static void rule(Graphics2D g, int x, int y, int width) {
        g.setColor(LINE);
        g.drawLine(x, y, x + width, y);
    }
}
