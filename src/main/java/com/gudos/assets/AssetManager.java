package com.gudos.assets;

import com.gudos.game.Scene;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** Loads bundled images once and exposes scene/state lookups to screens without file-handling code. Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). */
/*
 * AssetManager
 *
 * Purpose: Loads bundled images, the scene manifest and decoded TV-noise frames, then serves cached
 * lookups.
 *
 * Execution flow: Main constructs one manager in the boot worker. Screens ask scene(scene,state),
 * menuRender(index) or noise(frame); resource opens a classpath stream.
 *
 * Design rationale: Decode each referenced path once, require a default for all
 * rooms, and fail startup for a listed missing asset. Unknown state names fall back to a clean
 * default. This separates a bad key from a broken packaged asset.
 *
 * State: scenes maps keys such as cam03.occupied to images; menuRenders holds five
 * portraits; noise holds five alpha-masked GIF frames.
 *
 * Summary: Gameplay supplies a room and state key; AssetManager supplies the corresponding
 * cached image. It does not decide where Gudo moves.
 * Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
 */
public final class AssetManager {
    private final Map<String, BufferedImage> scenes = new HashMap<>();
    private final BufferedImage[] menuRenders = new BufferedImage[5];
    private final BufferedImage[] noise;

    /*
     * Decode five title portraits; load scenes.properties and decode each unique referenced image
     * once; require a default for all seven scenes. Decode all GIF frames and turn luminance into
     * alpha for white TV noise. Missing listed resources throw IOException during boot.
     * Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public AssetManager() throws IOException {
        String[] menuFiles = {"00_NORMAL.png", "01_HOLLOW_STARE.png", "02_DEAD_EYES.png", "03_BROKEN_SMILE.png", "04_LAST_FLICKER.png"};
        for (int i = 0; i < menuFiles.length; i++) {
            try (InputStream stream = resource("/images/menu/" + menuFiles[i])) {
                menuRenders[i] = ImageIO.read(stream);
                if (menuRenders[i] == null) throw new IOException("Unsupported menu image: " + menuFiles[i]);
            }
        }
        // The manifest maps keys such as cam03.occupied to a classpath image resource. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
        Properties manifest = new Properties();
        try (InputStream stream = resource("/config/scenes.properties")) { manifest.load(stream); }
        // Several state keys may share one file; decode that file only once. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
        Map<String, BufferedImage> loaded = new HashMap<>();
        for (String key : manifest.stringPropertyNames()) {
            String path = manifest.getProperty(key).strip();
            BufferedImage image = loaded.get(path);
            if (image == null) {
                try (InputStream stream = resource(path)) { image = ImageIO.read(stream); }
                if (image == null) throw new IOException("Unsupported image: " + path);
                loaded.put(path, image);
            }
            scenes.put(key, image);
        }
        // Every room needs a clean default image; broken required assets fail startup visibly. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
        for (Scene scene : Scene.values()) {
            if (!scenes.containsKey(scene.id() + ".default")) throw new IOException("Missing default render: " + scene.id());
        }
        // Unmodified source GIF is bundled with its attribution and CC BY-SA license. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
        // Runtime frames turn black into transparency so noise overlays the actual room. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
        try (InputStream stream = resource("/effects/television-static.gif");
             ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            try {
                reader.setInput(imageStream);
                noise = new BufferedImage[reader.getNumImages(true)];
                for (int frame = 0; frame < noise.length; frame++) {
                    BufferedImage original = reader.read(frame);
                    noise[frame] = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < original.getHeight(); y++) for (int x = 0; x < original.getWidth(); x++) {
                        int rgb = original.getRGB(x, y);
                        int luminance = (((rgb >>> 16) & 255) + ((rgb >>> 8) & 255) + (rgb & 255)) / 3;
                        noise[frame].setRGB(x, y, (luminance << 24) | 0xFFFFFF);
                    }
                }
            } finally { reader.dispose(); }
        }
    }

    // Unknown state names fall back to the room's default; listed-but-missing files fail during loading. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
    /*
     * Look up scene.id + dot + state in cached images; if the key is unknown return that scene
     * default. A manifest entry with a missing file already fails during construction instead of
     * silently falling back.
     * Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public BufferedImage scene(Scene scene, String state) {
        return scenes.getOrDefault(scene.id() + "." + state, scenes.get(scene.id() + ".default"));
    }

    // Wrap frame indices so callers can animate the noise indefinitely. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
    /*
     * Use floorMod to wrap positive or negative frame indices through the cached GIF frames. The
     * returned image is shared, so callers should draw rather than mutate it.
     * Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public BufferedImage noise(int frame) { return noise[Math.floorMod(frame, noise.length)]; }
    /*
     * Return the decoded GIF frame count. The bundled static contains five distinct
     * animation frames.
     * Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public int noiseFrameCount() { return noise.length; }
    /*
     * Return one of the five decoded title portraits by index. The caller supplies a valid 0..4
     * index; this method does not clamp it.
     * Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public BufferedImage menuRender(int index) { return menuRenders[index]; }

    // Read from the classpath, so the same resource lookup works inside the packaged JAR. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
    // The caller owns and closes the returned stream. | Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
    /*
     * Open a classpath resource, including when the game is packaged in a JAR. Throw IOException if
     * absent; the caller must close the stream using try-with-resources.
     * Reviewer (42-page edition): [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     */
    public static InputStream resource(String path) throws IOException {
        InputStream stream = AssetManager.class.getResourceAsStream(path);
        if (stream == null) throw new IOException("Missing bundled resource: " + path);
        return stream;
    }
}
