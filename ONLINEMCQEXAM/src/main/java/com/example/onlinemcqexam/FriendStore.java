package com.example.onlinemcqexam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class FriendStore {
    private static final int MAX_CHAT_MESSAGES = 500;
    private static final String CHAT_METADATA_SEPARATOR = "\u001F";
    private static final Path FRIENDS_PATH = AppPaths.dataFile("friends.csv");
    private static final Path PENDING_PATH = AppPaths.dataFile("friend_requests.csv");
    private static final Path CHATS_PATH = AppPaths.dataFile("chat_history.csv");

    private final Map<String, Set<String>> friends = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> pending = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> chats = new ConcurrentHashMap<>();

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
        
        // Use thread-safe operations
        Set<String> incomingSet = pending.computeIfAbsent(sender, k -> Collections.synchronizedSet(new HashSet<>()));
        synchronized (incomingSet) {
            if (incomingSet.contains(target)) {
                incomingSet.remove(target);
                if (incomingSet.isEmpty()) {
                    pending.remove(sender);
                }
                addFriendship(sender, target);
                persistQuietly();
                return FriendRequestStatus.ACCEPTED;
            }
        }
        
        Set<String> targetPendingSet = pending.computeIfAbsent(target, k -> Collections.synchronizedSet(new HashSet<>()));
        synchronized (targetPendingSet) {
            if (!targetPendingSet.add(sender)) {
                return FriendRequestStatus.ALREADY;
            }
        }
        persistQuietly();
        return FriendRequestStatus.SENT;
    }

    public synchronized List<String> listPending(String user) {
        String normalized = normalize(user);
        Set<String> requests = pending.getOrDefault(normalized, Collections.emptySet());
        synchronized (requests) {
            return new ArrayList<>(new TreeSet<>(requests));
        }
    }

    public synchronized boolean acceptRequest(String user, String from) {
        String normalized = normalize(user);
        String sender = normalize(from);
        Set<String> requests = pending.get(normalized);
        if (requests == null) {
            return false;
        }
        synchronized (requests) {
            if (!requests.remove(sender)) {
                return false;
            }
            if (requests.isEmpty()) {
                pending.remove(normalized);
            }
        }
        addFriendship(normalized, sender);
        persistQuietly();
        return true;
    }

    public synchronized boolean declineRequest(String user, String from) {
        String normalized = normalize(user);
        String sender = normalize(from);
        Set<String> requests = pending.get(normalized);
        if (requests == null) {
            return false;
        }
        synchronized (requests) {
            if (!requests.remove(sender)) {
                return false;
            }
            if (requests.isEmpty()) {
                pending.remove(normalized);
            }
        }
        persistQuietly();
        return true;
    }

    public synchronized List<String> listFriends(String user) {
        String normalized = normalize(user);
        Set<String> list = friends.getOrDefault(normalized, Collections.emptySet());
        synchronized (list) {
            return new ArrayList<>(new TreeSet<>(list));
        }
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
        List<ChatMessage> history = chats.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (history) {
            history.add(new ChatMessage(sender, text.trim(), System.currentTimeMillis()));
            if (history.size() > MAX_CHAT_MESSAGES) {
                history.remove(0);
            }
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
        List<ChatMessage> history = chats.getOrDefault(key, Collections.synchronizedList(new ArrayList<>()));
        List<String> display = new ArrayList<>();
        synchronized (history) {
            for (ChatMessage message : history) {
                display.add(message.from + CHAT_METADATA_SEPARATOR + message.sentAtEpochMillis + CHAT_METADATA_SEPARATOR + message.text);
            }
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
            Set<String> targetSet = pending.computeIfAbsent(target, k -> Collections.synchronizedSet(new HashSet<>()));
            synchronized (targetSet) {
                targetSet.add(from);
            }
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
            List<ChatMessage> historyList = chats.computeIfAbsent(chatKey(a, b), k -> Collections.synchronizedList(new ArrayList<>()));
            synchronized (historyList) {
                historyList.add(new ChatMessage(sender, text, timestamp));
            }
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

    private void addFriendship(String a, String b) {
        Set<String> aFriends = friends.computeIfAbsent(a, k -> Collections.synchronizedSet(new HashSet<>()));
        Set<String> bFriends = friends.computeIfAbsent(b, k -> Collections.synchronizedSet(new HashSet<>()));
        synchronized (aFriends) {
            aFriends.add(b);
        }
        synchronized (bFriends) {
            bFriends.add(a);
        }
    }

    private boolean areFriends(String a, String b) {
        Set<String> set = friends.get(a);
        if (set == null) {
            return false;
        }
        synchronized (set) {
            return set.contains(b);
        }
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
