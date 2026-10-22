package com.gudos.assets;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class PanoramaRendererTest {
    private BufferedImage gradient() {
        BufferedImage image = new BufferedImage(1916, 821, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            image.setRGB(x, y, ((x * 255 / 1916) << 16) | ((y * 255 / 821) << 8) | 80);
        }
        return image;
    }

    @Test void perspectiveCurvesVerticalSamplingInsteadOfJustCropping() {
        BufferedImage projected = new PanoramaRenderer().render(gradient(), 0.5);
        int topEdge = (projected.getRGB(0, 0) >>> 8) & 255;
        int topCenter = (projected.getRGB(PanoramaRenderer.WIDTH / 2, 0) >>> 8) & 255;
        assertTrue(topEdge - topCenter > 15, "Cylindrical projection must bend the edges, unlike a flat crop");
    }

    @Test void turningRevealsDifferentPartsOfTheRoomAndReusesOutputBuffer() {
        PanoramaRenderer renderer = new PanoramaRenderer();
        BufferedImage source = gradient();
        BufferedImage left = renderer.render(source, 0);
        int leftCenter = left.getRGB(PanoramaRenderer.WIDTH / 2, PanoramaRenderer.HEIGHT / 2);
        BufferedImage right = renderer.render(source, 1);
        int rightCenter = right.getRGB(PanoramaRenderer.WIDTH / 2, PanoramaRenderer.HEIGHT / 2);
        assertSame(left, right);
        assertNotEquals(leftCenter, rightCenter);
        assertSame(right, renderer.render(source, 1));
    }
}
