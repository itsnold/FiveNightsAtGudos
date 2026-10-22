package com.gudos.persistence;

public record Settings(String playerName, int volume, boolean reducedEffects, boolean fullscreen) {
    public static final Settings DEFAULT = new Settings("Night Guard", 55, false, false);

    public Settings {
        playerName = playerName == null ? "" : playerName.replaceAll("\\p{Cntrl}", "").strip();
        if (playerName.isEmpty()) playerName = "Night Guard";
        if (playerName.length() > 32) playerName = playerName.substring(0, 32);
        volume = Math.max(0, Math.min(100, volume));
    }

    public Settings withFullscreen(boolean value) {
        return new Settings(playerName, volume, reducedEffects, value);
    }
}
