package com.example.onlinemcqexam;

import java.util.ArrayList;
import java.util.List;

public final class DiscussionStore {
    private static final int MAX_MESSAGES = 200;

    private final List<DiscussionMessage> messages = new ArrayList<>();

    public synchronized void addMessage(String author, String text) {
        DiscussionMessage message = new DiscussionMessage(author, text);
        messages.add(0, message);
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(messages.size() - 1);
        }
    }

    public synchronized List<String> getDisplayMessages() {
        List<String> display = new ArrayList<>(messages.size());
        for (DiscussionMessage message : messages) {
            StringBuilder builder = new StringBuilder();
            builder.append("Q: ").append(message.text).append(" (").append(message.author).append(")");
            display.add(builder.toString());
        }
        return display;
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
