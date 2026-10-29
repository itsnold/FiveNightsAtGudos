package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;

import java.awt.*;

/** Displays the final RunRecord and reflects the asynchronous save's pending/success/failure status. */
public final class ResultPanel extends BackdropPanel {
    private final RunRecord run;
    private final GameButton next;
    private final GameButton menu;
    private final GameButton retry;
    private String saveStatus = "Writing your shift to SQLite and shift-log.txt…";
    private boolean saveFailed;

    public ResultPanel(AssetManager assets, Navigation navigation, RunRecord run, Runnable retrySave) {
        super(assets);
        this.run = run;
        int nextNight = run.outcome() == Outcome.COMPLETED ? Math.min(5, run.night() + 1) : run.night();
        String label = run.outcome() == Outcome.COMPLETED && run.night() < 5 ? "NEXT NIGHT     →" : "PLAY AGAIN     →";
        next = place(new GameButton(label, GameButton.Style.PRIMARY, () -> navigation.startNight(nextNight)), 370, 481, 260, 51);
        menu = place(new GameButton("MAIN MENU", GameButton.Style.SECONDARY, navigation::menu), 649, 481, 260, 51);
        retry = place(new GameButton("RETRY SAVE", GameButton.Style.SECONDARY, retrySave), 509, 578, 260, 40);
        next.setEnabled(false);
        menu.setEnabled(false);
        retry.setVisible(false);
    }

    // Block navigation to another shift until storage finishes, preserving this pending result.
    public void saving() {
        saveFailed = false;
        saveStatus = "Writing your shift to SQLite and shift-log.txt…";
        next.setEnabled(false);
        menu.setEnabled(false);
        retry.setVisible(false);
    }

    // Application calls this after both the SQLite save and text report succeed.
    public void saved() {
        saveFailed = false;
        saveStatus = "SAVED TO SQLITE  +  SHIFT-LOG.TXT";
        next.setEnabled(true);
        menu.setEnabled(true);
        retry.setVisible(false);
    }

    // Keep the result available for retry; returning to the menu requires an incomplete-save confirmation.
    public void failed() {
        saveFailed = true;
        saveStatus = "Save failed. Retry, or return to the menu without a complete save.";
        next.setEnabled(false);
        menu.setEnabled(true);
        retry.setVisible(true);
    }

    @Override protected void render(Graphics2D g) {
        backdrop(g);
        boolean completed = run.outcome() == Outcome.COMPLETED;
        Theme.tracked(g, "NIGHT %02d  /  %s".formatted(run.night(), run.outcome().label().toUpperCase(java.util.Locale.ROOT)), 452, 151, 12, completed ? Theme.GREEN : Theme.AMBER);
        String title = completed ? "6 AM" : run.outcome() == Outcome.CAUGHT ? "GUDO FOUND YOU" : run.outcome() == Outcome.POWER_OUT ? "POWER FAILURE" : "OFF THE CLOCK";
        g.setFont(Theme.heading(completed ? 112 : 64));
        int titleX = (WIDTH - g.getFontMetrics().stringWidth(title)) / 2;
        Theme.text(g, title, titleX, 281, Theme.heading(completed ? 112 : 64), Theme.PAPER);
        String subtitle = completed ? "The morning shift can take it from here."
                : run.outcome() == Outcome.CAUGHT ? "Face his approach and hold F until he disappears. Don't look away."
                : run.outcome() == Outcome.POWER_OUT ? "The building ran out of power. Use the light only when you need it."
                : "Your attempt is recorded. The next night is not unlocked.";
        g.setFont(Theme.sans(18));
        Theme.text(g, subtitle, (WIDTH - g.getFontMetrics().stringWidth(subtitle)) / 2, 324, Theme.sans(18), Theme.MUTED);
        Theme.rule(g, 370, 365, 540);
        Theme.tracked(g, "TIME ON DUTY", 414, 397, 10, Theme.AMBER);
        Theme.text(g, run.durationLabel(), 434, 439, Theme.mono(32), Theme.PAPER);
        Theme.tracked(g, "POWER REMAINING", 688, 397, 10, Theme.AMBER);
        Theme.text(g, "%d%%".formatted(Math.round(run.powerRemaining())), 731, 439, Theme.mono(32), Theme.PAPER);
        g.setFont(Theme.mono(saveFailed ? 12 : 11));
        Theme.text(g, saveStatus, (WIDTH - g.getFontMetrics().stringWidth(saveStatus)) / 2, 562, Theme.mono(saveFailed ? 12 : 11), saveFailed ? Theme.RED : Theme.MUTED);
    }
}
