package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

public class ScheduleExamController {
    private static final String TERMS_FILE = AppPaths.packageResourceFile("terms.csv").toString();
    private static final String COURSES_FILE = AppPaths.packageResourceFile("courses.csv").toString();
    private static final String EXAMS_FILE = AppPaths.resourceFile("exams.csv").toString();
    private static final String QUESTIONS_FILE = AppPaths.packageResourceFile("questions.csv").toString();
    private static final String SCHEDULE_FILE = AppPaths.appFile("schedule.csv").toString();
    private static final String SCHEDULE_DRAFT_FILE = AppPaths.appFile("schedule_drafts.csv").toString();

    @FXML
    private Button backButton;
    @FXML
    private Label facilityRoomLabel;
    @FXML
    private Label facilityCapacityLabel;
    @FXML
    private Label facilityAvailabilityLabel;
    @FXML
    private ChoiceBox<String> termChoice;
    @FXML
    private ChoiceBox<String> courseChoice;
    @FXML
    private ChoiceBox<String> templateChoice;
    @FXML
    private DatePicker examDatePicker;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;
    @FXML
    private Button saveDraftButton;
    @FXML
    private Button publishButton;
    @FXML
    private Label statusLabel;

    private final Map<String, TermOption> termByLabel = new LinkedHashMap<>();
    private final Map<String, CourseOption> courseByLabel = new LinkedHashMap<>();
    private final Map<String, TemplateOption> templateByLabel = new LinkedHashMap<>();

    private final List<TermOption> allTerms = new ArrayList<>();
    private final List<CourseOption> allCourses = new ArrayList<>();
    private final List<TemplateOption> allTemplates = new ArrayList<>();

    private Runnable onBackRequested;
    private Runnable onPublished;

    @FXML
    private void initialize() {
        configureSpinners();
        wireInputs();
        refreshFromData();
    }

    public void setOnBackRequested(Runnable onBackRequested) {
        this.onBackRequested = onBackRequested;
    }

    public void setOnPublished(Runnable onPublished) {
        this.onPublished = onPublished;
    }

    public void refreshFromData() {
        loadAllCsvData();
        populateTerms();
        updateFacilityCard();
        validateForm();
    }

    private void configureSpinners() {
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        hourSpinner.setEditable(false);
        minuteSpinner.setEditable(false);
    }

    private void wireInputs() {
        backButton.setOnAction(event -> {
            if (onBackRequested != null) {
                onBackRequested.run();
            }
        });

        termChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            populateCoursesForTerm(newValue);
            validateForm();
        });

        courseChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            populateTemplatesForCourse(newValue);
            updateFacilityCard();
            validateForm();
        });

        templateChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> validateForm());
        examDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> validateForm());
        hourSpinner.valueProperty().addListener((obs, oldValue, newValue) -> validateForm());
        minuteSpinner.valueProperty().addListener((obs, oldValue, newValue) -> validateForm());

        saveDraftButton.setOnAction(event -> saveDraft());
        publishButton.setOnAction(event -> publishSchedule());
    }

    private void loadAllCsvData() {
        allTerms.clear();
        allCourses.clear();
        allTemplates.clear();

        loadTermsFromCsv();
        loadCoursesFromCsv();
        loadTemplatesFromCsv();

        if (allTerms.isEmpty() || allCourses.isEmpty()) {
            deriveTermAndCourseFallbackFromQuestions();
        }
        if (allTemplates.isEmpty()) {
            deriveTemplateFallbackFromExamsLegacy();
        }

        allTerms.sort(Comparator.comparing(term -> normalized(term.label())));
        allCourses.sort(Comparator.comparing(course -> normalized(course.label())));
        allTemplates.sort(Comparator.comparing(template -> normalized(template.label())));

        setStatus("Loaded " + allTerms.size() + " terms, " + allCourses.size() + " courses, " + allTemplates.size() + " templates.");
    }

    private void loadTermsFromCsv() {
        Path path = Paths.get(TERMS_FILE);
        if (Files.notExists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            CsvHeader header = null;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line == null || line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.isEmpty()) {
                    continue;
                }
                if (lineNo == 1 && looksLikeHeader(values)) {
                    header = CsvHeader.from(values);
                    continue;
                }
                if (header == null) {
                    if (values.size() < 2) {
                        continue;
                    }
                    String id = valueAt(values, 0);
                    String name = valueAt(values, 1);
                    String year = valueAt(values, 2);
                    allTerms.add(new TermOption(id, name, year));
                } else {
                    String id = header.get(values, "term_id", "termid", "id", "term");
                    String name = header.get(values, "term_name", "termname", "name");
                    String year = header.get(values, "academic_year", "academicyear", "year");
                    allTerms.add(new TermOption(id, name, year));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void loadCoursesFromCsv() {
        Path path = Paths.get(COURSES_FILE);
        if (Files.notExists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            CsvHeader header = null;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line == null || line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.isEmpty()) {
                    continue;
                }
                if (lineNo == 1 && looksLikeHeader(values)) {
                    header = CsvHeader.from(values);
                    continue;
                }

                CourseOption row;
                if (header == null) {
                    if (values.size() < 4) {
                        continue;
                    }
                    row = new CourseOption(valueAt(values, 0), valueAt(values, 1), valueAt(values, 2), valueAt(values, 3));
                } else {
                    row = new CourseOption(
                            header.get(values, "course_id", "courseid", "id"),
                            header.get(values, "course_name", "coursename", "name"),
                            header.get(values, "course_code", "coursecode", "code", "course"),
                            header.get(values, "term_id", "termid", "term")
                    );
                }
                if (!row.label().isBlank()) {
                    allCourses.add(row);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void loadTemplatesFromCsv() {
        Path path = Paths.get(EXAMS_FILE);
        if (Files.notExists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            CsvHeader header = null;
            int rowNo = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (line == null || line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.isEmpty()) {
                    continue;
                }
                if (rowNo == 1 && looksLikeHeader(values)) {
                    header = CsvHeader.from(values);
                    continue;
                }

                TemplateOption row;
                if (header != null && header.containsAny("examid", "examtitle", "courseid", "templatename")) {
                    String examId = header.get(values, "exam_id", "examid", "id");
                    String title = header.get(values, "exam_title", "examtitle", "title");
                    String courseId = header.get(values, "course_id", "courseid");
                    String templateName = header.get(values, "template_name", "templatename", "template");
                    row = new TemplateOption(examId, title, courseId, templateName, "", "", "", 0, 0, 0, "");
                } else {
                    String term = valueAt(values, 0);
                    String courseCode = valueAt(values, 1);
                    String courseName = valueAt(values, 2);
                    int totalQuestions = parseInt(valueAt(values, 3), 0);
                    int timeLimitSeconds = parseInt(valueAt(values, 4), 0);
                    int marks = parseInt(valueAt(values, 5), 0);
                    String mode = valueAt(values, 6);
                    String examId = "EX" + String.format(Locale.US, "%03d", Math.max(1, rowNo - 1));
                    String title = courseCode + " Assessment";
                    String templateName = mode == null || mode.isBlank() ? "Standard" : mode;
                    row = new TemplateOption(examId, title, courseCode, templateName, term, courseCode, courseName, totalQuestions, timeLimitSeconds, marks, mode);
                }
                if (!row.label().isBlank()) {
                    allTemplates.add(row);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void deriveTermAndCourseFallbackFromQuestions() {
        Path path = Paths.get(QUESTIONS_FILE);
        if (Files.notExists(path)) {
            return;
        }

        Set<String> knownTermIds = allTerms.stream().map(TermOption::termId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> knownCourseIds = allCourses.stream().map(CourseOption::courseId).collect(Collectors.toCollection(LinkedHashSet::new));

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line == null || line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() < 3) {
                    continue;
                }

                String termId = valueAt(values, 0);
                String courseCode = valueAt(values, 1);
                String courseName = valueAt(values, 2);

                if (!termId.isBlank() && knownTermIds.add(termId)) {
                    allTerms.add(new TermOption(termId, "Term " + termId, ""));
                }

                String courseId = courseCode;
                if (!courseId.isBlank() && knownCourseIds.add(courseId)) {
                    allCourses.add(new CourseOption(courseId, courseName, courseCode, termId));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void deriveTemplateFallbackFromExamsLegacy() {
        Path path = Paths.get(EXAMS_FILE);
        if (Files.notExists(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int row = 0;
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("term,coursecode,")) {
                    continue;
                }
                row++;
                List<String> values = parseCsvLine(line);
                if (values.size() < 8) {
                    continue;
                }
                String term = valueAt(values, 0);
                String courseCode = valueAt(values, 1);
                String courseName = valueAt(values, 2);
                int totalQuestions = parseInt(valueAt(values, 3), 0);
                int timeLimitSeconds = parseInt(valueAt(values, 4), 0);
                int marks = parseInt(valueAt(values, 5), 0);
                String mode = valueAt(values, 6);
                String examId = "EX" + String.format(Locale.US, "%03d", row);
                TemplateOption option = new TemplateOption(
                        examId,
                        courseCode + " Assessment",
                        courseCode,
                        mode == null || mode.isBlank() ? "Standard" : mode,
                        term,
                        courseCode,
                        courseName,
                        totalQuestions,
                        timeLimitSeconds,
                        marks,
                        mode
                );
                allTemplates.add(option);
            }
        } catch (IOException ignored) {
        }
    }

    private void populateTerms() {
        String previous = termChoice.getValue();
        termByLabel.clear();

        for (TermOption term : allTerms) {
            String label = term.label();
            if (!label.isBlank()) {
                termByLabel.put(label, term);
            }
        }

        termChoice.getItems().setAll(termByLabel.keySet());
        if (previous != null && termByLabel.containsKey(previous)) {
            termChoice.getSelectionModel().select(previous);
        } else if (!termChoice.getItems().isEmpty()) {
            termChoice.getSelectionModel().selectFirst();
        }

        populateCoursesForTerm(termChoice.getValue());
    }

    private void populateCoursesForTerm(String termLabel) {
        String previous = courseChoice.getValue();
        courseByLabel.clear();

        TermOption selectedTerm = termByLabel.get(termLabel);
        String selectedTermId = selectedTerm == null ? "" : selectedTerm.termId();

        for (CourseOption course : allCourses) {
            if (selectedTermId.isBlank() || selectedTermId.equals(course.termId())) {
                courseByLabel.put(course.label(), course);
            }
        }

        courseChoice.getItems().setAll(courseByLabel.keySet());
        if (previous != null && courseByLabel.containsKey(previous)) {
            courseChoice.getSelectionModel().select(previous);
        } else if (!courseChoice.getItems().isEmpty()) {
            courseChoice.getSelectionModel().selectFirst();
        } else {
            templateChoice.getItems().clear();
            templateByLabel.clear();
        }

        populateTemplatesForCourse(courseChoice.getValue());
    }

    private void populateTemplatesForCourse(String courseLabel) {
        String previous = templateChoice.getValue();
        templateByLabel.clear();

        CourseOption selectedCourse = courseByLabel.get(courseLabel);
        String selectedCourseId = selectedCourse == null ? "" : selectedCourse.courseId();
        String selectedCourseCode = selectedCourse == null ? "" : selectedCourse.courseCode();
        TermOption selectedTerm = termByLabel.get(termChoice.getValue());
        String selectedTermId = selectedTerm == null ? "" : selectedTerm.termId();

        for (TemplateOption template : allTemplates) {
            boolean byCourseId = !selectedCourseId.isBlank() && selectedCourseId.equalsIgnoreCase(template.courseId());
            boolean byCourseCode = !selectedCourseCode.isBlank() && selectedCourseCode.equalsIgnoreCase(template.courseCode());
            boolean byTerm = selectedTermId.isBlank() || template.term().isBlank() || selectedTermId.equalsIgnoreCase(template.term());
            if ((byCourseId || byCourseCode) && byTerm) {
                templateByLabel.put(template.label(), template);
            }
        }

        templateChoice.getItems().setAll(templateByLabel.keySet());
        if (previous != null && templateByLabel.containsKey(previous)) {
            templateChoice.getSelectionModel().select(previous);
        } else if (!templateChoice.getItems().isEmpty()) {
            templateChoice.getSelectionModel().selectFirst();
        }
    }

    private void updateFacilityCard() {
        CourseOption course = courseByLabel.get(courseChoice.getValue());
        TemplateOption template = templateByLabel.get(templateChoice.getValue());

        if (course == null) {
            facilityRoomLabel.setText("N/A");
            facilityCapacityLabel.setText("N/A");
            facilityAvailabilityLabel.setText("Waiting");
            return;
        }

        int roomNumber = Math.abs(course.courseCode().hashCode() % 18) + 201;
        int capacity = 30;
        if (template != null && template.totalQuestions() > 0) {
            capacity = Math.max(30, template.totalQuestions() * 2);
        }

        facilityRoomLabel.setText("Room " + roomNumber);
        facilityCapacityLabel.setText(String.valueOf(capacity));
        facilityAvailabilityLabel.setText(template == null ? "Checking" : "Ready");
    }

    private void validateForm() {
        boolean valid = termChoice.getValue() != null
                && courseChoice.getValue() != null
                && templateChoice.getValue() != null
                && examDatePicker.getValue() != null
                && hourSpinner.getValue() != null
                && minuteSpinner.getValue() != null;

        publishButton.setDisable(!valid);
        if (!valid) {
            setStatus("Complete all required fields to publish.");
        }
    }

    private void saveDraft() {
        TermOption term = termByLabel.get(termChoice.getValue());
        CourseOption course = courseByLabel.get(courseChoice.getValue());
        TemplateOption template = templateByLabel.get(templateChoice.getValue());
        LocalDate date = examDatePicker.getValue();

        if (term == null || course == null || template == null || date == null) {
            setStatus("Cannot save draft: missing required fields.");
            return;
        }

        String time = String.format(Locale.US, "%02d:%02d", hourSpinner.getValue(), minuteSpinner.getValue());

        try {
            appendDraft(term.termId(), course.courseCode(), template.examId(), date.toString(), time, template.templateName());
            setStatus("Draft saved successfully.");
        } catch (IOException ex) {
            setStatus("Unable to save draft.");
        }
    }

    private void publishSchedule() {
        if (publishButton.isDisabled()) {
            setStatus("Complete all required fields to publish.");
            return;
        }

        TermOption term = termByLabel.get(termChoice.getValue());
        CourseOption course = courseByLabel.get(courseChoice.getValue());
        TemplateOption template = templateByLabel.get(templateChoice.getValue());
        LocalDate date = examDatePicker.getValue();
        String time = String.format(Locale.US, "%02d:%02d", hourSpinner.getValue(), minuteSpinner.getValue());

        if (term == null || course == null || template == null || date == null) {
            setStatus("Unable to publish: invalid schedule details.");
            return;
        }

        try {
            appendPublishedSchedule(template.examId(), term.termId(), course.courseCode(), date.toString(), time);
            setStatus("Exam published for " + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " at " + time + ".");
            if (onPublished != null) {
                onPublished.run();
            }
        } catch (IOException ex) {
            setStatus("Unable to publish schedule.");
        }
    }

    private void appendDraft(String term, String courseCode, String examId, String date, String startTime, String templateName) throws IOException {
        Path path = Paths.get(SCHEDULE_DRAFT_FILE);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        boolean needsHeader = Files.notExists(path) || Files.size(path) == 0;
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            if (needsHeader) {
                writer.write("examId,term,course,date,startTime,template,state");
                writer.newLine();
            }
            writer.write(String.join(",",
                    csv(examId),
                    csv(term),
                    csv(courseCode),
                    csv(date),
                    csv(startTime),
                    csv(templateName),
                    "DRAFT"
            ));
            writer.newLine();
        }
    }

    private void appendPublishedSchedule(String examId, String term, String courseCode, String date, String startTime) throws IOException {
        Path path = Paths.get(SCHEDULE_FILE);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        boolean needsHeader = Files.notExists(path) || Files.size(path) == 0;
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            if (needsHeader) {
                writer.write("examId,term,course,date,startTime");
                writer.newLine();
            }
            writer.write(String.join(",",
                    csv(examId),
                    csv(term),
                    csv(courseCode.replace(" ", "")),
                    csv(date),
                    csv(startTime)
            ));
            writer.newLine();
        }
    }

    private String valueAt(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }

    private boolean looksLikeHeader(List<String> values) {
        String first = normalized(values.get(0));
        return first.contains("term") || first.contains("course") || first.contains("exam") || first.contains("id");
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
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

    private void setStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
                int index = keys.indexOf(normalizedAlias);
                if (index >= 0 && index < values.size()) {
                    return Objects.toString(values.get(index), "").trim();
                }
            }
            return "";
        }

        boolean containsAny(String... aliases) {
            for (String alias : aliases) {
                if (keys.contains(normalizeHeaderKey(alias))) {
                    return true;
                }
            }
            return false;
        }

        private static String normalizeHeaderKey(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        }
    }

    private record TermOption(String termId, String termName, String academicYear) {
        String label() {
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

    private record CourseOption(String courseId, String courseName, String courseCode, String termId) {
        String label() {
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

    private record TemplateOption(
            String examId,
            String examTitle,
            String courseId,
            String templateName,
            String term,
            String courseCode,
            String courseName,
            int totalQuestions,
            int timeLimitSeconds,
            int marks,
            String mode
    ) {
        String label() {
            String id = examId == null ? "" : examId.trim();
            String title = examTitle == null ? "" : examTitle.trim();
            String template = templateName == null ? "" : templateName.trim();
            if (!id.isBlank() && !title.isBlank()) {
                return id + " | " + title + " | " + (template.isBlank() ? "Template" : template);
            }
            if (!id.isBlank()) {
                return id;
            }
            if (!title.isBlank()) {
                return title;
            }
            return template;
        }
    }
}
