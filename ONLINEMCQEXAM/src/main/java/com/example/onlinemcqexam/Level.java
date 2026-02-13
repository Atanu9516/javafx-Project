package com.example.onlinemcqexam;

public enum Level {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    private final String displayName;

    Level(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Level fromCsv(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "BEGINNER" -> BEGINNER;
            case "INTERMEDIATE" -> INTERMEDIATE;
            case "ADVANCED" -> ADVANCED;
            default -> null;
        };
    }
}
