package com.example.onlinemcqexam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserStore {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = "users.csv";
    private static final String DELIMITER = ",";
    private static final Path RESULTS_PATH = AppPaths.resourceFile("results.csv");
    private static final Set<String> VALID_SEMESTERS = Set.of(
            "1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2"
    );
    private static final Map<String, DemoUser> DEMO_USERS = buildDemoUsers();

    private final Path usersPath;
    private final Object lock = new Object();  // Lock for file operations

    public UserStore() {
        this.usersPath = AppPaths.dataFile(USERS_FILE);
        ensureDemoAccounts();
    }

    private static Map<String, DemoUser> buildDemoUsers() {
        Map<String, DemoUser> users = new LinkedHashMap<>();
        users.put("student1", new DemoUser("student1", "password", "1-1"));
        users.put("student2", new DemoUser("student2", "student2", "1-2"));
        users.put("student3", new DemoUser("student3", "student3", "2-1"));
        users.put("teacher1", new DemoUser("teacher1", "teacher1", "1-1"));
        users.put("admin", new DemoUser("admin", "admin123", "1-1"));
        return Collections.unmodifiableMap(users);
    }

    private void ensureDemoAccounts() {
        synchronized (lock) {
            try {
                ensureFileExists();
                Map<String, StoredUser> users = loadUsers();
                boolean changed = false;
                for (DemoUser demoUser : DEMO_USERS.values()) {
                    String username = normalizeUsername(demoUser.username());
                    String semester = normalizeSemester(demoUser.semester());
                    String expectedHash = hashPassword(demoUser.password());
                    StoredUser current = users.get(username);
                    if (current == null
                            || !expectedHash.equals(current.passwordHash())
                            || !semester.equals(current.semester())) {
                        users.put(username, new StoredUser(username, expectedHash, semester));
                        changed = true;
                    }
                }
                if (changed) {
                    writeUsers(users);
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to initialize demo users", ex);
            }
        }
    }

    public Map<String, StoredUser> loadUsers() throws IOException {
        synchronized (lock) {
            ensureFileExists();
            List<String> lines = Files.readAllLines(usersPath, StandardCharsets.UTF_8);
            Map<String, StoredUser> users = new LinkedHashMap<>();
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 2) {
                    continue;
                }
                String username = normalizeUsername(parts.get(0));
                String passwordHash = parts.get(1).trim();
                String semester = parts.size() >= 3 ? normalizeSemester(parts.get(2)) : inferSemester(username);
                if (username.isBlank() || passwordHash.isBlank()) {
                    continue;
                }
                users.put(username, new StoredUser(username, passwordHash, semester));
            }
            return users;
        }
    }

    public boolean register(String username, String password, String semester) throws IOException {
        synchronized (lock) {
            String normalized = normalizeUsername(username);
            String normalizedSemester = normalizeSemester(semester);
            if (normalized.isBlank() || normalizedSemester.isBlank()) {
                return false;
            }
            ensureFileExists();
            Map<String, StoredUser> users = loadUsers();
            if (users.containsKey(normalized)) {
                return false;
            }
            String hashed = hashPassword(password);
            String record = csv(normalized) + DELIMITER + csv(hashed) + DELIMITER + csv(normalizedSemester) + System.lineSeparator();
            Files.writeString(usersPath, record, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
            return true;
        }
    }

    public StoredUser authenticate(String username, String password) throws IOException {
        synchronized (lock) {
            String normalized = normalizeUsername(username);
            Map<String, StoredUser> users = loadUsers();
            StoredUser user = users.get(normalized);
            if (user == null) {
                return null;
            }
            String inputHash = hashPassword(password);
            return user.passwordHash().equals(inputHash) ? user : null;
        }
    }

    public String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    public StoredUser findUser(String username) throws IOException {
        synchronized (lock) {
            String normalized = normalizeUsername(username);
            if (normalized.isBlank()) {
                return null;
            }
            return loadUsers().get(normalized);
        }
    }

    public void upsertUser(String username, String password, String semester) throws IOException {
        synchronized (lock) {
            String normalized = normalizeUsername(username);
            String normalizedSemester = normalizeSemester(semester);
            if (normalized.isBlank() || normalizedSemester.isBlank()) {
                return;
            }
            ensureFileExists();
            Map<String, StoredUser> users = loadUsers();
            StoredUser existing = users.get(normalized);
            String passwordHash = existing != null && existing.passwordHash() != null && !existing.passwordHash().isBlank()
                    ? existing.passwordHash()
                    : hashPassword(password == null ? "" : password);
            users.put(normalized, new StoredUser(normalized, passwordHash, normalizedSemester));
            writeUsers(users);
        }
    }

    public void updateSemester(String username, String semester) throws IOException {
        synchronized (lock) {
            String normalized = normalizeUsername(username);
            String normalizedSemester = normalizeSemester(semester);
            if (normalized.isBlank() || normalizedSemester.isBlank()) {
                return;
            }
            ensureFileExists();
            Map<String, StoredUser> users = loadUsers();
            StoredUser existing = users.get(normalized);
            if (existing == null) {
                return;
            }
            users.put(normalized, new StoredUser(normalized, existing.passwordHash(), normalizedSemester));
            writeUsers(users);
        }
    }

    public String normalizeSemester(String semester) {
        String normalized = semester == null ? "" : semester.trim();
        return VALID_SEMESTERS.contains(normalized) ? normalized : "";
    }

    private void ensureFileExists() throws IOException {
        Path dir = AppPaths.appRoot().resolve(DATA_DIR);
        Files.createDirectories(dir);
        if (!Files.exists(usersPath)) {
            Files.writeString(usersPath, "# username,hashedPassword,semester" + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    private void writeUsers(Map<String, StoredUser> users) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# username,hashedPassword,semester");
        for (StoredUser user : users.values()) {
            if (user == null) {
                continue;
            }
            lines.add(csv(user.username()) + DELIMITER + csv(user.passwordHash()) + DELIMITER + csv(user.semester()));
        }
        Files.write(usersPath, lines, StandardCharsets.UTF_8);
    }

    private String inferSemester(String username) {
        if (username == null || username.isBlank() || Files.notExists(RESULTS_PATH)) {
            return "";
        }
        Map<String, Integer> countBySemester = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(RESULTS_PATH, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase();
                if (lower.startsWith("username,term,course,")) {
                    continue;
                }
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 2) {
                    continue;
                }
                if (!normalizeUsername(parts.get(0)).equalsIgnoreCase(username)) {
                    continue;
                }
                String semester = normalizeSemester(parts.get(1));
                if (semester.isBlank()) {
                    continue;
                }
                countBySemester.merge(semester, 1, Integer::sum);
            }
        } catch (IOException ignored) {
            return "";
        }

        String winner = "";
        int winnerCount = 0;
        for (Map.Entry<String, Integer> entry : countBySemester.entrySet()) {
            if (entry.getValue() > winnerCount) {
                winner = entry.getKey();
                winnerCount = entry.getValue();
            }
        }
        return winner;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        values.add(current.toString());
        return values;
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"")) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
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

    public record StoredUser(String username, String passwordHash, String semester) {
    }

    private record DemoUser(String username, String password, String semester) {
    }
}
