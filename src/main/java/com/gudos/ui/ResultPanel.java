package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;

import java.awt.*;

/** Displays the final RunRecord and reflects the asynchronous save's pending/success/failure status. [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). Related: [Application.java:538 (finishShift)](../Application.java#L538) (creates this screen). */
/*
 * ResultPanel
 *
 * Related: [Application.java:538 (finishShift)](../Application.java#L538) (creates this screen);
 * [Application.java:574 (savePendingRun)](../Application.java#L574) (save and retry coordinator);
 * [Application.java:358 (startNight)](../Application.java#L358) (handles replay/next-night callbacks).
 *
 * Purpose: Displays an ended RunRecord and the pending/saved/failed storage status.
 *
 * Execution flow: Application.finishShift builds it; savePendingRun drives saving/saved/failed;
 * buttons request next/replay/menu/retry.
 *
 * Design rationale: Disable next/menu while saving; allow retry with the same
 * pending UUID. A completed Night 1-4 offers next; other endings offer the same night; Night 5
 * remains capped.
 *
 * State: run is immutable; next/menu/retry are buttons; saveFailed and saveStatus
 * control feedback.
 *
 * Summary: A displayed result and a fully saved result are different states. This panel makes
 * the asynchronous storage status visible.
 * [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
 */
public final class ResultPanel extends BackdropPanel {
    private final RunRecord run;
    private final GameButton next;
    private final GameButton menu;
    private final GameButton retry;
    private String saveStatus = "Writing your shift to SQLite and shift-log.txt…";
    private boolean saveFailed;

    /*
     * Retain the final RunRecord. Choose next night only for a normal completion, cap at five,
     * otherwise replay the same night. Create next/menu/retry callbacks; initially block navigation
     * and hide Retry until Application reports save status.
     * [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [Application.java:547 (ResultPanel call)](../Application.java#L547) (caller of ResultPanel);
     * [game/RunRecord.java:30 (outcome)](../game/RunRecord.java#L30) (used by ResultPanel);
     * [ui/Navigation.java:68 (startNight)](Navigation.java#L68) (used by ResultPanel).
     */
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

    // Block navigation to another shift until storage finishes, preserving this pending result. | [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (updates status from save callbacks); [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (attempt write behind the status).
    /*
     * Mark the status pending, disable next/menu and hide Retry. A second attempt must not replace
     * the pending result before storage finishes.
     * [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (updates status from save callbacks);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (attempt write behind the status);
     * [persistence/ShiftLogStore.java:61 (append)](../persistence/ShiftLogStore.java#L61) (report write also required for success).
     */
    public void saving() {
        saveFailed = false;
        saveStatus = "Writing your shift to SQLite and shift-log.txt…";
        next.setEnabled(false);
        menu.setEnabled(false);
        retry.setVisible(false);
    }

    // Application calls this after both the SQLite save and text report succeed. | [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (updates status from save callbacks); [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (attempt write behind the status).
    /*
     * Mark both stores successful, enable next/menu and hide Retry. Application calls this only
     * after SQLite save and text append both return.
     * [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (updates status from save callbacks);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (attempt write behind the status);
     * [persistence/ShiftLogStore.java:61 (append)](../persistence/ShiftLogStore.java#L61) (report write also required for success).
     */
    public void saved() {
        saveFailed = false;
        saveStatus = "SAVED TO SQLITE  +  SHIFT-LOG.TXT";
        next.setEnabled(true);
        menu.setEnabled(true);
        retry.setVisible(false);
    }

    // Keep the result available for retry; returning to the menu requires an incomplete-save confirmation. | [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24). | Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (updates status from save callbacks); [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (attempt write behind the status).
    /*
     * Keep next disabled, allow menu with a coordinator warning and show Retry Save. The same
     * immutable RunRecord remains available, so ordinary retries do not add duplicate rows.
     * [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [Application.java:574 (savePendingRun)](../Application.java#L574) (updates status from save callbacks);
     * [persistence/Database.java:261 (saveRun)](../persistence/Database.java#L261) (attempt write behind the status);
     * [persistence/ShiftLogStore.java:61 (append)](../persistence/ShiftLogStore.java#L61) (report write also required for success).
     */
    public void failed() {
        saveFailed = true;
        saveStatus = "Save failed. Retry, or return to the menu without a complete save.";
        next.setEnabled(false);
        menu.setEnabled(true);
        retry.setVisible(true);
    }

    /*
     * Draw the outcome-specific title/instruction, actual active duration, rounded remaining power
     * and save status. Only Outcome.COMPLETED receives normal victory treatment.
     * [p. 24: ResultPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-24).
     * Related: [game/RunRecord.java:30 (outcome)](../game/RunRecord.java#L30) (used by render);
     * [ui/Theme.java:150 (tracked)](Theme.java#L150) (used by render);
     * [Application.java:538 (finishShift)](../Application.java#L538) (creates this screen).
     */
    @Override protected void render(Graphics2D g) {
        backdrop(g);
        boolean completed = run.outcome() == Outcome.COMPLETED;
        Theme.tracked(g, "NIGHT %02d  /  %s".formatted(run.night(), run.outcome().label().toUpperCase(java.util.Locale.ROOT)), 452, 151, 12, completed ? Theme.GREEN : Theme.AMBER);
        String title = completed ? "6 AM" : run.outcome() == Outcome.MINIGAME_COMPLETED ? "SECRET FOUND" : run.outcome() == Outcome.CAUGHT ? "GUDO FOUND YOU" : run.outcome() == Outcome.POWER_OUT ? "POWER FAILURE" : "OFF THE CLOCK";
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
