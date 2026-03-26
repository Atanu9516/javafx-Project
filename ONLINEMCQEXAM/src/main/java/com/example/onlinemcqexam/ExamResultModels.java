package com.example.onlinemcqexam;

import java.time.LocalDateTime;

public final class ExamResultModels {
    private ExamResultModels() {
    }

    public record ResultSummaryRow(
            String examId,
            String username,
            String term,
            String course,
            int totalQuestions,
            int correctAnswers,
            double percentage,
            int timeTakenSeconds,
            String attemptDate,
            LocalDateTime parsedDate
    ) {
    }

    public record ResultDetailRow(
            String examId,
            String studentId,
            String questionId,
            String selectedOption,
            String correctOption,
            String course,
            String term,
            String attemptDate
    ) {
        public boolean isCorrect() {
            if (selectedOption == null || correctOption == null) {
                return false;
            }
            return selectedOption.trim().equalsIgnoreCase(correctOption.trim());
        }
    }

    public record QuestionMeta(
            String questionId,
            String questionText,
            String difficulty
    ) {
    }

    public record ExamMeta(
            String examId,
            String examTitle,
            String examDate,
            int totalMarks
    ) {
    }
}
