package com.example.onlinemcqexam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiscussionStore {
    private static final int MAX_MESSAGES = 200;
    private static final String DELIMITER = ",";
    private static final Path DISCUSSION_PATH = AppPaths.dataFile("discussion.csv");

    private final List<DiscussionMessage> messages = Collections.synchronizedList(new ArrayList<>());
    private final Object lock = new Object();

    public DiscussionStore() {
        loadState();
    }

    public synchronized void addMessage(String author, String text) {
        DiscussionMessage message = new DiscussionMessage(author, text);
        messages.add(0, message);
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(messages.size() - 1);
        }
        persistQuietly();
    }

    public synchronized List<String> getDisplayMessages() {
        List<String> display = new ArrayList<>(messages.size());
        synchronized (messages) {
            for (DiscussionMessage message : messages) {
                display.add("Q: " + message.text + " (" + message.author + ")");
            }
        }
        return display;
    }

    private void loadState() {
        try {
            ensureFileExists();
            List<String> lines = Files.readAllLines(DISCUSSION_PATH, StandardCharsets.UTF_8);
            synchronized (messages) {
                messages.clear();
                for (String line : lines) {
                    if (line == null || line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    List<String> parts = parseCsvLine(line);
                    if (parts.size() < 2) {
                        continue;
                    }
                    String author = NetworkProtocol.decode(parts.get(0));
                    String text = NetworkProtocol.decode(parts.get(1));
                    if (author.isBlank() || text.isBlank()) {
                        continue;
                    }
                    messages.add(new DiscussionMessage(author, text));
                }
                Collections.reverse(messages);
            }
        } catch (IOException ex) {
            System.err.println("Unable to load discussion store data: " + ex.getMessage());
        }
    }

    private void persistQuietly() {
        try {
            saveState();
        } catch (IOException ex) {
            System.err.println("Unable to persist discussion store data: " + ex.getMessage());
        }
    }

    private void saveState() throws IOException {
        ensureFileExists();
        List<String> lines = new ArrayList<>();
        lines.add("# author,text");
        synchronized (messages) {
            for (DiscussionMessage message : messages) {
                lines.add(csv(NetworkProtocol.encode(message.author)) + DELIMITER + csv(NetworkProtocol.encode(message.text)));
            }
        }
        Files.write(DISCUSSION_PATH, lines, StandardCharsets.UTF_8);
    }

    private void ensureFileExists() throws IOException {
        Path dir = AppPaths.appRoot().resolve("data");
        Files.createDirectories(dir);
        if (!Files.exists(DISCUSSION_PATH)) {
            Files.writeString(DISCUSSION_PATH, "# author,text" + System.lineSeparator(), StandardCharsets.UTF_8);
        }
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

    private static final class DiscussionMessage {
        private final String author;
        private final String text;

        private DiscussionMessage(String author, String text) {
            this.author = author;
            this.text = text;
        }
    }
}
