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

/** Loads bundled images once and exposes scene/state lookups to screens without file-handling code. [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). Related: [Main.java:72 (launch)](../Main.java#L72) (loads assets at startup); [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (uses room images). */
/*
 * AssetManager
 *
 * Related: [Main.java:72 (launch)](../Main.java#L72) (loads assets at startup);
 * [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (uses room images);
 * [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (uses CCTV images);
 * [config/scenes.properties](../../../../resources/config/scenes.properties) (image-state manifest).
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
 * [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
 */
public final class AssetManager {
    private final Map<String, BufferedImage> scenes = new HashMap<>();
    private final BufferedImage[] menuRenders = new BufferedImage[5];
    private final BufferedImage[] noise;

    /*
     * Decode five title portraits; load scenes.properties and decode each unique referenced image
     * once; require a default for all seven scenes. Decode all GIF frames and turn luminance into
     * alpha for white TV noise. Missing listed resources throw IOException during boot.
     * [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [Main.java:114 (AssetManager call)](../Main.java#L114) (caller of AssetManager);
     * [game/Scene.java:49 (Scene)](../game/Scene.java#L49) (scene enum constants used by AssetManager);
     * [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (uses room images).
     */
    public AssetManager() throws IOException {
        String[] menuFiles = {"00_NORMAL.png", "01_HOLLOW_STARE.png", "02_DEAD_EYES.png", "03_BROKEN_SMILE.png", "04_LAST_FLICKER.png"};
        for (int i = 0; i < menuFiles.length; i++) {
            try (InputStream stream = resource("/images/menu/" + menuFiles[i])) {
                menuRenders[i] = ImageIO.read(stream);
                if (menuRenders[i] == null) throw new IOException("Unsupported menu image: " + menuFiles[i]);
            }
        }
        // The manifest maps keys such as cam03.occupied to a classpath image resource. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [Main.java:114 (AssetManager call)](../Main.java#L114) (caller of AssetManager); [game/Scene.java:49 (Scene)](../game/Scene.java#L49) (scene enum constants used by AssetManager).
        Properties manifest = new Properties();
        try (InputStream stream = resource("/config/scenes.properties")) { manifest.load(stream); }
        // Several state keys may share one file; decode that file only once. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [Main.java:114 (AssetManager call)](../Main.java#L114) (caller of AssetManager); [game/Scene.java:49 (Scene)](../game/Scene.java#L49) (scene enum constants used by AssetManager).
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
        // Every room needs a clean default image; broken required assets fail startup visibly. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [Main.java:114 (AssetManager call)](../Main.java#L114) (caller of AssetManager); [game/Scene.java:49 (Scene)](../game/Scene.java#L49) (scene enum constants used by AssetManager).
        for (Scene scene : Scene.values()) {
            if (!scenes.containsKey(scene.id() + ".default")) throw new IOException("Missing default render: " + scene.id());
        }
        // Unmodified source GIF is bundled with its attribution and CC BY-SA license. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [Main.java:114 (AssetManager call)](../Main.java#L114) (caller of AssetManager); [game/Scene.java:49 (Scene)](../game/Scene.java#L49) (scene enum constants used by AssetManager).
        // Runtime frames turn black into transparency so noise overlays the actual room. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [Main.java:114 (AssetManager call)](../Main.java#L114) (caller of AssetManager); [game/Scene.java:49 (Scene)](../game/Scene.java#L49) (scene enum constants used by AssetManager).
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

    // Unknown state names fall back to the room's default; listed-but-missing files fail during loading. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (office image client); [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (camera image client).
    /*
     * Look up scene.id + dot + state in cached images; if the key is unknown return that scene
     * default. A manifest entry with a missing file already fails during construction instead of
     * silently falling back.
     * [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (office image client);
     * [ui/rendering/CameraSceneRenderer.java:65 (render)](../ui/rendering/CameraSceneRenderer.java#L65) (camera image client);
     * [game/GameSession.java:247 (sceneState)](../game/GameSession.java#L247) (provides occupancy keys).
     */
    public BufferedImage scene(Scene scene, String state) {
        return scenes.getOrDefault(scene.id() + "." + state, scenes.get(scene.id() + ".default"));
    }

    // Wrap frame indices so callers can animate the noise indefinitely. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [ui/VhsOverlay.java:49 (paint)](../ui/VhsOverlay.java#L49) (decoded-static client); [ui/rendering/VhsRendererDecorator.java:77 (render)](../ui/rendering/VhsRendererDecorator.java#L77) (camera interference client).
    /*
     * Use floorMod to wrap positive or negative frame indices through the cached GIF frames. The
     * returned image is shared, so callers should draw rather than mutate it.
     * [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [ui/VhsOverlay.java:49 (paint)](../ui/VhsOverlay.java#L49) (decoded-static client);
     * [ui/rendering/VhsRendererDecorator.java:77 (render)](../ui/rendering/VhsRendererDecorator.java#L77) (camera interference client).
     */
    public BufferedImage noise(int frame) { return noise[Math.floorMod(frame, noise.length)]; }
    /*
     * Return the decoded GIF frame count. The bundled static contains five distinct
     * animation frames.
     * [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [ui/VhsOverlay.java:49 (paint)](../ui/VhsOverlay.java#L49) (decoded-static client);
     * [ui/rendering/VhsRendererDecorator.java:77 (render)](../ui/rendering/VhsRendererDecorator.java#L77) (camera interference client).
     */
    public int noiseFrameCount() { return noise.length; }
    /*
     * Return one of the five decoded title portraits by index. The caller supplies a valid 0..4
     * index; this method does not clamp it.
     * [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [ui/MenuPanel.java:187 (render)](../ui/MenuPanel.java#L187) (draws flicker portraits);
     * [ui/BackdropPanel.java:53 (backdrop)](../ui/BackdropPanel.java#L53) (secondary-page portrait background).
     */
    public BufferedImage menuRender(int index) { return menuRenders[index]; }

    // Read from the classpath, so the same resource lookup works inside the packaged JAR. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [audio/AudioService.java:87 (resource call)](../audio/AudioService.java#L87) (caller of resource); [Main.java:72 (launch)](../Main.java#L72) (loads assets at startup).
    // The caller owns and closes the returned stream. | [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). | Related: [audio/AudioService.java:87 (resource call)](../audio/AudioService.java#L87) (caller of resource); [Main.java:72 (launch)](../Main.java#L72) (loads assets at startup).
    /*
     * Open a classpath resource, including when the game is packaged in a JAR. Throw IOException if
     * absent; the caller must close the stream using try-with-resources.
     * [p. 28: AssetManager](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [audio/AudioService.java:87 (resource call)](../audio/AudioService.java#L87) (caller of resource);
     * [Main.java:72 (launch)](../Main.java#L72) (loads assets at startup);
     * [ui/GamePanel.java:477 (renderOffice)](../ui/GamePanel.java#L477) (uses room images).
     */
    public static InputStream resource(String path) throws IOException {
        InputStream stream = AssetManager.class.getResourceAsStream(path);
        if (stream == null) throw new IOException("Missing bundled resource: " + path);
        return stream;
    }
}
