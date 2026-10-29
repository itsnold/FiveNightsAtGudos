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

/** Loads bundled images once and exposes scene/state lookups to screens without file-handling code. */
public final class AssetManager {
    private final Map<String, BufferedImage> scenes = new HashMap<>();
    private final BufferedImage[] menuRenders = new BufferedImage[5];
    private final BufferedImage[] noise;

    public AssetManager() throws IOException {
        String[] menuFiles = {"00_NORMAL.png", "01_HOLLOW_STARE.png", "02_DEAD_EYES.png", "03_BROKEN_SMILE.png", "04_LAST_FLICKER.png"};
        for (int i = 0; i < menuFiles.length; i++) {
            try (InputStream stream = resource("/images/menu/" + menuFiles[i])) {
                menuRenders[i] = ImageIO.read(stream);
                if (menuRenders[i] == null) throw new IOException("Unsupported menu image: " + menuFiles[i]);
            }
        }
        // The manifest maps keys such as cam03.occupied to a classpath image resource.
        Properties manifest = new Properties();
        try (InputStream stream = resource("/config/scenes.properties")) { manifest.load(stream); }
        // Several state keys may share one file; decode that file only once.
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
        // Every room needs a clean default image; broken required assets fail startup visibly.
        for (Scene scene : Scene.values()) {
            if (!scenes.containsKey(scene.id() + ".default")) throw new IOException("Missing default render: " + scene.id());
        }
        // Unmodified source GIF is bundled with its attribution and CC BY-SA license.
        // Runtime frames turn black into transparency so noise overlays the actual room.
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

    // Unknown state names fall back to the room's default; listed-but-missing files fail during loading.
    public BufferedImage scene(Scene scene, String state) {
        return scenes.getOrDefault(scene.id() + "." + state, scenes.get(scene.id() + ".default"));
    }

    // Wrap frame indices so callers can animate the noise indefinitely.
    public BufferedImage noise(int frame) { return noise[Math.floorMod(frame, noise.length)]; }
    public int noiseFrameCount() { return noise.length; }
    public BufferedImage menuRender(int index) { return menuRenders[index]; }

    // Read from the classpath, so the same resource lookup works inside the packaged JAR.
    // The caller owns and closes the returned stream.
    public static InputStream resource(String path) throws IOException {
        InputStream stream = AssetManager.class.getResourceAsStream(path);
        if (stream == null) throw new IOException("Missing bundled resource: " + path);
        return stream;
    }
}
