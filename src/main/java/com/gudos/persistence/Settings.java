package com.gudos.persistence;

/** Immutable preferences; selecting another playerName selects another local database profile. */
public record Settings(String playerName, int volume, boolean reducedEffects, boolean fullscreen) {
    public static final Settings DEFAULT = new Settings("Night Guard", 55, false, false);

    public Settings {
        // Validate values regardless of whether they came from the UI or a manually edited text file.
        playerName = playerName == null ? "" : playerName.replaceAll("\\p{Cntrl}", "").strip();
        if (playerName.isEmpty()) playerName = "Night Guard";
        if (playerName.length() > 32) playerName = playerName.substring(0, 32);
        volume = Math.max(0, Math.min(100, volume));
    }

    // A record is immutable: changing one preference creates a replacement Settings value.
    public Settings withFullscreen(boolean value) {
        return new Settings(playerName, volume, reducedEffects, value);
    }
}
