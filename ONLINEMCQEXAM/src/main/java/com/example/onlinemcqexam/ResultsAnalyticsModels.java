package com.example.onlinemcqexam;

public final class ResultsAnalyticsModels {
    private ResultsAnalyticsModels() {
    }

    public record TermRecord(String termId, String termName, String academicYear) {
        public String display() {
            String name = termName == null ? "" : termName.trim();
            String id = termId == null ? "" : termId.trim();
            String year = academicYear == null ? "" : academicYear.trim();
            if (!name.isBlank() && !year.isBlank()) {
                return name + " (" + year + ")";
            }
            if (!name.isBlank()) {
                return name;
            }
            return id;
        }
    }

    public record CourseRecord(String courseId, String courseName, String courseCode, String termId) {
        public String display() {
            String code = courseCode == null ? "" : courseCode.trim();
            String name = courseName == null ? "" : courseName.trim();
            if (!code.isBlank() && !name.isBlank()) {
                return code + ": " + name;
            }
            if (!code.isBlank()) {
                return code;
            }
            return name;
        }
    }

    public record ExamRecord(String examId, String examTitle, String courseId, String termId, String examDate) {
    }

    public record StudentRecord(String studentId, String name, String email, String batch) {
    }

    public record ResultRecord(String studentId, String examId, String termId, String courseId, double score, double totalMarks,
                               double percentage, String grade, String examDate, String sessionType) {
    }
}
