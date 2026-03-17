package com.example.onlinemcqexam;

import java.io.IOException;

public final class NetworkUserService implements UserService {
    private final ExamClient client;

    public NetworkUserService(ExamClient client) {
        this.client = client;
    }

    @Override
    public boolean register(String username, String password) throws IOException {
        ExamClient.ServerResponse response = client.register(username, password);
        if (response.ok()) {
            return true;
        }
        if ("EXISTS".equalsIgnoreCase(response.message())) {
            return false;
        }
        throw new IOException("Server error: " + response.message());
    }

    @Override
    public boolean authenticate(String username, String password) throws IOException {
        ExamClient.ServerResponse response = client.login(username, password);
        if (response.ok()) {
            return true;
        }
        if ("INVALID".equalsIgnoreCase(response.message())) {
            return false;
        }
        throw new IOException("Server error: " + response.message());
    }

    @Override
    public String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }
}
