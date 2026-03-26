package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class QuestionBankController {
    @FXML
    private TextField searchField;
    @FXML
    private Button clearSearchButton;
    @FXML
    private Button filterActionButton;
    @FXML
    private ToggleButton chipAll;
    @FXML
    private ToggleButton chipEasy;
    @FXML
    private ToggleButton chipMedium;
    @FXML
    private ToggleButton chipHard;
    @FXML
    private VBox questionCardContainer;
    @FXML
    private Button addNewQuestionButton;
    @FXML
    private Button backButton;
    @FXML
    private ChoiceBox<String> termChoice;
    @FXML
    private ChoiceBox<String> courseChoice;
    @FXML
    private Label totalQuestionsLabel;
    @FXML
    private Label showingStatusLabel;
    @FXML
    private Label repoHealthPercentLabel;
    @FXML
    private ProgressBar repoHealthBar;

    private final ToggleGroup difficultyGroup = new ToggleGroup();
    private final List<QuestionRow> allQuestions = new ArrayList<>();
    private String activeDifficulty = "All";
    private Runnable onBackRequested;

    @FXML
    private void initialize() {
        wireDifficultyChips();
        wireInputs();
        loadCsvData();
        populateFilterChoices();
        applyFilters();
    }

    private void wireDifficultyChips() {
        chipAll.setToggleGroup(difficultyGroup);
        chipEasy.setToggleGroup(difficultyGroup);
        chipMedium.setToggleGroup(difficultyGroup);
        chipHard.setToggleGroup(difficultyGroup);
        chipAll.setSelected(true);

        difficultyGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                chipAll.setSelected(true);
                return;
            }
            ToggleButton selected = (ToggleButton) newValue;
            activeDifficulty = selected.getText();
            applyFilters();
        });
    }

    private void wireInputs() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        clearSearchButton.setOnAction(event -> searchField.clear());
        filterActionButton.setOnAction(event -> termChoice.requestFocus());
        backButton.setOnAction(event -> {
            if (onBackRequested != null) {
                onBackRequested.run();
            }
        });
        termChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            refreshCourseChoicesForTerm(newValue);
            applyFilters();
        });
        courseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        addNewQuestionButton.setOnAction(event -> showAddQuestionInfo());
    }

    public void setOnBackRequested(Runnable onBackRequested) {
        this.onBackRequested = onBackRequested;
    }

    private void loadCsvData() {
        allQuestions.clear();
        Path csvPath = resolveQuestionCsvPath();
        if (csvPath == null) {
            showAlert("Question CSV Not Found", "Could not find question.csv or questions.csv in resources.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            CsvHeader header = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line == null || line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.isEmpty()) {
                    continue;
                }

                if (lineNumber == 1 && looksLikeHeader(values)) {
                    header = CsvHeader.from(values);
                    continue;
                }

                QuestionRow parsed = header == null
                        ? parseWithoutHeader(values)
                        : parseWithHeader(values, header);

                if (parsed != null && !parsed.questionText.isBlank()) {
                    allQuestions.add(parsed);
                }
            }
        } catch (IOException ex) {
            showAlert("Question CSV Error", "Unable to load question CSV data.");
        }
    }

    private void populateFilterChoices() {
        Set<String> terms = allQuestions.stream()
                .map(q -> q.term)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> sortedTerms = new ArrayList<>(terms);
        sortedTerms.sort(Comparator.naturalOrder());

        termChoice.getItems().setAll("All Terms");
        termChoice.getItems().addAll(sortedTerms);
        termChoice.getSelectionModel().selectFirst();

        refreshCourseChoicesForTerm(termChoice.getValue());
    }

    private void refreshCourseChoicesForTerm(String term) {
        String selectedTerm = term == null ? "All Terms" : term;
        Set<String> courses = allQuestions.stream()
                .filter(q -> "All Terms".equals(selectedTerm) || selectedTerm.equals(q.term))
                .map(q -> q.course)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> sortedCourses = new ArrayList<>(courses);
        sortedCourses.sort(Comparator.naturalOrder());

        courseChoice.getItems().setAll("All Courses");
        courseChoice.getItems().addAll(sortedCourses);
        courseChoice.getSelectionModel().selectFirst();
    }

    private void applyFilters() {
        String keyword = normalized(searchField.getText());
        String selectedTerm = safeChoiceValue(termChoice, "All Terms");
        String selectedCourse = safeChoiceValue(courseChoice, "All Courses");
        String selectedDifficulty = activeDifficulty == null ? "All" : activeDifficulty;

        List<QuestionRow> filtered = allQuestions.stream()
                .filter(q -> "All Terms".equals(selectedTerm) || selectedTerm.equals(q.term))
                .filter(q -> "All Courses".equals(selectedCourse) || selectedCourse.equals(q.course))
                .filter(q -> "All".equalsIgnoreCase(selectedDifficulty) || selectedDifficulty.equalsIgnoreCase(q.difficulty))
                .filter(q -> keyword.isBlank() || normalized(q.questionId).contains(keyword) || normalized(q.questionText).contains(keyword))
                .collect(Collectors.toList());

        renderQuestionCards(filtered);
        updateStats(filtered, selectedTerm, selectedCourse);
    }

    private void renderQuestionCards(List<QuestionRow> rows) {
        questionCardContainer.getChildren().clear();

        for (int i = 0; i < rows.size(); i++) {
            QuestionRow row = rows.get(i);

            Label numberCapsule = new Label(String.valueOf(i + 1));
            numberCapsule.getStyleClass().add("qb-number-capsule");

            Label idLabel = new Label("[" + row.questionId + "]");
            idLabel.getStyleClass().add("qb-id-label");

            Label diffLabel = new Label(row.difficulty);
            diffLabel.getStyleClass().add("qb-difficulty-tag");
            diffLabel.getStyleClass().add("qb-difficulty-" + row.difficulty.toLowerCase(Locale.ROOT));

            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);
            HBox topRow = new HBox(8, numberCapsule, idLabel, topSpacer, diffLabel);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label questionText = new Label(row.questionText);
            questionText.setWrapText(true);
            questionText.getStyleClass().add("qb-question-text");

            VBox content = new VBox(10, topRow, questionText);
            content.getStyleClass().add("qb-question-card");
            questionCardContainer.getChildren().add(content);
        }
    }

    private void updateStats(List<QuestionRow> filtered, String term, String course) {
        int total = filtered.size();
        totalQuestionsLabel.setText(String.valueOf(total));

        String termText = "All Terms".equals(term) ? "all terms" : term;
        String courseText = "All Courses".equals(course) ? "all courses" : course;
        showingStatusLabel.setText("Showing " + total + " questions for " + termText + " - " + courseText);

        double healthRatio = calculateHealthRatio(allQuestions);
        repoHealthBar.setProgress(healthRatio);
        repoHealthPercentLabel.setText(String.format(Locale.US, "%.1f%%", healthRatio * 100.0));
    }

    private double calculateHealthRatio(List<QuestionRow> rows) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        long complete = rows.stream().filter(QuestionRow::isComplete).count();
        return complete / (double) rows.size();
    }

    private void showAddQuestionInfo() {
        showAlert("Add New Question", "Use your existing Teacher Add Question flow to persist a new question to CSV.");
    }

    private String safeChoiceValue(ChoiceBox<String> choiceBox, String fallback) {
        String value = choiceBox.getValue();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Path resolveQuestionCsvPath() {
        List<Path> candidates = List.of(
                Paths.get("src", "main", "resources", "com", "example", "onlinemcqexam", "question.csv"),
                Paths.get("src", "main", "resources", "com", "example", "onlinemcqexam", "questions.csv"),
                Paths.get("src", "main", "resources", "question.csv"),
                Paths.get("src", "main", "resources", "questions.csv")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean looksLikeHeader(List<String> values) {
        String first = normalized(values.get(0));
        return first.contains("question") || first.contains("term") || first.contains("course");
    }

    private QuestionRow parseWithHeader(List<String> values, CsvHeader header) {
        String id = header.get(values, "question_id", "questionid", "id", "examid");
        String term = header.get(values, "term");
        String course = header.get(values, "course", "coursecode");
        String difficulty = header.get(values, "difficulty");
        String text = header.get(values, "question_text", "question");
        String optionA = header.get(values, "option_a", "optiona");
        String optionB = header.get(values, "option_b", "optionb");
        String optionC = header.get(values, "option_c", "optionc");
        String optionD = header.get(values, "option_d", "optiond");
        String answer = header.get(values, "answer", "correctindex", "correct");

        if (id.isBlank()) {
            id = header.get(values, "questionid");
        }
        if (difficulty.isBlank()) {
            difficulty = inferDifficulty(id, term);
        }
        return new QuestionRow(id, term, course, normalizeDifficulty(difficulty), text, optionA, optionB, optionC, optionD, answer);
    }

    private QuestionRow parseWithoutHeader(List<String> values) {
        if (values.size() < 6) {
            return null;
        }

        String term = valueAt(values, 0);
        String course = valueAt(values, 1);
        String id = valueAt(values, 3);
        String text = valueAt(values, 4);
        String optionA = valueAt(values, 5);
        String optionB = valueAt(values, 6);
        String optionC = valueAt(values, 7);
        String optionD = valueAt(values, 8);
        String answer = valueAt(values, 9);
        String difficulty = inferDifficulty(id, term);

        return new QuestionRow(id, term, course, normalizeDifficulty(difficulty), text, optionA, optionB, optionC, optionD, answer);
    }

    private String inferDifficulty(String questionId, String term) {
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

        String normalizedTerm = normalized(term);
        if (normalizedTerm.startsWith("1-")) {
            return "Easy";
        }
        if (normalizedTerm.startsWith("2-")) {
            return "Medium";
        }
        return "Hard";
    }

    private String normalizeDifficulty(String difficulty) {
        String value = normalized(difficulty);
        if (value.startsWith("e")) {
            return "Easy";
        }
        if (value.startsWith("m")) {
            return "Medium";
        }
        if (value.startsWith("h")) {
            return "Hard";
        }
        return "Medium";
    }

    private String valueAt(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString());
        return values;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record CsvHeader(List<String> keys) {
        static CsvHeader from(List<String> headerValues) {
            List<String> normalizedKeys = headerValues.stream()
                    .map(CsvHeader::normalizeHeaderKey)
                    .toList();
            return new CsvHeader(normalizedKeys);
        }

        String get(List<String> values, String... aliases) {
            for (String alias : aliases) {
                String normalizedAlias = normalizeHeaderKey(alias);
                int idx = keys.indexOf(normalizedAlias);
                if (idx >= 0 && idx < values.size()) {
                    return Objects.toString(values.get(idx), "").trim();
                }
            }
            return "";
        }

        private static String normalizeHeaderKey(String value) {
            if (value == null) {
                return "";
            }
            // Keep letters only so variants like "# term" and "correctIndex(1-4)" match aliases.
            return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        }
    }

    private record QuestionRow(
            String questionId,
            String term,
            String course,
            String difficulty,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String answer
    ) {
        boolean isComplete() {
            return !questionId.isBlank()
                    && !term.isBlank()
                    && !course.isBlank()
                    && !questionText.isBlank()
                    && !optionA.isBlank()
                    && !optionB.isBlank()
                    && !optionC.isBlank()
                    && !optionD.isBlank()
                    && !answer.isBlank();
        }
    }
}
