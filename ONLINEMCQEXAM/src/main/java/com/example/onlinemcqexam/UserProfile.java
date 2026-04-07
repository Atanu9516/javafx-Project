package com.example.onlinemcqexam;

public record UserProfile(String username, String semester) {
    public UserProfile {
        username = username == null ? "" : username.trim();
        semester = semester == null ? "" : semester.trim();
    }
}
