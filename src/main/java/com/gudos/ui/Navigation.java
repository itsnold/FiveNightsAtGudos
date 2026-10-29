package com.gudos.ui;

import com.gudos.persistence.Settings;

/** Screen requests implemented by Application; panels need not know window or persistence details. */
public interface Navigation {
    enum Page { EXTRAS, NIGHTS, GUIDE, SETTINGS, RECORDS, CREDITS }
    void menu();
    void page(Page page);
    void startNight(int night);
    void saveSettings(Settings settings);
    void resumeShift();
    void leaveShift();
    void quit();
}
