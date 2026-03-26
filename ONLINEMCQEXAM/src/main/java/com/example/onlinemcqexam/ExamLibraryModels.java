package com.example.onlinemcqexam;

import java.time.LocalDate;

public final class ExamLibraryModels {
    private ExamLibraryModels() {
    }

    public record TermRecord(String termId, String termName, String academicYear) {
        public String display() {
            String name = termName == null ? "" : termName.trim();
            String year = academicYear == null ? "" : academicYear.trim();
            if (!name.isBlank() && !year.isBlank()) {
                return name + " (" + year + ")";
            }
            if (!name.isBlank()) {
                return name;
            }
            return termId == null ? "" : termId;
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

    public record ScheduleRecord(String examId, String termId, String courseId, LocalDate date, String startTime) {
    }

    public record ExamRecord(
            String examId,
            String examTitle,
            String courseId,
            String termId,
            int questionCount,
            int durationMinutes,
            String status,
            LocalDate examDate
    ) {
    }
}
