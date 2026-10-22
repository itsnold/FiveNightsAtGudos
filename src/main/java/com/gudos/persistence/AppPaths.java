package com.gudos.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record AppPaths(Path directory) {
    public AppPaths {
        directory = directory.toAbsolutePath().normalize();
    }

    public static AppPaths resolve() throws IOException {
        String override = System.getProperty("gudo.dataDir");
        String local = System.getenv("LOCALAPPDATA");
        Path directory = override != null && !override.isBlank() ? Path.of(override)
                : local != null && !local.isBlank() ? Path.of(local, "FiveNightsAtGudos")
                : Path.of(System.getProperty("user.home"), ".five-nights-at-gudos");
        Files.createDirectories(directory);
        return new AppPaths(directory);
    }

    public Path database() { return directory.resolve("gudo.db"); }
    public Path settings() { return directory.resolve("settings.txt"); }
    public Path shiftLog() { return directory.resolve("shift-log.txt"); }
}
