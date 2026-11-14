package com.gudos;

import com.gudos.game.minigame.SecretGame;
import com.gudos.ui.SecretPanel;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

class SecretLaunchTest {
    @Test void levelCanBeCompletedWithinTheTimer() {
        var level = Application.createSecretLevel();
        var reference = new SecretGame(level);
        reference.update(.8, false, true);
        // Sample every patrol at simulation-step resolution, independently of player movement.
        java.awt.Polygon[][] beams = new java.awt.Polygon[3000][3];
        for (int i = 0; i < beams.length; i++) {
            reference.update(.02, false, false);
            for (int guard = 0; guard < 3; guard++) beams[i][guard] = reference.witnessBeam(guard);
        }
        record Node(int position, Node previous, int direction) { }
        var states = new java.util.HashMap<Integer, Node>();
        states.put(0, new Node(0, null, 0));
        Node solution = null;
        // Search walking/waiting choices. Each step is 0.1 seconds or 19 scene units.
        for (int step = 0; step < 600 && solution == null; step++) {
            var next = new java.util.HashMap<Integer, Node>();
            for (Node node : states.values()) for (int direction = -1; direction <= 1; direction++) {
                int position = node.position() + direction;
                if (position < 0 || position > 120 || next.containsKey(position)) continue;
                boolean safe = true;
                for (int sub = 0; sub < 5 && safe; sub++) {
                    double bundleX = 232 + node.position() * 19 + direction * 3.8 * (sub + 1) + 20;
                    boolean covered = level.cover().stream().anyMatch(r -> r.contains(bundleX, 618));
                    if (!covered) for (var beam : beams[step * 5 + sub]) if (beam.contains(bundleX, 618)) { safe = false; break; }
                }
                if (!safe) continue;
                Node candidate = new Node(position, node, direction);
                next.put(position, candidate);
                if (232 + position * 19 + 20 >= level.destination().x) { solution = candidate; break; }
            }
            states = next;
        }
        assertNotNull(solution, "Three patrols must leave a walkable, timed route");
        var moves = new java.util.ArrayList<Integer>();
        for (Node node = solution; node.previous() != null; node = node.previous()) moves.add(node.direction());
        java.util.Collections.reverse(moves);
        var game = new SecretGame(level);
        game.update(.8, false, true);
        game.action();
        for (int direction : moves) game.update(.1, direction < 0, direction > 0);
        assertEquals(SecretGame.State.PLAYING, game.state(), "planned route should avoid detection");
        game.action();
        assertEquals(SecretGame.State.WON, game.state());
    }
    @Test void productionLevelAndPanelCanBeConstructedOnSwingThread() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var level = Application.createSecretLevel();
            assertEquals(2560, level.width());
            assertEquals(6, level.cover().size());
            assertEquals(3, level.patrols().size());
            var game = new SecretGame(level);
            var panel = new SecretPanel(game, won -> { }, sound -> { }, () -> { }, () -> false);
            panel.setSize(1280, 720);
            panel.tick(0.033);
            var image = new java.awt.image.BufferedImage(1280, 720, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            try {
                panel.paint(graphics);
                java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/ui-preview"));
                javax.imageio.ImageIO.write(image, "png", java.nio.file.Path.of("target/ui-preview/secret-production-start.png").toFile());
                // Walk without picking up the bundle to inspect the scrolling kitchen safely.
                game.update(8, false, true);
                panel.paint(graphics);
                javax.imageio.ImageIO.write(image, "png", java.nio.file.Path.of("target/ui-preview/secret-production-kitchen.png").toFile());
            } catch (java.io.IOException error) { throw new RuntimeException(error); }
            finally { graphics.dispose(); }
        });
    }
}
