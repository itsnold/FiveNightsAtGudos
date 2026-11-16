package com.gudos.ui;

import com.gudos.persistence.Settings;

/** Screen requests implemented by Application; panels need not know window or persistence details. [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20). Related: [Application.java:110 (Application)](../Application.java#L110) (implements this interface); [ui/MenuPanel.java:104 (MenuPanel)](MenuPanel.java#L104) (screen issuing requests). */
/*
 * Navigation
 *
 * Related: [Application.java:110 (Application)](../Application.java#L110) (implements this interface);
 * [ui/MenuPanel.java:104 (MenuPanel)](MenuPanel.java#L104) (screen issuing requests);
 * [ui/PausePanel.java:42 (PausePanel)](PausePanel.java#L42) (resume and leave requests).
 *
 * Purpose: Defines the small screen-facing facade contract implemented by Application.
 *
 * Execution flow: MenuPanel, ExtrasPanel, NightSelectPanel, SettingsPanel, PausePanel and
 * ResultPanel depend on this interface.
 *
 * Design rationale: Screens request intent without knowing JFrame, storage workers,
 * GameSession construction or AudioService internals. An interface alone is not proof of Facade; the
 * hidden orchestration in Application supplies the evidence.
 *
 * State: Page enumerates EXTRAS, NIGHTS, GUIDE, SETTINGS, RECORDS, CREDITS. Operations
 * include menu, page, startNight, saveSettings, resumeShift, leaveShift, quit.
 *
 * Summary: startNight is the facade operation used by New Game, Continue, night selection and
 * replay/next-night buttons. There is no method named facade and no separate newGame method.
 * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
 */
public interface Navigation {
    /*
     * Page
     *
     * Named secondary-screen requests understood by Application.page.
     * [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:110 (Application)](../Application.java#L110) (implements this interface);
     * [ui/MenuPanel.java:104 (MenuPanel)](MenuPanel.java#L104) (screen issuing requests);
     * [ui/PausePanel.java:42 (PausePanel)](PausePanel.java#L42) (resume and leave requests).
     */
    enum Page { EXTRAS, NIGHTS, GUIDE, SETTINGS, RECORDS, CREDITS }
    /*
     * Request the main menu through Application. Active attempts and incomplete saves may trigger
     * the coordinator abandonment/save flow; the caller does not manipulate cards directly.
     * [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:285 (menu)](../Application.java#L285) (implementation);
     * [ui/BackdropPanel.java:85 (backButton)](BackdropPanel.java#L85) (shared Back-button caller);
     * [ui/ResultPanel.java:50 (ResultPanel)](ResultPanel.java#L50) (result-menu caller).
     */
    void menu();
    /*
     * Request one Page enum. Application selects the panel and asynchronously loads records when
     * needed; panels do not issue SQL themselves.
     * [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:319 (page)](../Application.java#L319) (implementation);
     * [ui/MenuPanel.java:104 (MenuPanel)](MenuPanel.java#L104) (Extras caller);
     * [ui/ExtrasPanel.java:37 (ExtrasPanel)](ExtrasPanel.java#L37) (secondary-page callers).
     */
    void page(Page page);
    /*
     * Facade contract for starting an eligible fresh attempt. New Game passes 1; Continue passes
     * progress.nextNight; selected/replay/next buttons pass their chosen night. Application hides
     * validation, model/view construction, callback wiring, navigation and audio coordination.
     * Related: [Application.java:358 (startNight)](../Application.java#L358) (implementation);
     * [ui/MenuPanel.java:109 (startNight call)](MenuPanel.java#L109) (New Game caller); [ui/MenuPanel.java:110 (startNight call)](MenuPanel.java#L110) (Continue caller);
     * [ui/NightSelectPanel.java:48 (startNight call)](NightSelectPanel.java#L48) (night-selection caller);
     * [ui/ResultPanel.java:55 (startNight call)](ResultPanel.java#L55) (replay and next-night callers).
     * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     */
    void startNight(int night);
    /*
     * Submit an immutable Settings value to Application for background preference/profile work and
     * later application. A screen never writes settings.txt directly.
     * [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:621 (saveSettings)](../Application.java#L621) (implementation);
     * [ui/SettingsPanel.java:63 (SettingsPanel)](SettingsPanel.java#L63) (Apply button caller).
     */
    void saveSettings(Settings settings);
    /*
     * Request resumption of the same paused in-memory normal shift. This is not a database
     * checkpoint restore.
     * [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:500 (resumeShift)](../Application.java#L500) (resume implementation);
     * [Application.java:516 (leaveShift)](../Application.java#L516) (leave implementation);
     * [ui/PausePanel.java:42 (PausePanel)](PausePanel.java#L42) (pause-screen callers).
     */
    void resumeShift();
    /*
     * Request confirmed early departure. Application records ABANDONED and waits for saving before
     * returning to the menu.
     * [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:500 (resumeShift)](../Application.java#L500) (resume implementation);
     * [Application.java:516 (leaveShift)](../Application.java#L516) (leave implementation);
     * [ui/PausePanel.java:42 (PausePanel)](PausePanel.java#L42) (pause-screen callers).
     */
    void leaveShift();
    /*
     * Request orderly exit. Application handles active/incomplete attempts and resource cleanup
     * before disposing the window.
     * [p. 20: Navigation](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-20).
     * Related: [Application.java:703 (quit)](../Application.java#L703) (implementation);
     * [ui/MenuPanel.java:104 (MenuPanel)](MenuPanel.java#L104) (Quit button caller).
     */
    void quit();
}
