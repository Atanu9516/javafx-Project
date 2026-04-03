package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExamResultController {
    private static final String RESULTS_FILE = AppPaths.resourceFile("results.csv").toString();
    private static final String RESULT_DETAILS_FILE = AppPaths.resourceFile("result_details.csv").toString();
    private static final String QUESTIONS_FILE = AppPaths.packageResourceFile("questions.csv").toString();
    private static final String EXAMS_FILE = AppPaths.resourceFile("exams.csv").toString();
    private static final String NO_DATA = "No data available";
    private static final double PASS_PERCENTAGE = 40.0;
    private static final DateTimeFormatter HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @FXML
    private Label examTitleLabel;
    @FXML
    private Label completionDateLabel;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private Label finalScoreLabel;
    @FXML
    private Label accuracyBadgeLabel;
    @FXML
    private Label statusMessageLabel;
    @FXML
    private Label correctCountLabel;
    @FXML
    private Label wrongCountLabel;
    @FXML
    private Label accuracyLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox questionBreakdownContainer;
    @FXML
    private Label emptyStateLabel;

    private Runnable onResultBoard;
    private Runnable onProgress;
    private Runnable onDiscussion;
    private Runnable onBackToDashboard;

    public void setNavigationActions(Runnable onResultBoard,
                                     Runnable onProgress,
                                     Runnable onDiscussion,
                                     Runnable onBackToDashboard) {
        this.onResultBoard = onResultBoard;
        this.onProgress = onProgress;
        this.onDiscussion = onDiscussion;
        this.onBackToDashboard = onBackToDashboard;
    }

    public void loadResultForAttempt(String studentId, String examId, String attemptDate) {
        ExamResultModels.ResultSummaryRow summary = resolveSummary(studentId, examId, attemptDate).orElse(null);
        if (summary == null) {
            applyNoDataState();
            return;
        }

        List<ExamResultModels.ResultDetailRow> details = loadDetailRows().stream()
                .filter(row -> row.studentId().equalsIgnoreCase(studentId == null ? "" : studentId))
                .filter(row -> equalsIgnoreCaseSafe(row.attemptDate(), summary.attemptDate()))
                .filter(row -> examId == null || examId.isBlank() || equalsIgnoreCaseSafe(row.examId(), examId))
                .collect(Collectors.toList());

        Map<String, ExamResultModels.QuestionMeta> questionMetaById = loadQuestionMeta();
        Map<String, ExamResultModels.ExamMeta> examMetaById = loadExamMeta();

        int totalQuestions;
        int correctCount;
        if (!details.isEmpty()) {
            totalQuestions = details.size();
            correctCount = (int) details.stream().filter(ExamResultModels.ResultDetailRow::isCorrect).count();
        } else {
            totalQuestions = summary.totalQuestions();
            correctCount = summary.correctAnswers();
        }

        int wrongCount = Math.max(totalQuestions - correctCount, 0);
        double percentage = totalQuestions <= 0 ? 0.0 : ((correctCount * 100.0) / totalQuestions);
        String status = percentage >= PASS_PERCENTAGE ? "Passed" : "Failed";

        ExamResultModels.ExamMeta examMeta = examMetaById.getOrDefault(
                normalized(summary.examId()),
                new ExamResultModels.ExamMeta(summary.examId(), NO_DATA, summary.attemptDate(), totalQuestions)
        );

        String resolvedTitle = resolveTitle(examMeta.examTitle(), summary.course());
        examTitleLabel.setText(resolvedTitle);
        completionDateLabel.setText(formatCompletionDate(summary.attemptDate(), examMeta.examDate()));
        totalTimeLabel.setText(formatDuration(summary.timeTakenSeconds()));

        finalScoreLabel.setText(totalQuestions <= 0 ? NO_DATA : (correctCount + " / " + totalQuestions));
        accuracyBadgeLabel.setText(totalQuestions <= 0 ? NO_DATA : String.format(Locale.US, "%.0f%%", percentage));
        statusMessageLabel.setText(percentage >= PASS_PERCENTAGE
                ? "Strong completion with accurate responses"
                : "Review incorrect questions and retry");

        correctCountLabel.setText(totalQuestions <= 0 ? NO_DATA : String.valueOf(correctCount));
        wrongCountLabel.setText(totalQuestions <= 0 ? NO_DATA : String.valueOf(wrongCount));
        accuracyLabel.setText(totalQuestions <= 0 ? NO_DATA : String.format(Locale.US, "%.0f%%", percentage));
        statusLabel.setText(totalQuestions <= 0 ? NO_DATA : status);

        statusLabel.getStyleClass().removeAll("exam-result-status-pass", "exam-result-status-fail", "exam-result-status-no-data");
        statusLabel.getStyleClass().add(totalQuestions <= 0
                ? "exam-result-status-no-data"
                : (percentage >= PASS_PERCENTAGE ? "exam-result-status-pass" : "exam-result-status-fail"));

        populateBreakdown(details, questionMetaById);
    }

    @FXML
    private void onResultBoard() {
        if (onResultBoard != null) {
            onResultBoard.run();
        }
    }

    @FXML
    private void onProgress() {
        if (onProgress != null) {
            onProgress.run();
        }
    }

    @FXML
    private void onDiscussion() {
        if (onDiscussion != null) {
            onDiscussion.run();
        }
    }

    @FXML
    private void onBackToDashboard() {
        if (onBackToDashboard != null) {
            onBackToDashboard.run();
        }
    }

    private Optional<ExamResultModels.ResultSummaryRow> resolveSummary(String studentId, String examId, String attemptDate) {
        List<ExamResultModels.ResultSummaryRow> rows = loadSummaryRows().stream()
                .filter(row -> row.username().equalsIgnoreCase(studentId == null ? "" : studentId))
                .collect(Collectors.toList());

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        if (attemptDate != null && !attemptDate.isBlank()) {
            Optional<ExamResultModels.ResultSummaryRow> exact = rows.stream()
                    .filter(row -> equalsIgnoreCaseSafe(row.attemptDate(), attemptDate))
                    .filter(row -> examId == null || examId.isBlank() || equalsIgnoreCaseSafe(row.examId(), examId))
                    .max(Comparator.comparing(ExamResultModels.ResultSummaryRow::parsedDate));
            if (exact.isPresent()) {
                return exact;
            }
        }

        return rows.stream().max(Comparator.comparing(ExamResultModels.ResultSummaryRow::parsedDate));
    }

    private List<ExamResultModels.ResultSummaryRow> loadSummaryRows() {
        Path path = Paths.get(RESULTS_FILE);
        if (Files.notExists(path)) {
            return List.of();
        }

        List<ExamResultModels.ResultSummaryRow> rows = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("username,term,course,examid,")) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() < 10) {
                    continue;
                }

                String username = values.get(0).trim();
                String term = values.get(1).trim();
                String course = values.get(2).trim();
                String examId = values.get(3).trim();
                int totalQuestions = parseIntOrDefault(values.get(4).trim(), 0);
                int correctAnswers = parseIntOrDefault(values.get(5).trim(), 0);
                double percentage = parseDoubleOrDefault(values.get(6).trim(), 0.0);
                int timeTaken = parseIntOrDefault(values.get(7).trim(), 0);
                String date = values.get(8).trim();

                rows.add(new ExamResultModels.ResultSummaryRow(
                        examId,
                        username,
                        term,
                        course,
                        totalQuestions,
                        correctAnswers,
                        percentage,
                        timeTaken,
                        date,
                        parseDate(date)
                ));
            }
        } catch (IOException ex) {
            return List.of();
        }
        return rows;
    }

    private List<ExamResultModels.ResultDetailRow> loadDetailRows() {
        Path path = Paths.get(RESULT_DETAILS_FILE);
        if (Files.notExists(path)) {
            return List.of();
        }

        List<ExamResultModels.ResultDetailRow> rows = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("exam_id,student_id,question_id,")) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() < 8) {
                    continue;
                }
                rows.add(new ExamResultModels.ResultDetailRow(
                        values.get(0).trim(),
                        values.get(1).trim(),
                        values.get(2).trim(),
                        values.get(3).trim(),
                        values.get(4).trim(),
                        values.get(5).trim(),
                        values.get(6).trim(),
                        values.get(7).trim()
                ));
            }
        } catch (IOException ex) {
            return List.of();
        }
        return rows;
    }

    private Map<String, ExamResultModels.QuestionMeta> loadQuestionMeta() {
        Path path = Paths.get(QUESTIONS_FILE);
        if (Files.notExists(path)) {
            return Map.of();
        }

        Map<String, ExamResultModels.QuestionMeta> metaById = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.contains("question") && lower.contains("course") && lower.contains("term")) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                if (values.size() < 5) {
                    continue;
                }

                String questionId = values.size() > 3 ? values.get(3).trim() : "";
                String questionText = values.size() > 4 ? values.get(4).trim() : "";
                String difficulty = deriveDifficulty(questionId, values.size() > 0 ? values.get(0).trim() : "");

                if (!questionId.isBlank()) {
                    metaById.put(normalized(questionId), new ExamResultModels.QuestionMeta(
                            questionId,
                            questionText.isBlank() ? NO_DATA : questionText,
                            difficulty
                    ));
                }
            }
        } catch (IOException ex) {
            return Map.of();
        }
        return metaById;
    }

    private Map<String, ExamResultModels.ExamMeta> loadExamMeta() {
        Path path = Paths.get(EXAMS_FILE);
        if (Files.notExists(path)) {
            return Map.of();
        }

        Map<String, ExamResultModels.ExamMeta> map = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int index = 0;
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                index++;

                List<String> values = parseCsvLine(line);
                if (values.isEmpty()) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (index == 1 && lower.startsWith("term,coursecode,course")) {
                    continue;
                }

                if (values.size() >= 8) {
                    String examId = "EX" + String.format(Locale.US, "%03d", index - 1);
                    String title = values.get(1).trim().isBlank() ? values.get(2).trim() : values.get(1).trim();
                    map.put(normalized(examId), new ExamResultModels.ExamMeta(examId, title, NO_DATA, parseIntOrDefault(values.get(5).trim(), 0)));
                }
            }
        } catch (IOException ex) {
            return Map.of();
        }
        return map;
    }

    private void populateBreakdown(List<ExamResultModels.ResultDetailRow> details,
                                   Map<String, ExamResultModels.QuestionMeta> questionMetaById) {
        questionBreakdownContainer.getChildren().clear();

        if (details.isEmpty()) {
            emptyStateLabel.setText(NO_DATA);
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            return;
        }

        List<ExamResultModels.ResultDetailRow> sorted = new ArrayList<>(details);
        sorted.sort(Comparator.comparing(ExamResultModels.ResultDetailRow::questionId));

        int number = 1;
        for (ExamResultModels.ResultDetailRow detail : sorted) {
            ExamResultModels.QuestionMeta meta = questionMetaById.get(normalized(detail.questionId()));
            String questionText = meta == null ? NO_DATA : meta.questionText();
            String difficulty = meta == null ? NO_DATA : meta.difficulty();
            boolean correct = detail.isCorrect();

            HBox card = new HBox(12);
            card.getStyleClass().add("exam-result-question-card");

            VBox content = new VBox(3);
            Label indexLabel = new Label(String.format(Locale.US, "%02d", number));
            indexLabel.getStyleClass().add("exam-result-question-index");

            Label questionLabel = new Label(questionText);
            questionLabel.getStyleClass().add("exam-result-question-text");
            questionLabel.setWrapText(true);

            Label metaLabel = new Label("Difficulty: " + difficulty);
            metaLabel.getStyleClass().add("exam-result-question-meta");

            content.getChildren().addAll(indexLabel, questionLabel, metaLabel);
            HBox.setHgrow(content, Priority.ALWAYS);

            Label badge = new Label(correct ? "Correct" : "Incorrect");
            badge.getStyleClass().addAll("exam-result-question-badge", correct
                    ? "exam-result-question-correct"
                    : "exam-result-question-wrong");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            card.getChildren().addAll(content, spacer, badge);
            questionBreakdownContainer.getChildren().add(card);
            number++;
        }

        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);
    }

    private void applyNoDataState() {
        examTitleLabel.setText(NO_DATA);
        completionDateLabel.setText(NO_DATA);
        totalTimeLabel.setText(NO_DATA);
        finalScoreLabel.setText(NO_DATA);
        accuracyBadgeLabel.setText(NO_DATA);
        statusMessageLabel.setText(NO_DATA);
        correctCountLabel.setText(NO_DATA);
        wrongCountLabel.setText(NO_DATA);
        accuracyLabel.setText(NO_DATA);
        statusLabel.setText(NO_DATA);
        statusLabel.getStyleClass().removeAll("exam-result-status-pass", "exam-result-status-fail");
        statusLabel.getStyleClass().add("exam-result-status-no-data");
        questionBreakdownContainer.getChildren().clear();
        emptyStateLabel.setText(NO_DATA);
        emptyStateLabel.setVisible(true);
        emptyStateLabel.setManaged(true);
    }

    private String resolveTitle(String examTitle, String course) {
        if (examTitle != null && !examTitle.isBlank() && !NO_DATA.equals(examTitle)) {
            return examTitle;
        }
        if (course == null || course.isBlank()) {
            return NO_DATA;
        }
        return course.trim() + " Assessment";
    }

    private String formatCompletionDate(String attemptDate, String examDate) {
        if (attemptDate != null && !attemptDate.isBlank()) {
            return "Completed on " + attemptDate;
        }
        if (examDate != null && !examDate.isBlank() && !NO_DATA.equals(examDate)) {
            return "Completed on " + examDate;
        }
        return NO_DATA;
    }

    private String formatDuration(int totalSeconds) {
        if (totalSeconds <= 0) {
            return NO_DATA;
        }
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", mins, secs);
    }

    private String deriveDifficulty(String questionId, String term) {
        String id = normalized(questionId);
        if (id.contains("-e-") || id.contains("easy")) {
            return "Easy";
        }
        if (id.contains("-m-") || id.contains("medium")) {
            return "Medium";
        }
        if (id.contains("-h-") || id.contains("hard")) {
            return "Hard";
        }

        String trimmedTerm = term == null ? "" : term.trim();
        if (trimmedTerm.startsWith("1-")) {
            return "Easy";
        }
        if (trimmedTerm.startsWith("2-")) {
            return "Medium";
        }
        if (!trimmedTerm.isBlank()) {
            return "Hard";
        }
        return NO_DATA;
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

    private boolean equalsIgnoreCaseSafe(String left, String right) {
        String l = left == null ? "" : left.trim();
        String r = right == null ? "" : right.trim();
        return l.equalsIgnoreCase(r);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double parseDoubleOrDefault(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private LocalDateTime parseDate(String value) {
        try {
            return LocalDateTime.parse(value, HISTORY_DATE_FORMAT);
        } catch (Exception ex) {
            return LocalDateTime.MIN;
        }
    }
}
