package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateExamController {
    private static final int DEFAULT_SECONDS_PER_QUESTION = 120;
    private static final String EXAMS_FILE = AppPaths.resourceFile("exams.csv").toString();

    @FXML
    private ChoiceBox<String> termChoice;
    @FXML
    private ChoiceBox<String> courseChoice;
    @FXML
    private ToggleButton manualModeButton;
    @FXML
    private ToggleButton autoModeButton;
    @FXML
    private Label questionCountLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button backButton;
    @FXML
    private Button manageBankButton;
    @FXML
    private VBox questionCardContainer;
    @FXML
    private Button targetMinusButton;
    @FXML
    private Label targetCountValueLabel;
    @FXML
    private Button targetPlusButton;
    @FXML
    private Label computedTimeLabel;
    @FXML
    private Label computedMarksLabel;
    @FXML
    private Button saveExamButton;
    @FXML
    private Label statusLabel;

    private final ToggleGroup modeGroup = new ToggleGroup();
    private final List<QuestionRecord> allQuestions = new ArrayList<>();
    private final List<QuestionRecord> filteredQuestions = new ArrayList<>();
    private final Map<String, CheckBox> selectionMap = new LinkedHashMap<>();
    private int targetCount = 0;

    private Runnable onManageBankRequested;
    private Runnable onExamSaved;
    private Runnable onBackRequested;

    @FXML
    private void initialize() {
        configureModeToggle();
        loadQuestionsFromCsv();
        wireInputs();
        populateFilters();
        applyFiltersAndRender();
    }

    public void setOnManageBankRequested(Runnable onManageBankRequested) {
        this.onManageBankRequested = onManageBankRequested;
    }

    public void setOnBackRequested(Runnable onBackRequested) {
        this.onBackRequested = onBackRequested;
    }

    public void setOnExamSaved(Runnable onExamSaved) {
        this.onExamSaved = onExamSaved;
    }

    public void refreshFromData() {
        loadQuestionsFromCsv();
        populateFilters();
        applyFiltersAndRender();
    }

    private void configureModeToggle() {
        manualModeButton.setToggleGroup(modeGroup);
        autoModeButton.setToggleGroup(modeGroup);
        manualModeButton.setSelected(true);

        modeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                manualModeButton.setSelected(true);
                return;
            }
            updateSelectionModeState();
            recomputeMetrics();
        });
    }

    private void wireInputs() {
        termChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            populateCoursesForTerm(newValue);
            applyFiltersAndRender();
        });
        courseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());

        backButton.setOnAction(event -> {
            if (onBackRequested != null) {
                onBackRequested.run();
            }
        });

        manageBankButton.setOnAction(event -> {
            if (onManageBankRequested != null) {
                onManageBankRequested.run();
            }
        });

        targetMinusButton.setOnAction(event -> {
            targetCount = Math.max(0, targetCount - 1);
            updateTargetDisplay();
            recomputeMetrics();
        });
        targetPlusButton.setOnAction(event -> {
            targetCount = Math.min(filteredQuestions.size(), targetCount + 1);
            updateTargetDisplay();
            recomputeMetrics();
        });

        saveExamButton.setOnAction(event -> saveExam());
    }

    private void loadQuestionsFromCsv() {
        allQuestions.clear();
        Path csvPath = resolveQuestionCsvPath();
        if (csvPath == null) {
            setStatus("question.csv was not found.");
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

                QuestionRecord record = header == null
                        ? parseWithoutHeader(values)
                        : parseWithHeader(values, header);

                if (record != null && !record.questionId.isBlank() && !record.questionText.isBlank()) {
                    allQuestions.add(record);
                }
            }
            setStatus(allQuestions.isEmpty() ? "No questions loaded from CSV." : "Loaded " + allQuestions.size() + " questions from CSV.");
        } catch (IOException ex) {
            setStatus("Unable to read question.csv.");
        }
    }

    private void populateFilters() {
        String previousTerm = termChoice.getValue();
        Set<String> terms = allQuestions.stream()
                .map(question -> question.term)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> sortedTerms = new ArrayList<>(terms);
        sortedTerms.sort(Comparator.naturalOrder());

        termChoice.getItems().setAll("All Terms");
        termChoice.getItems().addAll(sortedTerms);
        if (previousTerm != null && termChoice.getItems().contains(previousTerm)) {
            termChoice.getSelectionModel().select(previousTerm);
        } else {
            termChoice.getSelectionModel().selectFirst();
        }

        populateCoursesForTerm(termChoice.getValue());
    }

    private void populateCoursesForTerm(String selectedTerm) {
        String previousCourse = courseChoice.getValue();
        Set<String> courses = allQuestions.stream()
                .filter(question -> "All Terms".equals(selectedTerm) || selectedTerm == null || selectedTerm.isBlank() || selectedTerm.equals(question.term))
                .map(QuestionRecord::displayCourse)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> sortedCourses = new ArrayList<>(courses);
        sortedCourses.sort(Comparator.naturalOrder());

        courseChoice.getItems().setAll("All Courses");
        courseChoice.getItems().addAll(sortedCourses);
        if (previousCourse != null && courseChoice.getItems().contains(previousCourse)) {
            courseChoice.getSelectionModel().select(previousCourse);
        } else {
            courseChoice.getSelectionModel().selectFirst();
        }
    }

    private void applyFiltersAndRender() {
        String selectedTerm = safeChoiceValue(termChoice, "All Terms");
        String selectedCourse = safeChoiceValue(courseChoice, "All Courses");
        String keyword = normalized(searchField.getText());

        filteredQuestions.clear();
        for (QuestionRecord question : allQuestions) {
            if (!"All Terms".equals(selectedTerm) && !selectedTerm.equals(question.term)) {
                continue;
            }
            if (!"All Courses".equals(selectedCourse) && !selectedCourse.equals(question.displayCourse())) {
                continue;
            }
            if (!keyword.isBlank() && !normalized(question.questionText).contains(keyword) && !normalized(question.questionId).contains(keyword)) {
                continue;
            }
            filteredQuestions.add(question);
        }

        renderQuestionCards();
        questionCountLabel.setText(filteredQuestions.size() + " questions found");

        if (targetCount > filteredQuestions.size()) {
            targetCount = filteredQuestions.size();
            updateTargetDisplay();
        }
        recomputeMetrics();
    }

    private void renderQuestionCards() {
        questionCardContainer.getChildren().clear();

        if (filteredQuestions.isEmpty()) {
            Label emptyLabel = new Label("No matching questions were found for the selected filters.");
            emptyLabel.getStyleClass().add("ce-status");
            questionCardContainer.getChildren().add(emptyLabel);
            return;
        }

        for (QuestionRecord question : filteredQuestions) {
            CheckBox checkBox = selectionMap.computeIfAbsent(question.questionId, key -> new CheckBox());
            checkBox.setDisable(!isManualMode());
            checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
                if (isManualMode()) {
                    targetCount = getSelectedQuestions().size();
                    updateTargetDisplay();
                }
                recomputeMetrics();
            });

            Label idLabel = new Label("[" + question.questionId + "]");
            idLabel.getStyleClass().add("ce-qid");

            Label questionText = new Label(question.questionText);
            questionText.setWrapText(true);
            questionText.getStyleClass().add("ce-qtext");

            Label difficultyPill = new Label(question.difficulty);
            difficultyPill.getStyleClass().addAll("ce-pill", difficultyStyleClass(question.difficulty));

            Label marksPill = new Label("+" + question.marks + " marks");
            marksPill.getStyleClass().add("ce-marks-pill");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox topRow = new HBox(8, checkBox, idLabel, spacer, difficultyPill, marksPill);
            topRow.setAlignment(Pos.CENTER_LEFT);

            VBox card = new VBox(8, topRow, questionText);
            card.getStyleClass().add("ce-question-card");
            questionCardContainer.getChildren().add(card);
        }
    }

    private void updateSelectionModeState() {
        boolean manual = isManualMode();
        for (CheckBox checkBox : selectionMap.values()) {
            checkBox.setDisable(!manual);
        }
        targetMinusButton.setDisable(manual);
        targetPlusButton.setDisable(manual);

        if (manual) {
            targetCount = getSelectedQuestions().size();
        } else if (targetCount == 0 && !filteredQuestions.isEmpty()) {
            targetCount = Math.min(10, filteredQuestions.size());
        }
        updateTargetDisplay();
    }

    private void recomputeMetrics() {
        List<QuestionRecord> effective = getEffectiveQuestions();
        int totalMarks = effective.stream().mapToInt(question -> question.marks).sum();
        int totalMinutes = effective.stream().mapToInt(question -> question.secondsPerQuestion).sum() / 60;

        computedMarksLabel.setText(String.valueOf(totalMarks));
        computedTimeLabel.setText(totalMinutes + " min");
        if (isManualMode()) {
            targetCount = effective.size();
            updateTargetDisplay();
        }
    }

    private List<QuestionRecord> getSelectedQuestions() {
        return filteredQuestions.stream()
                .filter(question -> {
                    CheckBox checkBox = selectionMap.get(question.questionId);
                    return checkBox != null && checkBox.isSelected();
                })
                .collect(Collectors.toList());
    }

    private List<QuestionRecord> getEffectiveQuestions() {
        if (isManualMode()) {
            return getSelectedQuestions();
        }
        int capped = Math.max(0, Math.min(targetCount, filteredQuestions.size()));
        return new ArrayList<>(filteredQuestions.subList(0, capped));
    }

    private void saveExam() {
        String term = safeChoiceValue(termChoice, "All Terms");
        String course = safeChoiceValue(courseChoice, "All Courses");
        if ("All Terms".equals(term) || "All Courses".equals(course)) {
            setStatus("Select a specific term and course before saving.");
            return;
        }

        List<QuestionRecord> selected = getEffectiveQuestions();
        if (selected.isEmpty()) {
            setStatus("Select questions (manual) or set a target count (auto-gen).");
            return;
        }

        int marks = selected.stream().mapToInt(question -> question.marks).sum();
        int totalQuestions = selected.size();
        int timeLimitSeconds = selected.stream().mapToInt(question -> question.secondsPerQuestion).sum();

        String courseCode = extractCourseCode(course);
        String courseName = extractCourseName(course);
        String mode = isManualMode() ? "MANUAL" : "AUTO";
        String questionIds = selected.stream().map(question -> question.questionId).collect(Collectors.joining(";"));

        try {
            appendExamRow(term, courseCode, courseName, totalQuestions, timeLimitSeconds, marks, mode, questionIds);
            setStatus("Exam saved with " + totalQuestions + " questions.");
            if (onExamSaved != null) {
                onExamSaved.run();
            }
        } catch (IOException ex) {
            setStatus("Unable to save exam to exams.csv.");
        }
    }

    private void appendExamRow(String term, String courseCode, String courseName, int totalQuestions, int timeLimitSeconds,
                               int marks, String mode, String questionIds) throws IOException {
        Path path = Paths.get(EXAMS_FILE);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        boolean hasHeader = Files.exists(path) && Files.size(path) > 0;
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            if (!hasHeader) {
                writer.write("term,courseCode,courseName,totalQuestions,timeLimitSeconds,marks,mode,questionIds");
                writer.newLine();
            }
            writer.write(csv(term) + "," + csv(courseCode) + "," + csv(courseName) + "," + totalQuestions + ","
                    + timeLimitSeconds + "," + marks + "," + csv(mode) + "," + csv(questionIds));
            writer.newLine();
        }
    }

    private String extractCourseCode(String courseDisplay) {
        int separator = courseDisplay.indexOf(':');
        if (separator > 0) {
            return courseDisplay.substring(0, separator).trim();
        }
        return courseDisplay.trim();
    }

    private String extractCourseName(String courseDisplay) {
        int separator = courseDisplay.indexOf(':');
        if (separator > 0 && separator + 1 < courseDisplay.length()) {
            return courseDisplay.substring(separator + 1).trim();
        }
        return courseDisplay.trim();
    }

    private void updateTargetDisplay() {
        targetCountValueLabel.setText(String.valueOf(Math.max(targetCount, 0)));
    }

    private String difficultyStyleClass(String difficulty) {
        String normalized = normalized(difficulty);
        if (normalized.startsWith("e")) {
            return "ce-pill-easy";
        }
        if (normalized.startsWith("h")) {
            return "ce-pill-hard";
        }
        return "ce-pill-medium";
    }

    private boolean isManualMode() {
        return manualModeButton.isSelected();
    }

    private String safeChoiceValue(ChoiceBox<String> choiceBox, String fallback) {
        String value = choiceBox.getValue();
        return value == null || value.isBlank() ? fallback : value;
    }

    private void setStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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

    private QuestionRecord parseWithHeader(List<String> values, CsvHeader header) {
        String id = header.get(values, "question_id", "questionid", "id");
        String term = header.get(values, "term");
        String courseCode = header.get(values, "course", "course_code", "coursecode", "module");
        String courseName = header.get(values, "course_name", "coursename", "course_title", "modulename");
        String difficulty = header.get(values, "difficulty");
        String marksRaw = header.get(values, "marks", "mark");
        String questionText = header.get(values, "question_text", "question", "text");
        String optionA = header.get(values, "option_a", "optiona");
        String optionB = header.get(values, "option_b", "optionb");
        String optionC = header.get(values, "option_c", "optionc");
        String optionD = header.get(values, "option_d", "optiond");
        String answer = header.get(values, "answer", "correct", "correctindex");

        String normalizedDifficulty = normalizeDifficulty(difficulty, id, term);
        int marks = parseMarksOrDefault(marksRaw, normalizedDifficulty);
        return new QuestionRecord(id, term, courseCode, courseName, normalizedDifficulty, marks, questionText,
                optionA, optionB, optionC, optionD, answer, DEFAULT_SECONDS_PER_QUESTION);
    }

    private QuestionRecord parseWithoutHeader(List<String> values) {
        if (values.size() >= 10) {
            String term = valueAt(values, 0);
            String courseCode = valueAt(values, 1);
            String courseName = valueAt(values, 2);
            String id = valueAt(values, 3);
            String questionText = valueAt(values, 4);
            String optionA = valueAt(values, 5);
            String optionB = valueAt(values, 6);
            String optionC = valueAt(values, 7);
            String optionD = valueAt(values, 8);
            String answer = valueAt(values, 9);
            String difficulty = normalizeDifficulty("", id, term);
            int marks = parseMarksOrDefault("", difficulty);
            return new QuestionRecord(id, term, courseCode, courseName, difficulty, marks, questionText,
                    optionA, optionB, optionC, optionD, answer, DEFAULT_SECONDS_PER_QUESTION);
        }
        return null;
    }

    private String normalizeDifficulty(String difficulty, String questionId, String term) {
        String value = normalized(difficulty);
        if (value.startsWith("e")) {
            return "EASY";
        }
        if (value.startsWith("m")) {
            return "MEDIUM";
        }
        if (value.startsWith("h")) {
            return "HARD";
        }

        String id = normalized(questionId);
        if (id.contains("-e-") || id.contains("easy")) {
            return "EASY";
        }
        if (id.contains("-m-") || id.contains("medium")) {
            return "MEDIUM";
        }
        if (id.contains("-h-") || id.contains("hard")) {
            return "HARD";
        }

        String termValue = normalized(term);
        if (termValue.startsWith("1-")) {
            return "EASY";
        }
        if (termValue.startsWith("2-")) {
            return "MEDIUM";
        }
        return "HARD";
    }

    private int parseMarksOrDefault(String marksRaw, String difficulty) {
        try {
            int value = Integer.parseInt(marksRaw == null ? "" : marksRaw.trim());
            return Math.max(value, 1);
        } catch (NumberFormatException ignored) {
            return switch (difficulty) {
                case "EASY" -> 5;
                case "MEDIUM" -> 10;
                default -> 15;
            };
        }
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
            return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        }
    }

    private record QuestionRecord(
            String questionId,
            String term,
            String courseCode,
            String courseName,
            String difficulty,
            int marks,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String answer,
            int secondsPerQuestion
    ) {
        String displayCourse() {
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
}
