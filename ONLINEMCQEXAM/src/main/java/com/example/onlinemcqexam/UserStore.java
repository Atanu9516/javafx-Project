package com.example.onlinemcqexam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserStore {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = "users.csv";
    private static final String DELIMITER = ",";

    private final Path usersPath;

    public UserStore() {
        this.usersPath = Paths.get(DATA_DIR, USERS_FILE);
    }

    public Map<String, String> loadUsers() throws IOException {
        ensureFileExists();
        List<String> lines = Files.readAllLines(usersPath, StandardCharsets.UTF_8);
        Map<String, String> users = new HashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(DELIMITER, 2);
            if (parts.length == 2) {
                users.put(parts[0], parts[1]);
            }
        }
        return users;
    }

    public boolean register(String username, String password) throws IOException {
        String normalized = normalizeUsername(username);
        ensureFileExists();
        Map<String, String> users = loadUsers();
        if (users.containsKey(normalized)) {
            return false;
        }
        String hashed = hashPassword(password);
        String record = normalized + DELIMITER + hashed + System.lineSeparator();
        Files.writeString(usersPath, record, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        return true;
    }

    public boolean authenticate(String username, String password) throws IOException {
        String normalized = normalizeUsername(username);
        Map<String, String> users = loadUsers();
        String storedHash = users.get(normalized);
        if (storedHash == null) {
            return false;
        }
        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

    public String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private void ensureFileExists() throws IOException {
        Path dir = Paths.get(DATA_DIR);
        Files.createDirectories(dir);
        if (!Files.exists(usersPath)) {
            Files.writeString(usersPath, "# username,hashedPassword" + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
