package com.example.onlinemcqexam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FriendStore {
    private static final int MAX_CHAT_MESSAGES = 500;

    private final Map<String, Set<String>> friends = new HashMap<>();
    private final Map<String, Set<String>> pending = new HashMap<>();
    private final Map<String, List<ChatMessage>> chats = new HashMap<>();

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
            // Auto-accept if both requested each other.
            incoming.remove(target);
            if (incoming.isEmpty()) {
                pending.remove(sender);
            }
            addFriendship(sender, target);
            return FriendRequestStatus.ACCEPTED;
        }
        Set<String> targetPending = pending.computeIfAbsent(target, key -> new HashSet<>());
        if (!targetPending.add(sender)) {
            return FriendRequestStatus.ALREADY;
        }
        return FriendRequestStatus.SENT;
    }

    public synchronized List<String> listPending(String user) {
        String normalized = normalize(user);
        Set<String> requests = pending.getOrDefault(normalized, Set.of());
        return new ArrayList<>(requests);
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
        return true;
    }

    public synchronized List<String> listFriends(String user) {
        String normalized = normalize(user);
        Set<String> list = friends.getOrDefault(normalized, Set.of());
        return new ArrayList<>(list);
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
        history.add(new ChatMessage(sender, text.trim()));
        if (history.size() > MAX_CHAT_MESSAGES) {
            history.remove(0);
        }
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
            display.add(message.from + ": " + message.text);
        }
        return display;
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

        private ChatMessage(String from, String text) {
            this.from = from;
            this.text = text;
        }
    }
}
