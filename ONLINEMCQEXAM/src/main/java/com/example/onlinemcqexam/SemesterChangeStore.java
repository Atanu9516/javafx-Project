package com.example.onlinemcqexam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SemesterChangeStore {
    private static final String DATA_DIR = "data";
    private static final String REQUESTS_FILE = "semester_change_requests.csv";
    private static final String DELIMITER = ",";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Path RESULTS_PATH = AppPaths.resourceFile("results.csv");
    private static final Path RESULT_DETAILS_PATH = AppPaths.resourceFile("result_details.csv");
    private static final Path EXAM_HISTORY_PATH = AppPaths.resourceFile("exam_history.csv");

    private final Path requestsPath;
    private final UserStore userStore;

    public SemesterChangeStore(UserStore userStore) {
        this.userStore = userStore;
        this.requestsPath = AppPaths.appRoot().resolve(DATA_DIR).resolve(REQUESTS_FILE);
    }

    public synchronized RequestOutcome requestChange(String username, String requestedSemester) throws IOException {
        String normalizedUsername = userStore.normalizeUsername(username);
        String normalizedRequestedSemester = userStore.normalizeSemester(requestedSemester);
        if (normalizedUsername.isBlank() || normalizedRequestedSemester.isBlank()) {
            return RequestOutcome.INVALID;
        }

        UserStore.StoredUser user = userStore.findUser(normalizedUsername);
        if (user == null) {
            return RequestOutcome.NOT_FOUND;
        }

        String currentSemester = userStore.normalizeSemester(user.semester());
        if (normalizedRequestedSemester.equals(currentSemester)) {
            return RequestOutcome.NO_CHANGE;
        }

        Map<String, SemesterChangeRequest> requests = loadRequestsByUser();
        SemesterChangeRequest existing = requests.get(normalizedUsername);
        if (existing != null && existing.isPending()) {
            return RequestOutcome.ALREADY_PENDING;
        }

        requests.put(normalizedUsername, new SemesterChangeRequest(
                normalizedUsername,
                currentSemester,
                normalizedRequestedSemester,
                "PENDING",
                now(),
                ""
        ));
        writeRequests(requests);
        return RequestOutcome.CREATED;
    }

    public synchronized List<SemesterChangeRequest> listPendingRequests() throws IOException {
        return loadRequestsByUser().values().stream()
                .filter(SemesterChangeRequest::isPending)
                .toList();
    }

    public synchronized DecisionOutcome reviewRequest(String username, String requestedSemester, boolean approve) throws IOException {
        String normalizedUsername = userStore.normalizeUsername(username);
        String normalizedRequestedSemester = userStore.normalizeSemester(requestedSemester);
        if (normalizedUsername.isBlank() || normalizedRequestedSemester.isBlank()) {
            return DecisionOutcome.INVALID;
        }

        Map<String, SemesterChangeRequest> requests = loadRequestsByUser();
        SemesterChangeRequest request = requests.get(normalizedUsername);
        if (request == null || !request.isPending() || !normalizedRequestedSemester.equals(request.requestedSemester())) {
            return DecisionOutcome.NOT_FOUND;
        }

        String reviewedAt = now();
        SemesterChangeRequest updated = new SemesterChangeRequest(
                request.username(),
                request.currentSemester(),
                request.requestedSemester(),
                approve ? "APPROVED" : "DECLINED",
                request.requestedAt(),
                reviewedAt
        );
        requests.put(normalizedUsername, updated);
        if (approve) {
            userStore.updateSemester(normalizedUsername, normalizedRequestedSemester);
            resetProgressData(normalizedUsername, request.currentSemester(), normalizedRequestedSemester);
        }
        writeRequests(requests);
        return DecisionOutcome.UPDATED;
    }

    public synchronized SemesterChangeRequest findLatestForUser(String username) throws IOException {
        String normalizedUsername = userStore.normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            return null;
        }
        return loadRequestsByUser().get(normalizedUsername);
    }

    private Map<String, SemesterChangeRequest> loadRequestsByUser() throws IOException {
        ensureFileExists();
        List<String> lines = Files.readAllLines(requestsPath, StandardCharsets.UTF_8);
        Map<String, SemesterChangeRequest> requests = new LinkedHashMap<>();
        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            List<String> parts = parseCsvLine(line);
            if (parts.size() < 6) {
                continue;
            }
            SemesterChangeRequest request = new SemesterChangeRequest(
                    parts.get(0),
                    parts.get(1),
                    parts.get(2),
                    parts.get(3),
                    parts.get(4),
                    parts.get(5)
            );
            if (!request.username().isBlank()) {
                requests.put(request.username(), request);
            }
        }
        return requests;
    }

    private void writeRequests(Map<String, SemesterChangeRequest> requests) throws IOException {
        ensureFileExists();
        List<String> lines = new ArrayList<>();
        lines.add("# username,currentSemester,requestedSemester,status,requestedAt,reviewedAt");
        for (SemesterChangeRequest request : requests.values()) {
            lines.add(csv(request.username()) + DELIMITER
                    + csv(request.currentSemester()) + DELIMITER
                    + csv(request.requestedSemester()) + DELIMITER
                    + csv(request.status()) + DELIMITER
                    + csv(request.requestedAt()) + DELIMITER
                    + csv(request.reviewedAt()));
        }
        Files.write(requestsPath, lines, StandardCharsets.UTF_8);
    }

    private void ensureFileExists() throws IOException {
        Path dir = AppPaths.appRoot().resolve(DATA_DIR);
        Files.createDirectories(dir);
        if (!Files.exists(requestsPath)) {
            Files.writeString(
                    requestsPath,
                    "# username,currentSemester,requestedSemester,status,requestedAt,reviewedAt" + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
        }
    }

    private String now() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
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

    private void resetProgressData(String username, String fromSemester, String targetSemester) throws IOException {
        String normalizedUsername = userStore.normalizeUsername(username);
        String normalizedFrom = userStore.normalizeSemester(fromSemester);
        String normalizedTarget = userStore.normalizeSemester(targetSemester);
        if (normalizedUsername.isBlank()) {
            return;
        }
        clearResultsFile(normalizedUsername, normalizedFrom, normalizedTarget);
        clearResultDetailsFile(normalizedUsername, normalizedFrom, normalizedTarget);
        clearExamHistoryFile(normalizedUsername);
    }

    private void clearResultsFile(String username, String fromSemester, String targetSemester) throws IOException {
        if (Files.notExists(RESULTS_PATH)) {
            return;
        }
        List<String> lines = Files.readAllLines(RESULTS_PATH, StandardCharsets.UTF_8);
        List<String> updated = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                updated.add(line);
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (!values.isEmpty()
                    && values.size() >= 2
                    && userStore.normalizeUsername(values.get(0)).equalsIgnoreCase(username)
                    && matchesEitherSemester(userStore.normalizeSemester(values.get(1)), fromSemester, targetSemester)) {
                continue;
            }
            updated.add(line);
        }
        Files.write(RESULTS_PATH, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private void clearResultDetailsFile(String username, String fromSemester, String targetSemester) throws IOException {
        if (Files.notExists(RESULT_DETAILS_PATH)) {
            return;
        }
        List<String> lines = Files.readAllLines(RESULT_DETAILS_PATH, StandardCharsets.UTF_8);
        List<String> updated = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                updated.add(line);
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (values.size() >= 8
                    && userStore.normalizeUsername(values.get(1)).equalsIgnoreCase(username)
                    && matchesEitherSemester(userStore.normalizeSemester(values.get(6)), fromSemester, targetSemester)) {
                continue;
            }
            updated.add(line);
        }
        Files.write(RESULT_DETAILS_PATH, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private void clearExamHistoryFile(String username) throws IOException {
        if (Files.notExists(EXAM_HISTORY_PATH)) {
            return;
        }
        List<String> lines = Files.readAllLines(EXAM_HISTORY_PATH, StandardCharsets.UTF_8);
        List<String> updated = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                updated.add(line);
                continue;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith("username,coursecode,") || lower.startsWith("coursecode,date,")) {
                updated.add(line);
                continue;
            }
            List<String> values = parseCsvLine(line);
            boolean matchesFirst = !values.isEmpty() && userStore.normalizeUsername(values.get(0)).equalsIgnoreCase(username);
            boolean matchesLast = values.size() > 1 && userStore.normalizeUsername(values.get(values.size() - 1)).equalsIgnoreCase(username);
            if (matchesFirst || matchesLast) {
                continue;
            }
            updated.add(line);
        }
        Files.write(EXAM_HISTORY_PATH, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private boolean matchesEitherSemester(String value, String firstSemester, String secondSemester) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.equals(firstSemester) || value.equals(secondSemester);
    }

    public enum RequestOutcome {
        CREATED,
        INVALID,
        NOT_FOUND,
        NO_CHANGE,
        ALREADY_PENDING
    }

    public enum DecisionOutcome {
        UPDATED,
        INVALID,
        NOT_FOUND
    }
}
