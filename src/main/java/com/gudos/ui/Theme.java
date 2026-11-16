package com.gudos.ui;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Shared drawing helpers, colors, and bundled font used to keep all screens visually consistent. [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality); [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors). */
/*
 * Theme
 *
 * Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality);
 * [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors);
 * [ui/GamePanel.java:597 (renderHud)](GamePanel.java#L597) (text and HUD styling);
 * [ui/InfoPanel.java:68 (renderGuide)](InfoPanel.java#L68) (paragraph layout).
 *
 * Purpose: Centralizes the bundled pixel font, colors and reusable drawing utilities.
 *
 * Execution flow: Panels/buttons/renderers call static helpers instead of reproducing
 * font/image/text code.
 *
 * Design rationale: One style source keeps UI consistent. Explicit wrap/ellipsize
 * handle Java2D text limits. cover crops to fill while contain preserves the entire image.
 *
 * State: PIXEL is loaded from /fonts/VT323-Regular.ttf. Names such as sans/mono still
 * derive the same pixel font; AMBER currently equals white.
 *
 * Summary: This is a static utility class, not a Singleton object or a game engine.
 * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
 */
public final class Theme {
    public static final Color BACKGROUND = Color.BLACK;
    public static final Color PAPER = Color.WHITE;
    public static final Color MUTED = new Color(180, 180, 180);
    public static final Color AMBER = Color.WHITE;
    public static final Color GREEN = new Color(43, 226, 18);
    public static final Color RED = new Color(241, 24, 24);
    public static final Color LINE = new Color(255, 255, 255, 115);
    private static final Font PIXEL = loadPixelFont();

    /*
     * Private constructor prevents utility instances. Static font/drawing helpers provide shared
     * presentation services.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality);
     * [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors);
     * [ui/GamePanel.java:597 (renderHud)](GamePanel.java#L597) (text and HUD styling).
     */
    private Theme() { }

    // Load from the classpath so packaged/offline runs use the same font as development runs. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality); [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors).
    /*
     * Read the bundled VT323 TTF from the classpath and create a Font. Fail clearly if
     * missing/unreadable instead of silently substituting a different layout.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality);
     * [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors);
     * [ui/GamePanel.java:597 (renderHud)](GamePanel.java#L597) (text and HUD styling).
     */
    private static Font loadPixelFont() {
        try (InputStream stream = Theme.class.getResourceAsStream("/fonts/VT323-Regular.ttf")) {
            if (stream == null) throw new IllegalStateException("Missing bundled VT323 font");
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception error) {
            throw new IllegalStateException("Could not load the bundled pixel font", error);
        }
    }

    /*
     * Derive the shared VT323 font at the requested exact point size.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/BackdropPanel.java:71 (pixel call)](BackdropPanel.java#L71) (caller of pixel);
     * [ui/ExtrasPanel.java:45 (pixel call)](ExtrasPanel.java#L45) (caller of pixel);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static Font pixel(int size) { return PIXEL.deriveFont((float) size); }
    /*
     * Derive VT323 at 1.25 times the requested size. Despite the helper name, this is the bundled
     * pixel family, not a separate sans-serif resource.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [Main.java:78 (sans call)](../Main.java#L78) (caller of sans);
     * [ui/BackdropPanel.java:72 (sans call)](BackdropPanel.java#L72) (caller of sans);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static Font sans(int size) { return PIXEL.deriveFont(size * 1.25f); }
    /*
     * Forward to pixel for title font sizing. Shared naming expresses usage rather than introducing
     * another font family.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [Main.java:163 (heading call)](../Main.java#L163) (caller of heading);
     * [ui/RecordsPanel.java:146 (heading call)](RecordsPanel.java#L146) (caller of heading);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static Font heading(int size) { return pixel(size); }
    /*
     * Derive VT323 at 1.25 times the size for monospaced-style UI text. It reuses the same loaded
     * font object family.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/InfoPanel.java:81 (mono call)](InfoPanel.java#L81) (caller of mono);
     * [ui/RecordsPanel.java:78 (mono call)](RecordsPanel.java#L78) (caller of mono);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static Font mono(int size) { return PIXEL.deriveFont(size * 1.25f); }

    // Keep pixel text sharp while using bilinear sampling for scaled room images. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [Main.java:158 (quality call)](../Main.java#L158) (caller of quality); [ui/GameButton.java:81 (quality call)](GameButton.java#L81) (caller of quality).
    /*
     * Set Java2D hints: crisp un-antialiased pixel text/shapes, bilinear room image interpolation
     * and render quality. Individual retro views can override sampling locally.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [Main.java:158 (quality call)](../Main.java#L158) (caller of quality);
     * [ui/GameButton.java:81 (quality call)](GameButton.java#L81) (caller of quality);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // Draw a small black shadow behind opaque light text to improve readability over backgrounds. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [Main.java:163 (text call)](../Main.java#L163) (caller of text); [ui/BackdropPanel.java:71 (text call)](BackdropPanel.java#L71) (caller of text).
    /*
     * Set font, optionally draw a small black shadow for opaque light text, then draw foreground
     * text at its baseline. Coordinates are logical scene units.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [Main.java:163 (text call)](../Main.java#L163) (caller of text);
     * [ui/BackdropPanel.java:71 (text call)](BackdropPanel.java#L71) (caller of text);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static void text(Graphics2D g, String text, int x, int y, Font font, Color color) {
        g.setFont(font);
        if (color.getAlpha() > 200 && !color.equals(BACKGROUND)) {
            g.setColor(Color.BLACK);
            g.drawString(text, x + 2, y + 2);
        }
        g.setColor(color);
        g.drawString(text, x, y);
    }

    // Increase character spacing for small section labels. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/InfoPanel.java:70 (tracked call)](InfoPanel.java#L70) (caller of tracked); [ui/RecordsPanel.java:139 (tracked call)](RecordsPanel.java#L139) (caller of tracked).
    /*
     * Draw a small label character by character with added spacing based on FontMetrics. It is a
     * presentation helper only.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/InfoPanel.java:70 (tracked call)](InfoPanel.java#L70) (caller of tracked);
     * [ui/RecordsPanel.java:139 (tracked call)](RecordsPanel.java#L139) (caller of tracked);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
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

    // Java2D drawString does not wrap text, so break paragraphs into lines using font measurements. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/InfoPanel.java:96 (paragraph call)](InfoPanel.java#L96) (caller of paragraph); [ui/SettingsPanel.java:287 (paragraph call)](SettingsPanel.java#L287) (caller of paragraph).
    /*
     * Split explicit newline paragraphs, add words until FontMetrics would exceed width, and advance
     * baselines by lineHeight. Java2D drawString has no automatic wrapping.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/InfoPanel.java:96 (paragraph call)](InfoPanel.java#L96) (caller of paragraph);
     * [ui/SettingsPanel.java:287 (paragraph call)](SettingsPanel.java#L287) (caller of paragraph);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
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

    // Shorten text to fit its allocated width while indicating that characters were omitted. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/GameButton.java:138 (ellipsize call)](GameButton.java#L138) (caller of ellipsize); [ui/RecordsPanel.java:141 (ellipsize call)](RecordsPanel.java#L141) (caller of ellipsize).
    /*
     * Measure the whole string; if it does not fit, shorten the suffix until text plus ellipsis
     * fits. This prevents long player names/paths from overrunning allotted space.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/GameButton.java:138 (ellipsize call)](GameButton.java#L138) (caller of ellipsize);
     * [ui/RecordsPanel.java:141 (ellipsize call)](RecordsPanel.java#L141) (caller of ellipsize);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static String ellipsize(Graphics2D g, String text, int width) {
        if (g.getFontMetrics().stringWidth(text) <= width) return text;
        int length = text.length();
        while (length > 0 && g.getFontMetrics().stringWidth(text.substring(0, length) + "…") > width) length--;
        return text.substring(0, length) + "…";
    }

    // Fill a rectangle completely, cropping overflow; pan chooses which horizontal part remains visible. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/BackdropPanel.java:56 (cover call)](BackdropPanel.java#L56) (caller of cover); [ui/MenuPanel.java:190 (cover call)](MenuPanel.java#L190) (caller of cover).
    /*
     * Use max(width scale,height scale) to fill the whole destination, clip overflow and choose
     * horizontal crop by clamped pan. Restore the incoming clip afterward.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/BackdropPanel.java:56 (cover call)](BackdropPanel.java#L56) (caller of cover);
     * [ui/MenuPanel.java:190 (cover call)](MenuPanel.java#L190) (caller of cover);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static void cover(Graphics2D g, BufferedImage image, int x, int y, int w, int h, double pan) {
        double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
        int width = (int) Math.ceil(image.getWidth() * scale);
        int height = (int) Math.ceil(image.getHeight() * scale);
        Shape clip = g.getClip();
        g.clipRect(x, y, w, h);
        g.drawImage(image, x - (int) ((width - w) * Math.max(0, Math.min(1, pan))), y - (height - h) / 2, width, height, null);
        g.setClip(clip);
    }

    // Fit the entire image inside a rectangle without cropping, preserving its aspect ratio. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality); [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors).
    /*
     * Use min(width scale,height scale) to fit the entire image centered without cropping. Empty
     * borders may remain.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality);
     * [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors);
     * [ui/GamePanel.java:597 (renderHud)](GamePanel.java#L597) (text and HUD styling).
     */
    public static void contain(Graphics2D g, BufferedImage image, int x, int y, int w, int h) {
        double scale = Math.min((double) w / image.getWidth(), (double) h / image.getHeight());
        int width = (int) Math.round(image.getWidth() * scale);
        int height = (int) Math.round(image.getHeight() * scale);
        g.drawImage(image, x + (w - width) / 2, y + (h - height) / 2, width, height, null);
    }

    // A radial gradient darkens the screen edges without modifying the source image. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/GamePanel.java:494 (vignette call)](GamePanel.java#L494) (caller of vignette); [ui/rendering/CameraSceneRenderer.java:72 (vignette call)](rendering/CameraSceneRenderer.java#L72) (caller of vignette).
    /*
     * Draw a radial dark gradient at the logical screen edges without altering the source image
     * pixels.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/GamePanel.java:494 (vignette call)](GamePanel.java#L494) (caller of vignette);
     * [ui/rendering/CameraSceneRenderer.java:72 (vignette call)](rendering/CameraSceneRenderer.java#L72) (caller of vignette);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static void vignette(Graphics2D g, int strength) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(640, 340), 810,
                new float[]{0, 0.5f, 1}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 15), new Color(0, 0, 0, strength)}));
        g.fillRect(0, 0, 1280, 720);
    }

    // Small decorative drawing helpers used by screens; they do not manage Swing components. | [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23). | Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality); [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors).
    /*
     * Draw four small line corners around a rectangle; this utility does not create a Swing
     * border/control.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality);
     * [ui/GameButton.java:79 (paintComponent)](GameButton.java#L79) (shared fonts and colors);
     * [ui/GamePanel.java:597 (renderHud)](GamePanel.java#L597) (text and HUD styling).
     */
    public static void cornerFrame(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1));
        int l = 18;
        for (int xx : new int[]{x, x + w}) for (int yy : new int[]{y, y + h}) {
            g.drawLine(xx, yy, xx + (xx == x ? l : -l), yy);
            g.drawLine(xx, yy, xx, yy + (yy == y ? l : -l));
        }
    }

    /*
     * Draw a horizontal separator using the shared LINE color.
     * [p. 23: Theme](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-23).
     * Related: [ui/InfoPanel.java:83 (rule call)](InfoPanel.java#L83) (caller of rule);
     * [ui/ResultPanel.java:136 (rule call)](ResultPanel.java#L136) (caller of rule);
     * [ui/ScenePanel.java:158 (paintComponent)](ScenePanel.java#L158) (sets drawing quality).
     */
    public static void rule(Graphics2D g, int x, int y, int width) {
        g.setColor(LINE);
        g.drawLine(x, y, x + width, y);
    }
}
