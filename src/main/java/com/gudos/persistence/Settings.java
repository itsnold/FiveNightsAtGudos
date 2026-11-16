package com.gudos.persistence;

/** Immutable preferences; selecting another playerName selects another local database profile. Reviewer (42-page edition): [p. 25: Settings](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25). */
/*
 * Settings
 *
 * Purpose: Validates and carries immutable user preferences.
 *
 * Execution flow: SettingsStore loads it; SettingsPanel creates a replacement; Application persists
 * and applies it.
 *
 * Design rationale: Sanitize control characters, trim/fallback/cap the name, and
 * clamp volume at the value boundary as well as the UI. A record replacement makes updates explicit.
 *
 * State: DEFAULT: Night Guard, 55 percent volume, reduced effects false, fullscreen
 * false. Name length is at most 32 Java characters.
 *
 * Summary: Changing playerName selects a local profile, not an authenticated online account.
 * withFullscreen preserves the other three preferences.
 * Reviewer (42-page edition): [p. 25: Settings](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
 */
public record Settings(String playerName, int volume, boolean reducedEffects, boolean fullscreen) {
    public static final Settings DEFAULT = new Settings("Night Guard", 55, false, false);

    /*
     * Strip control characters/outer whitespace from playerName, use Night Guard for empty input,
     * cap at32 and clamp volume to0..100. This compact record constructor runs regardless of whether
     * values came from UI or a file.
     * Reviewer (42-page edition): [p. 25: Settings](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     */
    public Settings {
        // Validate values regardless of whether they came from the UI or a manually edited text file. | Reviewer (42-page edition): [p. 25: Settings](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
        playerName = playerName == null ? "" : playerName.replaceAll("\\p{Cntrl}", "").strip();
        if (playerName.isEmpty()) playerName = "Night Guard";
        if (playerName.length() > 32) playerName = playerName.substring(0, 32);
        volume = Math.max(0, Math.min(100, volume));
    }

    // A record is immutable: changing one preference creates a replacement Settings value. | Reviewer (42-page edition): [p. 25: Settings](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
    /*
     * Create a new Settings with the supplied fullscreen value and the same name/volume/effects.
     * Records are immutable values, so replacing the value is the update mechanism.
     * Reviewer (42-page edition): [p. 25: Settings](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-25).
     */
    public Settings withFullscreen(boolean value) {
        return new Settings(playerName, volume, reducedEffects, value);
    }
}
