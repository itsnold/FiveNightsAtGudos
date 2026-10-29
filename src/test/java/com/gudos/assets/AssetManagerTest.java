package com.gudos.assets;

import com.gudos.audio.Sound;
import com.gudos.game.Scene;
import com.gudos.ui.Theme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;

import static org.junit.jupiter.api.Assertions.*;

class AssetManagerTest {
    private static AssetManager assets;
    @BeforeAll static void load() throws Exception { assets = new AssetManager(); }

    @Test void allSevenBundledScenesLoadAtOriginalResolution() {
        for (Scene scene : Scene.values()) {
            assertEquals(1916, assets.scene(scene, "default").getWidth());
            assertEquals(821, assets.scene(scene, "default").getHeight());
        }
    }

    @Test void futureUnmappedStatesFallbackToCleanRoom() {
        assertSame(assets.scene(Scene.CAM_03, "default"), assets.scene(Scene.CAM_03, "future-state"));
    }

    @Test void everyEnemyRenderIsBundledAndDistinctFromTheCleanRoom() {
        for (Scene scene : Scene.values()) {
            if (scene == Scene.OFFICE) continue;
            var occupied = assets.scene(scene, "occupied");
            assertNotSame(assets.scene(scene, "default"), occupied);
            assertTrue(occupied.getWidth() > 1800);
            assertEquals(21.0 / 9, occupied.getWidth() / (double) occupied.getHeight(), 0.02);
        }
        for (String state : new String[]{"left-occupied", "front-occupied", "right-occupied", "jumpscare"}) {
            assertNotSame(assets.scene(Scene.OFFICE, "default"), assets.scene(Scene.OFFICE, state), state);
        }
    }

    @Test void everyActiveWavCanBeDecodedWithoutAudioHardware() throws Exception {
        for (Sound sound : Sound.values()) {
            try (var stream = AudioSystem.getAudioInputStream(new BufferedInputStream(AssetManager.resource(sound.path())))) {
                assertTrue(stream.getFrameLength() > 0, sound.name());
                assertTrue(stream.getFormat().getSampleRate() > 0, sound.name());
            }
        }
    }

    @Test void downloadedStaticIsAnimatedAndPixelFontIsBundled() throws Exception {
        assertEquals(5, assets.noiseFrameCount());
        assertEquals(640, assets.noise(0).getWidth());
        assertEquals(361, assets.noise(0).getHeight());
        assertNotSame(assets.noise(0), assets.noise(1));
        assertTrue(Theme.pixel(32).getFamily().contains("VT323"));
        try (var credits = AssetManager.resource("/licenses/asset-credits.txt")) {
            assertTrue(new String(credits.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("GravelTravel22"));
        }
    }
}
