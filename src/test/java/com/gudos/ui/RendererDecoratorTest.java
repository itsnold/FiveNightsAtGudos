package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.assets.PanoramaRenderer;
import com.gudos.game.CameraMotion;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
import com.gudos.ui.rendering.CameraSceneRenderer;
import com.gudos.ui.rendering.DimmedRendererDecorator;
import com.gudos.ui.rendering.SceneRenderer;
import com.gudos.ui.rendering.VhsRendererDecorator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RendererDecoratorTest {
    private static AssetManager assets;
    private static final SceneRenderer WHITE_ROOM = (g, time, reducedEffects) -> {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT);
    };

    @BeforeAll static void load() throws Exception { assets = new AssetManager(); }

    @Test void nestedDecoratorsDelegateExactlyOnceWithTheOriginalFrameContext() {
        int[] calls = {0};
        SceneRenderer room = (g, time, reducedEffects) -> {
            calls[0]++;
            assertEquals(2.5, time);
            assertTrue(reducedEffects);
        };
        SceneRenderer decorated = new VhsRendererDecorator(
                new DimmedRendererDecorator(room, 115), assets, 0.85f, 3);
        render(decorated, 2.5, true);
        assertEquals(1, calls[0]);
    }

    @Test void reducedEffectsAndZeroOpacityKeepTheWrappedRendering() {
        int[] plain = pixels(render(WHITE_ROOM, 1, false));
        assertArrayEquals(plain, pixels(render(new VhsRendererDecorator(WHITE_ROOM, assets, 0.5f), 1, true)));
        assertArrayEquals(plain, pixels(render(new VhsRendererDecorator(WHITE_ROOM, assets, 0), 1, false)));
    }

    @Test void vhsAddsAnimatedInterferenceAndDimmingAddsDarkness() {
        SceneRenderer vhs = new VhsRendererDecorator(WHITE_ROOM, assets, 0.5f);
        int[] first = pixels(render(vhs, 0, false));
        assertFalse(Arrays.equals(pixels(render(WHITE_ROOM, 0, false)), first));
        assertFalse(Arrays.equals(first, pixels(render(vhs, 0.2, false))));
        BufferedImage dimmed = render(new DimmedRendererDecorator(WHITE_ROOM, 128), 0, false);
        assertEquals(new Color(127, 127, 127).getRGB(), dimmed.getRGB(100, 100));
    }

    @Test void decoratorOrderControlsTheLayerOrder() {
        SceneRenderer dimThenVhs = new VhsRendererDecorator(
                new DimmedRendererDecorator(WHITE_ROOM, 180), assets, 0.5f);
        SceneRenderer vhsThenDim = new DimmedRendererDecorator(
                new VhsRendererDecorator(WHITE_ROOM, assets, 0.5f), 180);
        assertFalse(Arrays.equals(pixels(render(dimThenVhs, 0, false)), pixels(render(vhsThenDim, 0, false))));
    }

    @Test void effectsDoNotLeakGraphicsSettingsIntoTheHud() {
        BufferedImage image = new BufferedImage(ScenePanel.WIDTH, ScenePanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            Theme.quality(g);
            g.setColor(Color.GREEN);
            g.setComposite(AlphaComposite.SrcOver.derive(0.8f));
            Paint paint = g.getPaint();
            Composite composite = g.getComposite();
            RenderingHints hints = g.getRenderingHints();
            SceneRenderer noOp = (graphics, time, reducedEffects) -> { };
            SceneRenderer decorated = new VhsRendererDecorator(
                    new DimmedRendererDecorator(noOp, 115), assets, 0.5f);
            decorated.render(g, 1, false);
            assertEquals(paint, g.getPaint());
            assertEquals(composite, g.getComposite());
            assertEquals(hints, g.getRenderingHints());
        } finally {
            g.dispose();
        }
    }

    @Test void cameraDecoratorsMatchThePreviousEffectsAndFollowCameraChanges() {
        GameSession session = new GameSession(1);
        session.toggleMonitor();
        session.update(3);
        SceneRenderer normal = new VhsRendererDecorator(new CameraSceneRenderer(session, assets), assets, 0.25f);
        SceneRenderer transition = new VhsRendererDecorator(
                new DimmedRendererDecorator(normal, 115), assets, 0.85f, 3);
        for (boolean reducedEffects : new boolean[]{false, true}) {
            for (boolean switching : new boolean[]{false, true}) {
                session.selectCamera(switching ? 6 : 3);
                session.setSceneState(Scene.camera(session.camera()), switching ? "occupied" : "default");
                SceneRenderer actual = switching && !reducedEffects ? transition : normal;
                SceneRenderer previous = (g, time, reduced) -> {
                    Scene scene = Scene.camera(session.camera());
                    BufferedImage room = new PanoramaRenderer().render(assets.scene(scene, session.sceneState(scene)),
                            CameraMotion.pan(scene, session.elapsed()));
                    g.drawImage(room, 0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT, null);
                    g.setColor(new Color(0, 0, 0, 32));
                    g.fillRect(0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT);
                    Theme.vignette(g, 205);
                    VhsOverlay.paint(g, assets, time, 0.25f, reduced);
                    if (switching && !reduced) {
                        g.setColor(new Color(0, 0, 0, 115));
                        g.fillRect(0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT);
                        VhsOverlay.paint(g, assets, time * 3, 0.85f, false);
                    }
                };
                assertArrayEquals(pixels(render(previous, 0.7, reducedEffects)), pixels(render(actual, 0.7, reducedEffects)));
            }
        }
    }

    @Test void constructorsRejectInvalidDecoratorConfiguration() {
        assertThrows(NullPointerException.class, () -> new DimmedRendererDecorator(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new DimmedRendererDecorator(WHITE_ROOM, -1));
        assertThrows(IllegalArgumentException.class, () -> new DimmedRendererDecorator(WHITE_ROOM, 256));
        assertThrows(IllegalArgumentException.class, () -> new VhsRendererDecorator(WHITE_ROOM, assets, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new VhsRendererDecorator(WHITE_ROOM, assets, -0.1f));
        assertThrows(IllegalArgumentException.class, () -> new VhsRendererDecorator(WHITE_ROOM, assets, 1.1f));
        assertThrows(IllegalArgumentException.class, () -> new VhsRendererDecorator(WHITE_ROOM, assets, 0.5f, 0));
        assertThrows(IllegalArgumentException.class, () -> new VhsRendererDecorator(WHITE_ROOM, assets, 0.5f, Double.NaN));
    }

    private static BufferedImage render(SceneRenderer renderer, double time, boolean reducedEffects) {
        BufferedImage image = new BufferedImage(ScenePanel.WIDTH, ScenePanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            Theme.quality(g);
            renderer.render(g, time, reducedEffects);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int[] pixels(BufferedImage image) { return ((DataBufferInt) image.getRaster().getDataBuffer()).getData(); }
}
