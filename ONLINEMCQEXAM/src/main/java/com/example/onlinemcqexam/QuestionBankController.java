package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
    private static final String QUESTIONS_FILE = AppPaths.packageResourceFile("questions.csv").toString();

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
        addNewQuestionButton.setOnAction(event -> addNewQuestion());
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

    private void addNewQuestion() {
        String term = safeChoiceValue(termChoice, "All Terms");
        String courseCode = safeChoiceValue(courseChoice, "All Courses");
        if ("All Terms".equals(term) || "All Courses".equals(courseCode)) {
            showAlert("Add New Question", "Select a specific term and course first.");
            return;
        }

        QuestionDraft draft = showQuestionDialog();
        if (draft == null) {
            return;
        }

        String courseName = resolveCourseName(term, courseCode);
        String questionId = nextQuestionId(term, courseCode, draft.difficulty());
        String line = String.join(",",
                csv(term),
                csv(courseCode),
                csv(courseName),
                csv(questionId),
                csv(draft.questionText()),
                csv(draft.optionA()),
                csv(draft.optionB()),
                csv(draft.optionC()),
                csv(draft.optionD()),
                String.valueOf(draft.correctIndex())
        );

        try {
            appendQuestionToCsv(line);
            loadCsvData();
            populateFilterChoices();
            if (termChoice.getItems().contains(term)) {
                termChoice.getSelectionModel().select(term);
            }
            refreshCourseChoicesForTerm(term);
            if (courseChoice.getItems().contains(courseCode)) {
                courseChoice.getSelectionModel().select(courseCode);
            }
            applyFilters();
            showAlert("Add New Question", "Question added successfully.");
        } catch (IOException ex) {
            showAlert("Add New Question", "Unable to save question.");
        }
    }

    private QuestionDraft showQuestionDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Question");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField questionField = new TextField();
        TextField optionAField = new TextField();
        TextField optionBField = new TextField();
        TextField optionCField = new TextField();
        TextField optionDField = new TextField();
        ChoiceBox<String> difficultyChoice = new ChoiceBox<>();
        difficultyChoice.getItems().setAll("Easy", "Medium", "Hard");
        difficultyChoice.getSelectionModel().selectFirst();
        ChoiceBox<String> correctChoice = new ChoiceBox<>();
        correctChoice.getItems().setAll("A", "B", "C", "D");
        correctChoice.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Question"), questionField);
        grid.addRow(1, new Label("Option A"), optionAField);
        grid.addRow(2, new Label("Option B"), optionBField);
        grid.addRow(3, new Label("Option C"), optionCField);
        grid.addRow(4, new Label("Option D"), optionDField);
        grid.addRow(5, new Label("Difficulty"), difficultyChoice);
        grid.addRow(6, new Label("Correct"), correctChoice);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait();
        if (dialog.getResult() != ButtonType.OK) {
            return null;
        }

        String questionText = questionField.getText() == null ? "" : questionField.getText().trim();
        String optionA = optionAField.getText() == null ? "" : optionAField.getText().trim();
        String optionB = optionBField.getText() == null ? "" : optionBField.getText().trim();
        String optionC = optionCField.getText() == null ? "" : optionCField.getText().trim();
        String optionD = optionDField.getText() == null ? "" : optionDField.getText().trim();
        if (questionText.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || optionC.isEmpty() || optionD.isEmpty()) {
            showAlert("Add New Question", "Fill in the question and all four options.");
            return null;
        }

        int correctIndex = switch (Objects.toString(correctChoice.getValue(), "A")) {
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 1;
        };
        String difficulty = Objects.toString(difficultyChoice.getValue(), "Easy");
        return new QuestionDraft(questionText, optionA, optionB, optionC, optionD, correctIndex, difficulty);
    }

    private void appendQuestionToCsv(String line) throws IOException {
        Path path = Paths.get(QUESTIONS_FILE);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        boolean needsHeader = Files.notExists(path) || Files.size(path) == 0;
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            if (needsHeader) {
                writer.write("# term,courseCode,courseName,questionId,question,optionA,optionB,optionC,optionD,correctIndex(1-4)");
                writer.newLine();
            }
            writer.write(line);
            writer.newLine();
        }
    }

    private String resolveCourseName(String term, String courseCode) {
        for (QuestionRow row : allQuestions) {
            if (term.equals(row.term) && courseCode.equals(row.course) && row.courseName != null && !row.courseName.isBlank()) {
                return row.courseName;
            }
        }
        return courseCode;
    }

    private String nextQuestionId(String term, String courseCode, String difficulty) {
        String normalizedCourse = courseCode.replace(" ", "").trim().toUpperCase(Locale.ROOT);
        String difficultyMarker = switch (normalized(difficulty)) {
            case "medium" -> "M";
            case "hard" -> "H";
            default -> "E";
        };
        int max = 0;
        for (QuestionRow row : allQuestions) {
            String id = row.questionId == null ? "" : row.questionId.trim().toUpperCase(Locale.ROOT);
            String basePrefix = (term + "-" + normalizedCourse).toUpperCase(Locale.ROOT);
            if (!id.startsWith(basePrefix) || !id.contains("-Q")) {
                continue;
            }
            String suffix = id.substring(id.lastIndexOf("-Q") + 2);
            try {
                max = Math.max(max, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
            }
        }
        return term + "-" + normalizedCourse + "-" + difficultyMarker + "-Q" + (max + 1);
    }

    private String safeChoiceValue(ChoiceBox<String> choiceBox, String fallback) {
        String value = choiceBox.getValue();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Path resolveQuestionCsvPath() {
        Path primary = AppPaths.packageResourceFile("questions.csv");
        if (Files.exists(primary)) {
            return primary;
        }

        Path alias = AppPaths.packageResourceFile("question.csv");
        if (Files.exists(alias)) {
            return alias;
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
        String courseName = header.get(values, "course_name", "coursename", "coursetitle");
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
        return new QuestionRow(id, term, course, courseName, normalizeDifficulty(difficulty), text, optionA, optionB, optionC, optionD, answer);
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

        String courseName = values.size() > 2 ? valueAt(values, 2) : course;
        return new QuestionRow(id, term, course, courseName, normalizeDifficulty(difficulty), text, optionA, optionB, optionC, optionD, answer);
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
            String courseName,
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

    private record QuestionDraft(
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            int correctIndex,
            String difficulty
    ) {
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
