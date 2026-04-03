package com.example.onlinemcqexam;

import java.io.IOException;

public final class NetworkUserService implements UserService {
    private final ExamClient client;
    private final UserStore localUserStore;

    public NetworkUserService(ExamClient client) {
        this.client = client;
        this.localUserStore = new UserStore();
    }

    @Override
    public boolean register(String username, String password, String semester) throws IOException {
        ExamClient.ServerResponse response = client.register(username, password, semester);
        if (response.ok()) {
            syncLocalUser(username, password, semester);
            return true;
        }
        if ("EXISTS".equalsIgnoreCase(response.message())) {
            syncLocalUser(username, password, semester);
            return false;
        }
        if ("INVALID".equalsIgnoreCase(response.message())) {
            throw new IOException("Select a valid semester.");
        }
        throw new IOException("Server error: " + response.message());
    }

    @Override
    public UserProfile authenticate(String username, String password) throws IOException {
        ExamClient.ServerResponse response = client.login(username, password);
        if (response.ok()) {
            String semester = resolveSemester(normalizeUsername(username), response.message());
            if (semester.isBlank()) {
                throw new IOException("No semester is assigned to this account.");
            }
            return new UserProfile(normalizeUsername(username), semester);
        }
        if ("INVALID".equalsIgnoreCase(response.message())) {
            return null;
        }
        if ("NO_SEMESTER".equalsIgnoreCase(response.message())) {
            String semester = resolveSemester(normalizeUsername(username), "");
            if (!semester.isBlank()) {
                return new UserProfile(normalizeUsername(username), semester);
            }
            throw new IOException("No semester is assigned to this account.");
        }
        throw new IOException("Server error: " + response.message());
    }

    @Override
    public String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private void syncLocalUser(String username, String password, String semester) {
        try {
            localUserStore.upsertUser(username, password, semester);
        } catch (IOException ignored) {
            // Network registration remains the source of truth; local sync is a fallback for semester lookup.
        }
    }

    private String resolveSemester(String username, String serverSemester) {
        String normalizedServerSemester = localUserStore.normalizeSemester(serverSemester);
        if (!normalizedServerSemester.isBlank()) {
            return normalizedServerSemester;
        }
        try {
            UserStore.StoredUser localUser = localUserStore.findUser(username);
            if (localUser != null) {
                return localUserStore.normalizeSemester(localUser.semester());
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }
}
