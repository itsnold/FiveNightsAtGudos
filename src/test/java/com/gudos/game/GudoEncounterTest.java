package com.gudos.game;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class GudoEncounterTest {
    @Test void followsConnectedRoutesAndClearsPreviousRooms() {
        GameSession game = new GameSession(1);
        game.update(18);
        assertEquals(Scene.CAM_04, game.gudo().location());
        assertEquals("occupied", game.sceneState(Scene.CAM_04));
        game.update(11);
        assertEquals(Scene.CAM_03, game.gudo().location());
        assertEquals("default", game.sceneState(Scene.CAM_04));
        game.update(11);
        assertEquals(Scene.CAM_02, game.gudo().location());
        game.update(11);
        assertEquals("left-occupied", game.sceneState(Scene.OFFICE));
        repel(game);
        assertEquals("default", game.sceneState(Scene.OFFICE));
        game.update(12);
        assertEquals(Scene.CAM_06, game.gudo().location());
        nextOffice(game);
        assertEquals("right-occupied", game.sceneState(Scene.OFFICE));
        repel(game);
        game.update(11);
        assertEquals(Scene.CAM_01, game.gudo().location());
        nextOffice(game);
        assertEquals("front-occupied", game.sceneState(Scene.OFFICE));
    }

    @Test void ignoringGudoIsFatalEvenWithALongFrameOrOpenMonitor() {
        for (boolean monitor : new boolean[]{false, true}) {
            GameSession game = new GameSession(1);
            if (monitor) game.toggleMonitor();
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> game.update(10000));
            assertEquals(Outcome.CAUGHT, game.result().outcome());
            assertEquals(60, game.elapsed(), 1e-8);
            assertFalse(game.monitorOpen());
            assertFalse(game.lightOn());
            RunRecord record = game.result();
            game.update(100);
            game.abandon();
            assertSame(record, game.result());
        }
    }

    @Test void flashlightMustFaceTheDoorAndFadesGradually() {
        GameSession game = new GameSession(1);
        nextOffice(game);
        game.setOfficePan(0.9);
        game.setLight(true);
        game.update(1);
        assertEquals(0, game.gudo().fadeProgress());
        game.setOfficePan(0.1);
        game.update(0.8);
        assertEquals(0.5, game.gudo().fadeProgress(), 1e-8);
        game.setLight(false);
        game.update(0.4);
        assertTrue(game.gudo().fadeProgress() > 0 && game.gudo().fadeProgress() < 0.5);
        game.setLight(true);
        game.update(1.6);
        assertFalse(game.gudo().atOffice());
        assertNull(game.gudo().location());
        assertEquals(1, game.gudo().repelled());
        assertFalse(game.finished());
    }

    @Test void pauseFreezesAttackAndFadeButReleasesLight() {
        GameSession game = new GameSession(1);
        nextOffice(game);
        game.setOfficePan(0.1);
        game.setLight(true);
        game.update(0.5);
        double remaining = game.gudo().attackRemaining();
        double fade = game.gudo().fadeProgress();
        game.pause();
        game.update(500);
        assertEquals(remaining, game.gudo().attackRemaining());
        assertEquals(fade, game.gudo().fadeProgress());
        assertFalse(game.lightOn());
        game.resume();
        game.update(0.5);
        assertTrue(game.gudo().fadeProgress() < fade);
    }

    @Test void monitorPreventsFlashlightDefense() {
        GameSession game = new GameSession(1);
        nextOffice(game);
        game.setOfficePan(0.1);
        game.toggleMonitor();
        game.setLight(true);
        game.update(3);
        assertEquals(0, game.gudo().fadeProgress());
        assertTrue(game.gudo().atOffice());
    }

    @Test void allFiveNightsCanBeSurvivedWithReactionTimeAndConservedPower() {
        for (int night = 1; night <= 5; night++) {
            GameSession game = new GameSession(night);
            double reaction = 0;
            while (!game.finished()) {
                if (game.gudo().atOffice()) {
                    reaction += 0.05;
                    // Allow time to lower the monitor and turn toward each doorway.
                    if (reaction >= 1.8) {
                        game.setOfficePan(game.gudo().approach().pan());
                        game.setLight(true);
                    }
                } else {
                    reaction = 0;
                    game.setLight(false);
                }
                game.update(0.05);
            }
            assertEquals(Outcome.COMPLETED, game.result().outcome(), "Night " + night);
            assertTrue(game.gudo().repelled() >= 3);
            assertTrue(game.power() > 50);
        }
        assertTrue(new GameSession(5).gudo().attackSeconds() < new GameSession(1).gudo().attackSeconds());
    }

    @Test void morningTakesPriorityAtAnAttackDeadline() {
        GameSession game = new GameSession(1, 60);
        game.update(1000);
        assertEquals(Outcome.COMPLETED, game.result().outcome());
        assertEquals(60, game.elapsed());
    }

    @Test void simulationIsIndependentOfFrameSize() {
        GameSession coarse = new GameSession(3);
        GameSession fine = new GameSession(3);
        coarse.update(1000);
        while (!fine.finished()) fine.update(1.0 / 60);
        assertEquals(coarse.result().outcome(), fine.result().outcome());
        assertEquals(coarse.elapsed(), fine.elapsed(), 1e-7);
        assertEquals(coarse.power(), fine.power(), 1e-7);
    }

    private static void nextOffice(GameSession game) {
        for (int i = 0; i < 3000 && !game.gudo().atOffice() && !game.finished(); i++) game.update(0.05);
        assertTrue(game.gudo().atOffice());
        assertFalse(game.finished());
    }

    private static void repel(GameSession game) {
        game.setOfficePan(game.gudo().approach().pan());
        game.setLight(true);
        game.update(game.gudo().flashSeconds());
        game.setLight(false);
        assertFalse(game.gudo().atOffice());
    }
}
