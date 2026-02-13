package com.example.onlinemcqexam;

import java.util.List;

public class Question {
    private final String id;
    private final Level level;
    private final String text;
    private final List<String> options;
    private final int correctIndex;

    public Question(String id, Level level, String text, List<String> options, int correctIndex) {
        this.id = id;
        this.level = level;
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getId() {
        return id;
    }

    public Level getLevel() {
        return level;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }
}
