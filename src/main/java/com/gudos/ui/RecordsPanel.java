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

/** Read-only table built from attempts already loaded by Application's storage worker. */
public final class RecordsPanel extends BackdropPanel {
    private final String player;
    private final Progress progress;
    private final boolean empty;

    public RecordsPanel(AssetManager assets, Navigation navigation, String player, Progress progress, List<RunRecord> runs) {
        super(assets);
        this.player = player;
        this.progress = progress;
        empty = runs.isEmpty();
        backButton(navigation);
        if (empty) return;
        // Stored Instants represent absolute timestamps; display them in this computer's local timezone.
        DateTimeFormatter date = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm").withZone(ZoneId.systemDefault());
        DefaultTableModel model = new DefaultTableModel(new String[]{"FINISHED", "NIGHT", "RESULT", "TIME ON DUTY", "POWER LEFT"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        // Convert typed game results into the five columns shown to the player. No SQL runs in this panel.
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

    // Summary counters cover all this profile's attempts, even though only the newest 200 rows are loaded.
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
