package com.gudos.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {
    @Test void ordinaryShiftReachesMorningAndKeepsPower() {
        GameSession game = new GameSession(1);
        while (!game.finished()) {
            game.setOfficePan(game.gudo().approach().pan());
            game.setLight(game.gudo().atOffice());
            game.update(0.1);
        }
        assertTrue(game.finished());
        assertEquals(Outcome.COMPLETED, game.result().outcome());
        assertEquals(360, game.elapsed(), 0.0001);
        assertTrue(game.power() > 60 && game.power() < 69.4);
        assertTrue(game.gudo().repelled() >= 3);
        assertEquals("6 AM", game.clockLabel());
        assertEquals("06:00 AM", game.preciseClock());
    }

    @Test void monitorCanExhaustThePowerBeforeMorning() {
        GameSession game = new GameSession(1, 1000);
        while (!game.finished()) {
            if (game.monitorOpen() == game.gudo().atOffice()) game.toggleMonitor();
            game.setOfficePan(game.gudo().approach().pan());
            game.setLight(game.gudo().atOffice());
            game.update(0.1);
        }
        assertEquals(Outcome.POWER_OUT, game.result().outcome());
        assertEquals(0, game.power(), 0.0001);
        assertTrue(game.elapsed() > 300 && game.elapsed() < 400);
        assertFalse(game.monitorOpen());
    }

    @Test void pausingStopsBothClockAndDrain() {
        GameSession game = new GameSession(2);
        game.update(8);
        game.setLight(true);
        game.pause();
        double power = game.power();
        game.update(500);
        assertEquals(8, game.elapsed(), 0.0001);
        assertEquals(power, game.power(), 0.0001);
        assertFalse(game.lightOn());
        assertFalse(game.finished());
        game.resume();
        game.update(8);
        assertEquals(16, game.elapsed(), 0.0001);
        assertEquals(Scene.CAM_04, game.gudo().location());
    }

    @Test void lightAndMonitorAreMutuallyExclusive() {
        GameSession game = new GameSession(1);
        game.setLight(true);
        assertTrue(game.lightOn());
        assertEquals(3, game.usage());
        game.toggleMonitor();
        assertFalse(game.lightOn());
        game.setLight(true);
        assertFalse(game.lightOn());
        game.selectCamera(6);
        assertEquals(6, game.camera());
        game.toggleMonitor();
        game.selectCamera(2);
        assertEquals(6, game.camera(), "Closed monitor should ignore camera shortcuts");
    }

    @Test void abandonedAndCompletedResultsAreStable() {
        GameSession game = new GameSession(1);
        game.update(18);
        game.pause();
        game.abandon();
        RunRecord first = game.result();
        game.update(100);
        game.abandon();
        game.toggleMonitor();
        assertSame(first, game.result());
        assertEquals(Outcome.ABANDONED, first.outcome());
        assertEquals("00:18", first.durationLabel());
    }

    @Test void stateHooksAreIndependentAndDefaultToCleanRenders() {
        GameSession game = new GameSession(1);
        assertEquals("default", game.sceneState(Scene.CAM_03));
        game.setSceneState(Scene.CAM_03, "future-state");
        assertEquals("future-state", game.sceneState(Scene.CAM_03));
        assertEquals("default", game.sceneState(Scene.OFFICE));
    }

    @Test void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GameSession(0));
        assertThrows(IllegalArgumentException.class, () -> new GameSession(6));
        assertThrows(IllegalArgumentException.class, () -> new GameSession(1, 0));
        GameSession game = new GameSession(1);
        assertThrows(IllegalArgumentException.class, () -> game.update(-1));
        assertThrows(IllegalArgumentException.class, () -> game.update(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> game.selectCamera(7));
        assertThrows(IllegalStateException.class, game::result);
    }
}
