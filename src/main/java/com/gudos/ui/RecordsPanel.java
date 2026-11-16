package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;
import com.gudos.persistence.Progress;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Read-only table built from attempts already loaded by Application's storage worker. [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). Related: [Application.java:319 (page)](../Application.java#L319) (loads data before creating this table); [persistence/Database.java:317 (loadRuns)](../persistence/Database.java#L317) (history query). */
/*
 * RecordsPanel
 *
 * Related: [Application.java:319 (page)](../Application.java#L319) (loads data before creating this table);
 * [persistence/Database.java:317 (loadRuns)](../persistence/Database.java#L317) (history query);
 * [game/RunRecord.java:39 (durationLabel)](../game/RunRecord.java#L39) (formats active duration);
 * [persistence/Progress.java:27 (Progress)](../persistence/Progress.java#L27) (all-time summary counts).
 *
 * Purpose: Displays the current profile summary and read-only recent attempts table.
 *
 * Execution flow: Extras -> Shift Records -> Application IO load -> constructor builds JTable from
 * the returned list.
 *
 * Design rationale: No SQL is run while rendering. Absolute Instants are displayed
 * in the local system zone. Only latest 200 rows are loaded; summary counts include all stored
 * attempts.
 *
 * State: player/progress/empty support headings/empty state; a noneditable
 * DefaultTableModel holds five columns and a JScrollPane provides scrolling.
 *
 * Summary: RecordsPanel displays saved history. Database.loadRuns performs
 * filtering/order/limit, and loadProgress supplies totals.
 * [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
 */
public final class RecordsPanel extends BackdropPanel {
    private final String player;
    private final Progress progress;
    private final boolean empty;

    /*
     * Receive already-loaded profile summary and recent rows, install Back, and if nonempty build a
     * read-only five-column JTable with local-zone timestamps, striped cells and scroll pane. No
     * JDBC runs in this constructor.
     * [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [Application.java:333 (RecordsPanel call)](../Application.java#L333) (caller of RecordsPanel);
     * [game/RunRecord.java:30 (endedAt)](../game/RunRecord.java#L30) (used by RecordsPanel);
     * [ui/Theme.java:103 (mono)](Theme.java#L103) (used by RecordsPanel).
     */
    public RecordsPanel(AssetManager assets, Navigation navigation, String player, Progress progress, List<RunRecord> runs) {
        super(assets);
        this.player = player;
        this.progress = progress;
        empty = runs.isEmpty();
        backButton(navigation);
        if (empty) return;
        // Stored Instants represent absolute timestamps; display them in this computer's local timezone. | [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [Application.java:333 (RecordsPanel call)](../Application.java#L333) (caller of RecordsPanel); [game/RunRecord.java:30 (endedAt)](../game/RunRecord.java#L30) (used by RecordsPanel).
        DateTimeFormatter date = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm").withZone(ZoneId.systemDefault());
        DefaultTableModel model = new DefaultTableModel(new String[]{"FINISHED", "NIGHT", "RESULT", "TIME ON DUTY", "POWER LEFT"}, 0) {
            /*
             * Always return false for every row/column. The history table is a display of saved
             * results, not an editor for the underlying database.
             * [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
             * Related: [Application.java:319 (page)](../Application.java#L319) (loads data before creating this table);
             * [persistence/Database.java:317 (loadRuns)](../persistence/Database.java#L317) (history query);
             * [game/RunRecord.java:39 (durationLabel)](../game/RunRecord.java#L39) (formats active duration).
             */
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        // Convert typed game results into the five columns shown to the player. No SQL runs in this panel. | [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [Application.java:333 (RecordsPanel call)](../Application.java#L333) (caller of RecordsPanel); [game/RunRecord.java:30 (endedAt)](../game/RunRecord.java#L30) (used by RecordsPanel).
        for (RunRecord run : runs) model.addRow(new Object[]{date.format(run.endedAt()), "%02d".formatted(run.night()), run.outcome().label(), run.durationLabel(), "%d%%".formatted(Math.round(run.powerRemaining()))});
        JTable table = new JTable(model);
        table.setFont(Theme.mono(14));
        table.setRowHeight(40);
        table.setBackground(new Color(16, 25, 18));
        table.setForeground(Theme.PAPER);
        table.setGridColor(new Color(43, 54, 39));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(65, 77, 53));
        table.setSelectionForeground(Theme.PAPER);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.mono(12));
        table.getTableHeader().setBackground(new Color(30, 39, 27));
        table.getTableHeader().setForeground(Theme.MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(235);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            /*
             * Reuse Swing default cell rendering, add padding, alternate row backgrounds and color
             * completed outcomes. It only styles existing values; it does not query or write SQL.
             * [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
             * Related: [persistence/Database.java:317 (loadRuns)](../persistence/Database.java#L317) (history displayed by this table renderer);
             * [Application.java:319 (page)](../Application.java#L319) (loads data before creating this table);
             * [persistence/Database.java:317 (loadRuns)](../persistence/Database.java#L317) (history query).
             */
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                if (!selected) {
                    setBackground(row % 2 == 0 ? new Color(16, 25, 18) : new Color(20, 29, 21));
                    setForeground(column == 2 ? runs.get(row).outcome() == Outcome.COMPLETED ? Theme.GREEN : Theme.AMBER : Theme.PAPER);
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        table.getAccessibleContext().setAccessibleName("Recent shift records for " + player);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        scroll.getViewport().setBackground(new Color(16, 25, 18));
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        place(scroll, 82, 331, 1116, 272);
    }

    // Summary counters cover all this profile's attempts, even though only the newest 200 rows are loaded. | [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). | Related: [persistence/Progress.java:27 (totalRuns)](../persistence/Progress.java#L27) (used by render); [ui/Theme.java:150 (tracked)](Theme.java#L150) (used by render).
    /*
     * Draw summary counters for this profile and the table empty-state message when needed. Counts
     * cover all attempts while the list query loads at most 200 recent rows.
     * [p. 25: RecordsPanel](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     * Related: [persistence/Progress.java:27 (totalRuns)](../persistence/Progress.java#L27) (used by render);
     * [ui/Theme.java:150 (tracked)](Theme.java#L150) (used by render);
     * [Application.java:319 (page)](../Application.java#L319) (loads data before creating this table).
     */
    @Override protected void render(Graphics2D g) {
        pageChrome(g, "THE SHIFT REGISTER", "Saved locally in SQLite. Showing the latest 200 attempts for this player.");
        String[] labels = {"ON DUTY", "SHIFTS ATTEMPTED", "SHIFTS COMPLETED", "NIGHTS CLEARED"};
        String[] values = {player, Integer.toString(progress.totalRuns()), Integer.toString(progress.completedRuns()), progress.highestCompleted() + " / 5"};
        for (int i = 0; i < labels.length; i++) {
            int x = 82 + i * 286;
            Theme.tracked(g, labels[i], x, 247, 10, Theme.AMBER);
            g.setFont(Theme.sans(i == 0 ? 23 : 30));
            Theme.text(g, Theme.ellipsize(g, values[i], 260), x, 287, Theme.sans(i == 0 ? 23 : 30), Theme.PAPER);
        }
        if (empty) {
            g.setColor(new Color(15, 24, 17, 220));
            g.fillRect(82, 331, 1116, 272);
            Theme.text(g, "THE REGISTER IS STILL EMPTY.", 404, 441, Theme.heading(28), Theme.PAPER);
            Theme.text(g, "Finish a shift—or leave one—to write your first entry.", 443, 478, Theme.sans(16), Theme.MUTED);
        }
        Theme.text(g, "A matching human-readable entry is appended to shift-log.txt after every attempt.", 82, 631, Theme.sans(14), Theme.MUTED);
    }
}
