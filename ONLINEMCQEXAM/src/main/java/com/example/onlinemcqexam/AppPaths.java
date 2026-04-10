package com.example.onlinemcqexam;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

final class AppPaths {
    private static final String APP_DIR_NAME = "OnlineMCQExam";
    private static final String PACKAGE_RESOURCE_PREFIX = "/com/example/onlinemcqexam/";
    private static final String HOME_OVERRIDE_PROPERTY = "onlinemcqexam.home";
    private static final String HOME_OVERRIDE_ENV = "ONLINEMCQEXAM_HOME";

    private static final Path APP_ROOT = detectAppRoot();
    private static final Path DATA_DIR = APP_ROOT.resolve("data");

    // CSVs expected to exist in writable runtime storage for first-run demo visibility.
    private static final String[] REQUIRED_CSV_FILES = {
            "users.csv",
            "terms.csv",
            "courses.csv",
            "questions.csv",
            "exams.csv",
            "exam_history.csv",
            "results.csv",
            "result_details.csv",
            "schedule.csv"
    };

    private static volatile boolean initialized;

    private AppPaths() {
    }

    static synchronized void initialize() {
        if (initialized) {
            return;
        }

        ensureDirectory(APP_ROOT);
        ensureDirectory(DATA_DIR);
        seedDemoData();
        initialized = true;
    }

    /**
     * Seeds all demo data files from resources to the data directory on first launch.
     * On subsequent launches, existing files are not overwritten to preserve user data.
     */
    private static void seedDemoData() {
        List<String> failures = new ArrayList<>();
        for (String filename : REQUIRED_CSV_FILES) {
            try {
                ensureSeeded(filename);
            } catch (RuntimeException ex) {
                failures.add(filename + " (" + ex.getMessage() + ")");
            }
        }

        if (!failures.isEmpty()) {
            throw new IllegalStateException("Failed to seed required CSV files: " + String.join("; ", failures));
        }
    }

    /**
     * Seeds a single demo data file from resources if it doesn't exist.
     * Uses package-scoped resources if available, falls back to root resources.
     *
     * @param filename the name of the demo data file to seed
     */
    static void ensureSeeded(String filename) {
        Path destination = DATA_DIR.resolve(filename);

        // Preserve existing user data: never overwrite a non-empty runtime file.
        if (isNonEmptyFile(destination)) {
            return;
        }

        List<String> candidates = resourceCandidates(filename);
        for (String resourcePath : candidates) {
            try (InputStream resourceStream = AppPaths.class.getResourceAsStream(resourcePath)) {
                if (resourceStream == null) {
                    continue;
                }

                Files.createDirectories(destination.getParent());
                Files.copy(resourceStream, destination, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[AppPaths] Seeded runtime data file '" + filename
                        + "' from classpath resource '" + resourcePath + "'.");
                return;
            } catch (IOException ex) {
                throw new UncheckedIOException(
                        "Unable to seed '" + filename + "' from resource '" + resourcePath + "'", ex);
            }
        }

        if (isNonEmptyFile(destination)) {
            return;
        }

        throw new IllegalStateException(
                "Packaged seed resource not found for '" + filename + "'. Tried: " + String.join(", ", candidates));
    }

    /**
     * Returns classpath resource candidates in priority order.
     */
    private static List<String> resourceCandidates(String filename) {
        List<String> candidates = new ArrayList<>();
        candidates.add(PACKAGE_RESOURCE_PREFIX + filename);
        candidates.add("/" + filename);

        if ("questions.csv".equalsIgnoreCase(filename)) {
            candidates.add(PACKAGE_RESOURCE_PREFIX + "question.csv");
            candidates.add("/question.csv");
        } else if ("question.csv".equalsIgnoreCase(filename)) {
            candidates.add(PACKAGE_RESOURCE_PREFIX + "questions.csv");
            candidates.add("/questions.csv");
        }

        return candidates;
    }

    static Path appRoot() {
        initialize();
        return APP_ROOT;
    }

    static Path appFile(String name) {
        initialize();
        return ensureParent(APP_ROOT.resolve(name));
    }

    static Path dataFile(String name) {
        initialize();
        return ensureParent(DATA_DIR.resolve(name));
    }

    // Legacy compatibility: these represent mutable runtime files seeded from packaged resources.
    static Path resourceFile(String name) {
        initialize();
        ensureSeeded(name);
        return DATA_DIR.resolve(name);
    }

    // Legacy compatibility: packaged CSVs are copied to writable runtime storage on first access.
    static Path packageResourceFile(String name) {
        initialize();
        ensureSeeded(name);
        return DATA_DIR.resolve(name);
    }

    static URL bundledResource(String name) {
        return AppPaths.class.getResource("/" + name);
    }

    static URL bundledPackageResource(String name) {
        return AppPaths.class.getResource(PACKAGE_RESOURCE_PREFIX + name);
    }

    static InputStream bundledResourceStream(String name) {
        return AppPaths.class.getResourceAsStream("/" + name);
    }

    static InputStream bundledPackageResourceStream(String name) {
        return AppPaths.class.getResourceAsStream(PACKAGE_RESOURCE_PREFIX + name);
    }

    private static Path detectAppRoot() {
        String propertyOverride = trimToNull(System.getProperty(HOME_OVERRIDE_PROPERTY));
        if (propertyOverride != null) {
            return Paths.get(propertyOverride).toAbsolutePath().normalize();
        }

        String envOverride = trimToNull(System.getenv(HOME_OVERRIDE_ENV));
        if (envOverride != null) {
            return Paths.get(envOverride).toAbsolutePath().normalize();
        }

        String localAppData = trimToNull(System.getenv("LOCALAPPDATA"));
        if (localAppData != null) {
            return Paths.get(localAppData, APP_DIR_NAME).toAbsolutePath().normalize();
        }

        String appData = trimToNull(System.getenv("APPDATA"));
        if (appData != null) {
            return Paths.get(appData, APP_DIR_NAME).toAbsolutePath().normalize();
        }

        return Paths.get(System.getProperty("user.home"), ".onlinemcqexam").toAbsolutePath().normalize();
    }

    private static boolean isNonEmptyFile(Path path) {
        try {
            return Files.exists(path) && Files.size(path) > 0L;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to inspect runtime data file: " + path, ex);
        }
    }

    private static Path ensureParent(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
            // Parent creation is best-effort; callers will surface actual IO errors.
        }
        return path;
    }

    private static void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to create directory: " + path, ex);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
