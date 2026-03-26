package com.example.onlinemcqexam;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LeaderboardController {
    private static final String EXAM_HISTORY_FILE = "src/main/resources/exam_history.csv";

    @FXML
    private ChoiceBox<String> termChoice;
    @FXML
    private ChoiceBox<String> courseChoice;
    @FXML
    private TextField searchField;
    @FXML
    private Label statusLabel;

    @FXML
    private TableView<LeaderboardEntry> leaderboardTable;
    @FXML
    private TableColumn<LeaderboardEntry, Integer> rankColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> studentNameColumn;
    @FXML
    private TableColumn<LeaderboardEntry, Integer> attemptsColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> averagePercentageColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> bestPercentageColumn;

    private final ObservableList<LeaderboardEntry> originalList = FXCollections.observableArrayList();
    private final FilteredList<LeaderboardEntry> filteredList = new FilteredList<>(originalList, entry -> true);
    private final SortedList<LeaderboardEntry> sortedList = new SortedList<>(filteredList);
    private String currentUser;

    @FXML
    private void initialize() {
        currentUser = UserSession.getUsername();
        configureTable();
        configureFilters();
        configureSearch();
        configureTableItems();
        configureCurrentUserHighlight();
        loadTerms();
    }

    private void configureTable() {
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        attemptsColumn.setCellValueFactory(new PropertyValueFactory<>("attempts"));
        averagePercentageColumn.setCellValueFactory(new PropertyValueFactory<>("averagePercentageDisplay"));
        bestPercentageColumn.setCellValueFactory(new PropertyValueFactory<>("bestPercentageDisplay"));
    }

    private void configureFilters() {
        termChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldTerm, newTerm) -> {
            loadCoursesForTerm(newTerm);
            refreshLeaderboard();
        });
        courseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldCourse, newCourse) -> refreshLeaderboard());
    }

    private void configureSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterLeaderboard(newVal));
    }

    private void configureTableItems() {
        sortedList.comparatorProperty().bind(leaderboardTable.comparatorProperty());
        leaderboardTable.setItems(sortedList);
    }

    private void configureCurrentUserHighlight() {
        leaderboardTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LeaderboardEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (currentUser != null && !currentUser.isBlank() && item.getUsername().equalsIgnoreCase(currentUser)) {
                    setStyle("-fx-background-color: #d1f7c4;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void filterLeaderboard(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            filteredList.setPredicate(entry -> true);
            return;
        }
        String normalizedKeyword = keyword.toLowerCase();
        filteredList.setPredicate(entry ->
                entry.getUsername() != null && entry.getUsername().toLowerCase().contains(normalizedKeyword)
        );
    }

    private void loadTerms() {
        termChoice.getItems().setAll("1-1", "1-2", "2-1", "2-2", "3-1", "3-2");
        termChoice.getSelectionModel().selectFirst();
    }

    private void loadCoursesForTerm(String term) {
        courseChoice.getItems().clear();
        if (term == null || term.isBlank()) {
            return;
        }
        try {
            List<QuestionBank.CourseInfo> courses = QuestionBank.loadCoursesForTerm(term);
            for (QuestionBank.CourseInfo course : courses) {
                courseChoice.getItems().add(normalizeCourseCode(course.getCourseCode()));
            }
            if (!courseChoice.getItems().isEmpty()) {
                courseChoice.getSelectionModel().selectFirst();
            }
        } catch (IOException ex) {
            statusLabel.setText("Unable to load courses for selected term.");
        }
    }

    @FXML
    private void refreshLeaderboard() {
        currentUser = UserSession.getUsername();
        String selectedTerm = termChoice.getValue();
        String selectedCourse = normalizeCourseCode(courseChoice.getValue());
        if (selectedTerm == null || selectedTerm.isBlank() || selectedCourse.isBlank()) {
            originalList.clear();
            statusLabel.setText("Select a term and course.");
            return;
        }

        List<ExamRecord> attempts = readRowsForCourse(selectedTerm, selectedCourse);
        Map<String, List<ExamRecord>> userMap = new LinkedHashMap<>();
        for (ExamRecord record : attempts) {
            userMap.computeIfAbsent(record.getUsername(), key -> new ArrayList<>()).add(record);
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<ExamRecord>> entry : userMap.entrySet()) {
            String username = entry.getKey();
            List<ExamRecord> records = entry.getValue();
            int attemptsCount = records.size();
            double sum = 0.0;
            double best = 0.0;
            for (ExamRecord record : records) {
                double score = record.getPercentage();
                sum += score;
                best = Math.max(best, score);
            }
            double avg = attemptsCount == 0 ? 0.0 : (sum / attemptsCount);
            entries.add(new LeaderboardEntry(0, username, attemptsCount, avg, best));
        }

        entries.sort(
                Comparator.comparing(LeaderboardEntry::getAveragePercentage).reversed()
                        .thenComparing(LeaderboardEntry::getBestPercentage).reversed()
        );

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        originalList.setAll(entries);
        filterLeaderboard(searchField != null ? searchField.getText() : null);
        leaderboardTable.refresh();
        scrollToCurrentUser();

        statusLabel.setText(entries.isEmpty()
                ? "No exam attempts found for " + selectedCourse + "."
                : entries.size() + " students ranked for " + selectedCourse + ".");
    }

    private void scrollToCurrentUser() {
        if (currentUser == null || currentUser.isBlank()) {
            return;
        }
        Platform.runLater(() -> {
            for (LeaderboardEntry entry : leaderboardTable.getItems()) {
                if (entry.getUsername() != null && entry.getUsername().equalsIgnoreCase(currentUser)) {
                    leaderboardTable.scrollTo(entry);
                    break;
                }
            }
        });
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) leaderboardTable.getScene().getWindow();
        stage.close();
    }

    private List<ExamRecord> readRowsForCourse(String selectedTerm, String selectedCourse) {
        List<ExamRecord> rows = new ArrayList<>();
        if (!isCourseInSelectedTerm(selectedTerm, selectedCourse)) {
            return rows;
        }
        Path path = Paths.get(EXAM_HISTORY_FILE);
        if (Files.notExists(path)) {
            return rows;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase();
                if (lower.startsWith("username,") || lower.startsWith("coursecode,")) {
                    continue;
                }
                ExamRecord record = parseExamRecord(line);
                if (record == null) {
                    continue;
                }
                String courseCode = normalizeCourseCode(record.getCourseCode());
                if (!selectedCourse.equals(courseCode)) {
                    continue;
                }

                // Course list is loaded from selected term, so matching course implies selected term.
                rows.add(record);
            }
        } catch (IOException ex) {
            statusLabel.setText("Unable to load leaderboard data.");
        }
        return rows;
    }

    private boolean isCourseInSelectedTerm(String selectedTerm, String selectedCourse) {
        if (selectedTerm == null || selectedTerm.isBlank() || selectedCourse == null || selectedCourse.isBlank()) {
            return false;
        }
        try {
            Set<String> termCourses = new HashSet<>();
            for (QuestionBank.CourseInfo course : QuestionBank.loadCoursesForTerm(selectedTerm)) {
                termCourses.add(normalizeCourseCode(course.getCourseCode()));
            }
            return termCourses.contains(selectedCourse);
        } catch (IOException ex) {
            statusLabel.setText("Unable to load courses for selected term.");
            return false;
        }
    }

    private ExamRecord parseExamRecord(String line) {
        String[] parts = line.split(",");
        if (parts.length < 7) {
            return null;
        }

        boolean newFormat = !looksLikeDate(parts[1].trim());
        String username;
        String courseCode;
        int totalQuestions;
        int correctAnswers;
        double percentage;

        if (newFormat) {
            username = parts[0].trim();
            courseCode = normalizeCourseCode(parts[1]);
            totalQuestions = parseIntOrDefault(parts[3], 0);
            correctAnswers = parseIntOrDefault(parts[4], 0);
            percentage = parseDoubleOrDefault(parts[5], computePercentage(correctAnswers, totalQuestions));
        } else {
            courseCode = normalizeCourseCode(parts[0]);
            totalQuestions = parseIntOrDefault(parts[2], 0);
            correctAnswers = parseIntOrDefault(parts[3], 0);
            percentage = parseDoubleOrDefault(parts[4], computePercentage(correctAnswers, totalQuestions));
            username = parts[6].trim();
        }

        if (username == null || username.trim().isEmpty() || username.equalsIgnoreCase("Unknown")) {
            return null;
        }

        return new ExamRecord(username.trim(), courseCode, percentage);
    }

    private boolean looksLikeDate(String value) {
        return value != null && value.contains("T") && value.contains("-") && value.contains(":");
    }

    private String normalizeCourseCode(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "").trim().toUpperCase();
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double parseDoubleOrDefault(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double computePercentage(int correctAnswers, int totalQuestions) {
        if (totalQuestions <= 0) {
            return 0.0;
        }
        return (correctAnswers * 100.0) / totalQuestions;
    }

    public static class ExamRecord {
        private final String username;
        private final String courseCode;
        private final double percentage;

        public ExamRecord(String username, String courseCode, double percentage) {
            this.username = username;
            this.courseCode = courseCode;
            this.percentage = percentage;
        }

        public String getUsername() {
            return username;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public double getPercentage() {
            return percentage;
        }
    }

    public static class LeaderboardEntry {
        private int rank;
        private final String username;
        private final int attempts;
        private final double averagePercentage;
        private final double bestPercentage;

        public LeaderboardEntry(int rank, String username, int attempts, double averagePercentage, double bestPercentage) {
            this.rank = rank;
            this.username = username;
            this.attempts = attempts;
            this.averagePercentage = averagePercentage;
            this.bestPercentage = bestPercentage;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getUsername() {
            return username;
        }

        public int getAttempts() {
            return attempts;
        }

        public double getAveragePercentage() {
            return averagePercentage;
        }

        public double getBestPercentage() {
            return bestPercentage;
        }

        public String getAveragePercentageDisplay() {
            return String.format("%.2f%%", averagePercentage);
        }

        public String getBestPercentageDisplay() {
            return String.format("%.2f%%", bestPercentage);
        }
    }
}
