package com.example.onlinemcqexam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class FriendStore {
    private static final int MAX_CHAT_MESSAGES = 500;
    private static final String CHAT_METADATA_SEPARATOR = "\u001F";
    private static final Path FRIENDS_PATH = AppPaths.dataFile("friends.csv");
    private static final Path PENDING_PATH = AppPaths.dataFile("friend_requests.csv");
    private static final Path CHATS_PATH = AppPaths.dataFile("chat_history.csv");

    private final Map<String, Set<String>> friends = new HashMap<>();
    private final Map<String, Set<String>> pending = new HashMap<>();
    private final Map<String, List<ChatMessage>> chats = new HashMap<>();

    public FriendStore() {
        loadState();
    }

    public synchronized FriendRequestStatus sendRequest(String from, String to) {
        String sender = normalize(from);
        String target = normalize(to);
        if (sender.isBlank() || target.isBlank() || sender.equals(target)) {
            return FriendRequestStatus.INVALID;
        }
        if (areFriends(sender, target)) {
            return FriendRequestStatus.ALREADY;
        }
        Set<String> incoming = pending.getOrDefault(sender, new HashSet<>());
        if (incoming.contains(target)) {
            incoming.remove(target);
            if (incoming.isEmpty()) {
                pending.remove(sender);
            }
            addFriendship(sender, target);
            persistQuietly();
            return FriendRequestStatus.ACCEPTED;
        }
        Set<String> targetPending = pending.computeIfAbsent(target, key -> new HashSet<>());
        if (!targetPending.add(sender)) {
            return FriendRequestStatus.ALREADY;
        }
        persistQuietly();
        return FriendRequestStatus.SENT;
    }

    public synchronized List<String> listPending(String user) {
        String normalized = normalize(user);
        Set<String> requests = pending.getOrDefault(normalized, Set.of());
        return new ArrayList<>(new TreeSet<>(requests));
    }

    public synchronized boolean acceptRequest(String user, String from) {
        String normalized = normalize(user);
        String sender = normalize(from);
        Set<String> requests = pending.get(normalized);
        if (requests == null || !requests.remove(sender)) {
            return false;
        }
        if (requests.isEmpty()) {
            pending.remove(normalized);
        }
        addFriendship(normalized, sender);
        persistQuietly();
        return true;
    }

    public synchronized boolean declineRequest(String user, String from) {
        String normalized = normalize(user);
        String sender = normalize(from);
        Set<String> requests = pending.get(normalized);
        if (requests == null || !requests.remove(sender)) {
            return false;
        }
        if (requests.isEmpty()) {
            pending.remove(normalized);
        }
        persistQuietly();
        return true;
    }

    public synchronized List<String> listFriends(String user) {
        String normalized = normalize(user);
        Set<String> list = friends.getOrDefault(normalized, Set.of());
        return new ArrayList<>(new TreeSet<>(list));
    }

    public synchronized boolean sendChat(String from, String to, String text) {
        String sender = normalize(from);
        String target = normalize(to);
        if (sender.isBlank() || target.isBlank() || text == null || text.isBlank()) {
            return false;
        }
        if (!areFriends(sender, target)) {
            return false;
        }
        String key = chatKey(sender, target);
        List<ChatMessage> history = chats.computeIfAbsent(key, k -> new ArrayList<>());
        history.add(new ChatMessage(sender, text.trim(), System.currentTimeMillis()));
        if (history.size() > MAX_CHAT_MESSAGES) {
            history.remove(0);
        }
        persistQuietly();
        return true;
    }

    public synchronized List<String> getChatHistory(String user, String friend) {
        String normalized = normalize(user);
        String other = normalize(friend);
        if (!areFriends(normalized, other)) {
            return List.of();
        }
        String key = chatKey(normalized, other);
        List<ChatMessage> history = chats.getOrDefault(key, List.of());
        List<String> display = new ArrayList<>(history.size());
        for (ChatMessage message : history) {
            display.add(message.from + CHAT_METADATA_SEPARATOR + message.sentAtEpochMillis + CHAT_METADATA_SEPARATOR + message.text);
        }
        return display;
    }

    private void loadState() {
        try {
            ensureFilesExist();
            loadFriends();
            loadPendingRequests();
            loadChats();
        } catch (IOException ex) {
            System.err.println("Unable to load friend store data: " + ex.getMessage());
        }
    }

    private void ensureFilesExist() throws IOException {
        Path dataDir = AppPaths.appRoot().resolve("data");
        Files.createDirectories(dataDir);
        ensureFileWithHeader(FRIENDS_PATH, "# user_a,user_b");
        ensureFileWithHeader(PENDING_PATH, "# target_user,from_user");
        ensureFileWithHeader(CHATS_PATH, "# user_a,user_b,sender,timestamp,text_b64");
    }

    private void ensureFileWithHeader(Path path, String header) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.writeString(path, header + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private void loadFriends() throws IOException {
        for (String line : Files.readAllLines(FRIENDS_PATH, StandardCharsets.UTF_8)) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (values.size() < 2) {
                continue;
            }
            String a = normalize(values.get(0));
            String b = normalize(values.get(1));
            if (a.isBlank() || b.isBlank() || a.equals(b)) {
                continue;
            }
            addFriendship(a, b);
        }
    }

    private void loadPendingRequests() throws IOException {
        for (String line : Files.readAllLines(PENDING_PATH, StandardCharsets.UTF_8)) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (values.size() < 2) {
                continue;
            }
            String target = normalize(values.get(0));
            String from = normalize(values.get(1));
            if (target.isBlank() || from.isBlank() || target.equals(from)) {
                continue;
            }
            pending.computeIfAbsent(target, key -> new HashSet<>()).add(from);
        }
    }

    private void loadChats() throws IOException {
        for (String line : Files.readAllLines(CHATS_PATH, StandardCharsets.UTF_8)) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (values.size() < 5) {
                continue;
            }
            String a = normalize(values.get(0));
            String b = normalize(values.get(1));
            String sender = normalize(values.get(2));
            long timestamp;
            try {
                timestamp = Long.parseLong(values.get(3).trim());
            } catch (NumberFormatException ex) {
                continue;
            }
            String text = NetworkProtocol.decode(values.get(4));
            if (a.isBlank() || b.isBlank() || sender.isBlank() || text.isBlank()) {
                continue;
            }
            chats.computeIfAbsent(chatKey(a, b), key -> new ArrayList<>())
                    .add(new ChatMessage(sender, text, timestamp));
        }
    }

    private void persistQuietly() {
        try {
            saveState();
        } catch (IOException ex) {
            System.err.println("Unable to persist friend store data: " + ex.getMessage());
        }
    }

    private void saveState() throws IOException {
        ensureFilesExist();
        writeFriends();
        writePendingRequests();
        writeChats();
    }

    private void writeFriends() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# user_a,user_b");
        Set<String> seenPairs = new LinkedHashSet<>();
        for (String user : new TreeSet<>(friends.keySet())) {
            for (String friend : new TreeSet<>(friends.getOrDefault(user, Set.of()))) {
                String pairKey = chatKey(user, friend);
                if (!seenPairs.add(pairKey)) {
                    continue;
                }
                String[] pair = pairKey.split("\\|", 2);
                lines.add(csv(pair[0]) + "," + csv(pair[1]));
            }
        }
        Files.write(FRIENDS_PATH, lines, StandardCharsets.UTF_8);
    }

    private void writePendingRequests() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# target_user,from_user");
        for (String target : new TreeSet<>(pending.keySet())) {
            for (String from : new TreeSet<>(pending.getOrDefault(target, Set.of()))) {
                lines.add(csv(target) + "," + csv(from));
            }
        }
        Files.write(PENDING_PATH, lines, StandardCharsets.UTF_8);
    }

    private void writeChats() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# user_a,user_b,sender,timestamp,text_b64");
        for (String key : new TreeSet<>(chats.keySet())) {
            String[] pair = key.split("\\|", 2);
            List<ChatMessage> history = chats.getOrDefault(key, List.of());
            for (ChatMessage message : history) {
                lines.add(csv(pair[0]) + ","
                        + csv(pair[1]) + ","
                        + csv(message.from) + ","
                        + csv(String.valueOf(message.sentAtEpochMillis)) + ","
                        + csv(NetworkProtocol.encode(message.text)));
            }
        }
        Files.write(CHATS_PATH, lines, StandardCharsets.UTF_8);
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
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private boolean areFriends(String a, String b) {
        Set<String> set = friends.get(a);
        return set != null && set.contains(b);
    }

    private void addFriendship(String a, String b) {
        friends.computeIfAbsent(a, key -> new HashSet<>()).add(b);
        friends.computeIfAbsent(b, key -> new HashSet<>()).add(a);
    }

    private String chatKey(String a, String b) {
        if (a.compareTo(b) <= 0) {
            return a + "|" + b;
        }
        return b + "|" + a;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public enum FriendRequestStatus {
        SENT,
        ALREADY,
        ACCEPTED,
        INVALID
    }

    private static final class ChatMessage {
        private final String from;
        private final String text;
        private final long sentAtEpochMillis;

        private ChatMessage(String from, String text, long sentAtEpochMillis) {
            this.from = from;
            this.text = text;
            this.sentAtEpochMillis = sentAtEpochMillis;
        }
    }
}
