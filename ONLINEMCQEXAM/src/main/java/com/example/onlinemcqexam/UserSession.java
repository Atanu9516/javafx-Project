package com.example.onlinemcqexam;

public final class UserSession {
    private static volatile String username;

    private UserSession() {
    }

    public static void setUsername(String value) {
        username = value;
    }

    public static String getUsername() {
        return username;
    }

    public static void clear() {
        username = null;
    }
}
