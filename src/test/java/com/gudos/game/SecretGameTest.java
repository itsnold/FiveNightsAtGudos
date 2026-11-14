package com.gudos.game;

import com.gudos.game.minigame.SecretGame;
import com.gudos.game.minigame.SecretLevel;
import com.gudos.game.minigame.SecretLevelBuilder;
import org.junit.jupiter.api.Test;
import java.awt.Rectangle;
import static org.junit.jupiter.api.Assertions.*;

class SecretGameTest {
    private SecretLevel level(double witnessLeft, double witnessRight) {
        return new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(200, 500, 100, 140))
                .witness(witnessLeft, witnessRight).timeLimit(60).build();
    }

    @Test void countdownStartsOnlyAtPickupAndContinuesAfterDropping() {
        SecretGame game = new SecretGame(level(900, 1100));
        game.update(10, false, false);
        assertFalse(game.started());
        assertEquals(60, game.remaining());
        game.action();
        game.update(1, false, false);
        game.action();
        game.update(1, false, false);
        assertEquals(58, game.remaining(), 0.001);
        assertFalse(game.carrying());
        game.update(59, false, false);
        assertEquals(SecretGame.State.LOST, game.state());
    }

    @Test void deliveryRequiresCarryingAndWins() {
        SecretGame game = new SecretGame(level(900, 1100));
        game.action();
        game.update(.55, false, true);
        game.action();
        assertEquals(SecretGame.State.WON, game.state());
    }

    @Test void witnessSeesCarriedOrDroppedBundleUnlessItIsBehindCover() {
        SecretGame game = new SecretGame(level(50, 70));
        game.action();
        game.update(.02, false, false);
        assertEquals(SecretGame.State.LOST, game.state());

        SecretLevel sheltered = new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(900, 500, 100, 140)).cover(new Rectangle(90, 500, 80, 140))
                .witness(50, 70).build();
        SecretGame hidden = new SecretGame(sheltered);
        hidden.action();
        hidden.action();
        hidden.update(.1, false, false);
        assertEquals(SecretGame.State.PLAYING, hidden.state());
    }

    @Test void witnessBeamIsShortAndPointsInPatrolDirection() {
        SecretGame game = new SecretGame(level(300, 400));
        assertTrue(game.witnessBeam().contains(380, 618));
        assertFalse(game.witnessBeam().contains(450, 618));
        assertFalse(game.witnessBeam().contains(250, 618));
        game.update(2, false, false); // Reach the right endpoint and turn around.
        assertFalse(game.witnessRight());
        assertTrue(game.witnessBeam().contains(game.witnessX() - 60, 618));
        assertFalse(game.witnessBeam().contains(game.witnessX() + 60, 618));
    }

    @Test void hidingWorksWhileCarryingOrAfterDroppingAndTimerContinues() {
        SecretLevel sheltered = new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(900, 500, 100, 140)).cover(new Rectangle(90, 500, 100, 140))
                .witness(50, 70).build();
        SecretGame game = new SecretGame(sheltered);
        game.action();
        assertTrue(game.bundleHidden());
        game.update(.1, false, false); // Witness faces the bundle, but the shelf hides it.
        assertEquals(SecretGame.State.PLAYING, game.state());
        game.action(); // Dropped bundle remains behind the shelf when Gudo walks away.
        game.update(.5, false, true);
        assertTrue(game.bundleHidden());
        assertEquals(SecretGame.State.PLAYING, game.state());
        assertEquals(59.4, game.remaining(), 0.001);
    }

    @Test void eachPatrolCanDetectTheBundleAndCoverProtectsAgainstAll() {
        for (int dangerous = 0; dangerous < 3; dangerous++) {
            SecretLevelBuilder builder = new SecretLevelBuilder().player(100).bundle(100)
                    .destination(new Rectangle(900, 500, 100, 140));
            for (int i = 0; i < 3; i++) builder.witness(i == dangerous ? 50 : 800 + 100 * i, i == dangerous ? 70 : 850 + 100 * i);
            SecretGame game = new SecretGame(builder.build());
            assertEquals(3, game.witnessCount());
            game.action();
            game.update(.02, false, false);
            assertEquals(SecretGame.State.LOST, game.state(), "patrol " + dangerous);
        }
        SecretGame hidden = new SecretGame(new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(900, 500, 100, 140)).cover(new Rectangle(90, 500, 80, 140))
                .witness(40, 50).witness(50, 60).witness(60, 70).build());
        hidden.action();
        hidden.update(.02, false, false);
        assertEquals(SecretGame.State.PLAYING, hidden.state());
    }

    @Test void extendedLevelAllowsWalkingBeyondTheOriginalScreen() {
        var level = new SecretLevelBuilder().width(2560).player(100).bundle(100)
                .destination(new Rectangle(2410, 510, 125, 130))
                .cover(new Rectangle(1400, 500, 165, 140)).witness(2100, 2200).build();
        SecretGame game = new SecretGame(level);
        game.update(7.2, false, true); // No timer or witness detection until the bundle is picked up.
        assertTrue(game.x() > 1400);
        assertTrue(game.x() < 1565);
    }
}
