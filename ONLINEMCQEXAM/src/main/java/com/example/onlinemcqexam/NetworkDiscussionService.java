package com.example.onlinemcqexam;

import java.io.IOException;
import java.util.List;

public final class NetworkDiscussionService implements DiscussionService {
    private final ExamClient client;

    public NetworkDiscussionService(ExamClient client) {
        this.client = client;
    }

    @Override
    public List<String> fetchMessages() throws IOException {
        return client.fetchDiscussion();
    }

    @Override
    public void postMessage(String author, String text) throws IOException {
        ExamClient.ServerResponse response = client.postDiscussion(author, text);
        if (!response.ok()) {
            throw new IOException(response.message());
        }
    }

    @Override
    public boolean commentMessage(int index) throws IOException {
        ExamClient.ServerResponse response = client.commentDiscussion(index);
        if (response.ok()) {
            return true;
        }
        if ("NOT_FOUND".equalsIgnoreCase(response.message())) {
            return false;
        }
        throw new IOException(response.message());
    }
}
