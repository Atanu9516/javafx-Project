package com.example.onlinemcqexam;

import java.util.List;

public class Question {
    private final String term;
    private final String courseCode;
    private final String courseName;
    private final String examId;
    private final String id;
    private final Level level;
    private final String text;
    private final List<String> options;
    private final int correctIndex;

    public Question(String id, Level level, String text, List<String> options, int correctIndex) {
        this(null, null, null, null, id, level, text, options, correctIndex);
    }

    public Question(String term, String courseCode, String courseName,
                    String id, Level level, String text, List<String> options, int correctIndex) {
        this(term, courseCode, courseName, null, id, level, text, options, correctIndex);
    }

    public Question(String term, String courseCode, String courseName, String examId,
                    String id, Level level, String text, List<String> options, int correctIndex) {
        this.term = term;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.examId = examId;
        this.id = id;
        this.level = level;
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getTerm() {
        return term;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getExamId() {
        return examId;
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
