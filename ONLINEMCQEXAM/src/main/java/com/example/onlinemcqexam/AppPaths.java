package com.example.onlinemcqexam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

final class AppPaths {
    private static final Path APP_ROOT = detectAppRoot();

    private AppPaths() {
    }

    static Path appRoot() {
        return APP_ROOT;
    }

    static Path appFile(String name) {
        return APP_ROOT.resolve(name);
    }

    static Path dataFile(String name) {
        return APP_ROOT.resolve(Paths.get("data", name));
    }

    static Path resourceFile(String name) {
        return APP_ROOT.resolve(Paths.get("src", "main", "resources", name));
    }

    static Path packageResourceFile(String name) {
        return APP_ROOT.resolve(Paths.get("src", "main", "resources", "com", "example", "onlinemcqexam", name));
    }

    private static Path detectAppRoot() {
        List<Path> candidates = List.of(
                Paths.get("ONLINEMCQEXAM"),
                Paths.get(".")
        );
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized.resolve("src").resolve("main").resolve("resources"))
                    && Files.exists(normalized.resolve("pom.xml"))) {
                return normalized;
            }
        }
        return Paths.get(".").toAbsolutePath().normalize();
    }
}
