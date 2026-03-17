package com.example.onlinemcqexam;

import java.io.IOException;
import java.util.List;

public interface DiscussionService {
    List<String> fetchMessages() throws IOException;

    void postMessage(String author, String text) throws IOException;

    boolean commentMessage(int index) throws IOException;
}
