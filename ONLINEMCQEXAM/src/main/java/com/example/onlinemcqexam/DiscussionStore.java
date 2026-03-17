package com.example.onlinemcqexam;

import java.util.ArrayList;
import java.util.List;

public final class DiscussionStore {
    private static final int MAX_MESSAGES = 200;

    private final List<DiscussionMessage> messages = new ArrayList<>();
    private long nextId = 1;

    public synchronized void addMessage(String author, String text) {
        DiscussionMessage message = new DiscussionMessage(nextId++, author, text, false);
        messages.add(0, message);
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(messages.size() - 1);
        }
    }

    public synchronized boolean commentAt(int index) {
        if (index < 0 || index >= messages.size()) {
            return false;
        }
        DiscussionMessage message = messages.get(index);
        if (message.commented) {
            return true;
        }
        message.commented = true;
        return true;
    }

    public synchronized List<String> getDisplayMessages() {
        List<String> display = new ArrayList<>(messages.size());
        for (DiscussionMessage message : messages) {
            StringBuilder builder = new StringBuilder();
            builder.append("Q: ").append(message.text).append(" (").append(message.author).append(")");
            if (message.commented) {
                builder.append(" [Commented]");
            }
            display.add(builder.toString());
        }
        return display;
    }

    private static final class DiscussionMessage {
        private final long id;
        private final String author;
        private final String text;
        private boolean commented;

        private DiscussionMessage(long id, String author, String text, boolean commented) {
            this.id = id;
            this.author = author;
            this.text = text;
            this.commented = commented;
        }
    }
}
