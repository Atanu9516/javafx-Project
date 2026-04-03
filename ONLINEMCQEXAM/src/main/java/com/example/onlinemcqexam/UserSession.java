package com.example.onlinemcqexam;

public final class UserSession {
    private static volatile String username;
    private static volatile String semester;

    private UserSession() {
    }

    public static void setUsername(String value) {
        username = value;
    }

    public static void setSemester(String value) {
        semester = value == null ? null : value.trim();
    }

    public static String getUsername() {
        return username;
    }

    public static String getSemester() {
        return semester;
    }

    public static void clear() {
        username = null;
        semester = null;
    }
}
