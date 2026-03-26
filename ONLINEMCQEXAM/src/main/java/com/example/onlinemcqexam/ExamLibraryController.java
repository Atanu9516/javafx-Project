package com.example.onlinemcqexam;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExamLibraryController {
    @FXML
    private Label instructorNameLabel;
    @FXML
    private Label instructorRoleLabel;
    @FXML
    private Button dashboardNavButton;
    @FXML
    private Button scheduleNavButton;
    @FXML
    private Button analyticsNavButton;
    @FXML
    private Label academicYearLabel;
    @FXML
    private Label totalExamsLabel;
    @FXML
    private Label totalDeltaLabel;
    @FXML
    private Label upcomingExamsLabel;
    @FXML
    private Label unscheduledExamsLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ChoiceBox<String> termFilterChoice;
    @FXML
    private ChoiceBox<String> courseFilterChoice;
    @FXML
    private ChoiceBox<String> statusFilterChoice;
    @FXML
    private Button newExamButton;
    @FXML
    private Label examCountLabel;
    @FXML
    private VBox examCardContainer;

    private final List<ExamLibraryModels.TermRecord> terms = new ArrayList<>();
    private final List<ExamLibraryModels.CourseRecord> courses = new ArrayList<>();
    private final List<ExamLibraryModels.ScheduleRecord> schedules = new ArrayList<>();
    private final List<ExamLibraryModels.ExamRecord> exams = new ArrayList<>();

    private final Map<String, ExamLibraryModels.TermRecord> termByDisplay = new LinkedHashMap<>();
    private final Map<String, ExamLibraryModels.CourseRecord> courseByDisplay = new LinkedHashMap<>();

    private Runnable onDashboardRequested;
    private Runnable onScheduleRequested;
    private Runnable onAnalyticsRequested;
    private Runnable onNewExamRequested;

    @FXML
    private void initialize() {
        wireActions();
        refreshFromData();
    }

    public void setOnDashboardRequested(Runnable onDashboardRequested) {
        this.onDashboardRequested = onDashboardRequested;
    }

    public void setOnScheduleRequested(Runnable onScheduleRequested) {
        this.onScheduleRequested = onScheduleRequested;
    }

    public void setOnAnalyticsRequested(Runnable onAnalyticsRequested) {
        this.onAnalyticsRequested = onAnalyticsRequested;
    }

    public void setOnNewExamRequested(Runnable onNewExamRequested) {
        this.onNewExamRequested = onNewExamRequested;
    }

    public void refreshFromData() {
        loadData();
        populateFilters();
        applyFilters();
    }

    private void wireActions() {
        dashboardNavButton.setOnAction(event -> {
            if (onDashboardRequested != null) {
                onDashboardRequested.run();
            }
        });
        scheduleNavButton.setOnAction(event -> {
            if (onScheduleRequested != null) {
                onScheduleRequested.run();
            }
        });
        analyticsNavButton.setOnAction(event -> {
            if (onAnalyticsRequested != null) {
                onAnalyticsRequested.run();
            }
        });
        newExamButton.setOnAction(event -> {
            if (onNewExamRequested != null) {
                onNewExamRequested.run();
            }
        });

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        termFilterChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            repopulateCourseFilter();
            applyFilters();
        });
        courseFilterChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilterChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadData() {
        terms.clear();
        courses.clear();
        schedules.clear();
        exams.clear();

        loadTerms();
        loadCourses();
        loadSchedules();
        loadExams();

        deriveMissingTermAndCourse();
        instructorNameLabel.setText("Instructor");
        instructorRoleLabel.setText("Course Supervisor");
    }

    private void loadTerms() {
        Path path = resolveFirstExisting(
                "src/main/resources/com/example/onlinemcqexam/terms.csv",
                "src/main/resources/terms.csv"
        );
        if (path == null) {
            return;
        }

        List<List<String>> rows = readCsv(path);
        if (rows.isEmpty()) {
            return;
        }

        CsvHeader header = CsvHeader.from(rows.get(0));
        for (int i = 1; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            String termId = header.get(values, "term_id", "termid", "id", "term");
            String termName = header.get(values, "term_name", "termname", "name");
            String year = header.get(values, "academic_year", "academicyear", "year");
            if (!termId.isBlank() || !termName.isBlank()) {
                terms.add(new ExamLibraryModels.TermRecord(termId, termName, year));
            }
        }
    }

    private void loadCourses() {
        Path path = resolveFirstExisting(
                "src/main/resources/com/example/onlinemcqexam/courses.csv",
                "src/main/resources/courses.csv"
        );
        if (path == null) {
            return;
        }

        List<List<String>> rows = readCsv(path);
        if (rows.isEmpty()) {
            return;
        }

        CsvHeader header = CsvHeader.from(rows.get(0));
        for (int i = 1; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            String courseId = header.get(values, "course_id", "courseid", "id");
            String courseName = header.get(values, "course_name", "coursename", "name");
            String courseCode = header.get(values, "course_code", "coursecode", "code", "course");
            String termId = header.get(values, "term_id", "termid", "term");
            if (!courseId.isBlank() || !courseCode.isBlank() || !courseName.isBlank()) {
                courses.add(new ExamLibraryModels.CourseRecord(
                        courseId.isBlank() ? courseCode : courseId,
                        courseName,
                        courseCode.isBlank() ? courseId : courseCode,
                        termId
                ));
            }
        }
    }

    private void loadSchedules() {
        Path path = resolveFirstExisting("schedule.csv", "src/main/resources/schedule.csv");
        if (path == null) {
            return;
        }

        List<List<String>> rows = readCsv(path);
        if (rows.isEmpty()) {
            return;
        }

        CsvHeader header = CsvHeader.from(rows.get(0));
        boolean hasHeader = header.containsAny("examid", "term", "course", "date", "starttime");
        for (int i = hasHeader ? 1 : 0; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.size() < 5) {
                continue;
            }
            String examId;
            String term;
            String course;
            String date;
            String startTime;

            if (hasHeader) {
                examId = header.get(values, "examid", "exam_id");
                term = header.get(values, "term", "term_id", "termid");
                course = header.get(values, "course", "course_id", "courseid", "course_code", "coursecode");
                date = header.get(values, "date", "exam_date", "examdate");
                startTime = header.get(values, "starttime", "start_time", "time");
            } else {
                examId = valueAt(values, 0);
                term = valueAt(values, 1);
                course = valueAt(values, 2);
                date = valueAt(values, 3);
                startTime = valueAt(values, 4);
            }

            LocalDate parsedDate = parseDate(date);
            schedules.add(new ExamLibraryModels.ScheduleRecord(
                    examId.trim(),
                    term.trim(),
                    canonicalCourse(course),
                    parsedDate,
                    startTime.trim()
            ));
        }
    }

    private void loadExams() {
        Path path = resolveFirstExisting("src/main/resources/exams.csv");
        if (path == null) {
            return;
        }

        List<List<String>> rows = readCsv(path);
        if (rows.isEmpty()) {
            return;
        }

        CsvHeader header = CsvHeader.from(rows.get(0));
        boolean modern = header.containsAny("examid", "examtitle", "courseid", "termid", "questioncount", "durationminutes");

        int serial = 0;
        Map<String, ExamLibraryModels.ScheduleRecord> latestScheduleByExamId = schedules.stream()
                .collect(Collectors.toMap(
                        schedule -> schedule.examId().toUpperCase(Locale.ROOT),
                        schedule -> schedule,
                        (left, right) -> right
                ));

        for (int i = 1; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.isEmpty()) {
                continue;
            }
            serial++;

            String examId;
            String examTitle;
            String courseId;
            String termId;
            int questionCount;
            int durationMinutes;
            String status;
            LocalDate examDate;

            if (modern) {
                examId = header.get(values, "exam_id", "examid", "id");
                examTitle = header.get(values, "exam_title", "examtitle", "title");
                courseId = header.get(values, "course_id", "courseid", "course", "course_code", "coursecode");
                termId = header.get(values, "term_id", "termid", "term");
                questionCount = parseInt(header.get(values, "question_count", "questioncount", "totalquestions", "questions"), 0);
                durationMinutes = parseInt(header.get(values, "duration_minutes", "durationminutes", "timelimitminutes", "duration"), 0);
                status = header.get(values, "status");
                examDate = parseDate(header.get(values, "exam_date", "examdate", "date"));
            } else {
                examId = String.format(Locale.US, "EX-%04d", serial + 9000);
                termId = valueAt(values, 0);
                String courseCode = valueAt(values, 1);
                String courseName = valueAt(values, 2);
                questionCount = parseInt(valueAt(values, 3), 0);
                durationMinutes = Math.max(1, parseInt(valueAt(values, 4), 0) / 60);
                examTitle = courseName.isBlank() ? (courseCode + " Assessment") : courseName + " Assessment";
                courseId = canonicalCourse(courseCode);
                status = "";
                examDate = null;
            }

            String normalizedId = examId.trim().toUpperCase(Locale.ROOT);
            ExamLibraryModels.ScheduleRecord schedule = latestScheduleByExamId.get(normalizedId);
            if (schedule != null) {
                if (examDate == null) {
                    examDate = schedule.date();
                }
                if (status.isBlank()) {
                    status = resolveStatusFromDate(schedule.date());
                }
                if (termId.isBlank()) {
                    termId = schedule.termId();
                }
                if (courseId.isBlank()) {
                    courseId = schedule.courseId();
                }
            }

            if (status.isBlank()) {
                status = examDate == null ? "NOT SCHEDULED" : resolveStatusFromDate(examDate);
            }

            exams.add(new ExamLibraryModels.ExamRecord(
                    examId,
                    examTitle,
                    canonicalCourse(courseId),
                    termId,
                    questionCount,
                    durationMinutes,
                    normalizeStatus(status),
                    examDate
            ));
        }
    }

    private void deriveMissingTermAndCourse() {
        Set<String> knownTerms = terms.stream().map(term -> term.termId().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> knownCourses = courses.stream().map(course -> canonicalCourse(course.courseCode())).collect(Collectors.toSet());

        for (ExamLibraryModels.ExamRecord exam : exams) {
            if (!exam.termId().isBlank() && knownTerms.add(exam.termId().toUpperCase(Locale.ROOT))) {
                terms.add(new ExamLibraryModels.TermRecord(exam.termId(), "Term " + exam.termId(), ""));
            }
            if (!exam.courseId().isBlank() && knownCourses.add(canonicalCourse(exam.courseId()))) {
                courses.add(new ExamLibraryModels.CourseRecord(exam.courseId(), exam.examTitle(), exam.courseId(), exam.termId()));
            }
        }
    }

    private void populateFilters() {
        termByDisplay.clear();
        courseByDisplay.clear();

        termFilterChoice.getItems().clear();
        termFilterChoice.getItems().add("All Terms");
        terms.stream()
                .sorted(Comparator.comparing(ExamLibraryModels.TermRecord::display, String::compareToIgnoreCase))
                .forEach(term -> {
                    String display = term.display();
                    termByDisplay.put(display, term);
                    termFilterChoice.getItems().add(display);
                });
        termFilterChoice.getSelectionModel().selectFirst();

        statusFilterChoice.getItems().setAll("All Status", "UPCOMING", "COMPLETE", "NOT SCHEDULED");
        statusFilterChoice.getSelectionModel().selectFirst();

        repopulateCourseFilter();
    }

    private void repopulateCourseFilter() {
        ExamLibraryModels.TermRecord selectedTerm = termByDisplay.get(termFilterChoice.getValue());
        courseByDisplay.clear();

        courseFilterChoice.getItems().clear();
        courseFilterChoice.getItems().add("All Courses");

        courses.stream()
                .filter(course -> selectedTerm == null || selectedTerm.termId().isBlank() || selectedTerm.termId().equalsIgnoreCase(course.termId()))
                .sorted(Comparator.comparing(ExamLibraryModels.CourseRecord::display, String::compareToIgnoreCase))
                .forEach(course -> {
                    String display = course.display();
                    courseByDisplay.put(display, course);
                    courseFilterChoice.getItems().add(display);
                });
        courseFilterChoice.getSelectionModel().selectFirst();
    }

    private void applyFilters() {
        String search = normalized(searchField.getText());
        ExamLibraryModels.TermRecord selectedTerm = termByDisplay.get(termFilterChoice.getValue());
        ExamLibraryModels.CourseRecord selectedCourse = courseByDisplay.get(courseFilterChoice.getValue());
        String selectedStatus = statusFilterChoice.getValue();

        List<ExamDisplayRow> rows = exams.stream()
                .map(this::toDisplayRow)
                .filter(row -> selectedTerm == null || selectedTerm.termId().isBlank() || selectedTerm.termId().equalsIgnoreCase(row.termId))
                .filter(row -> selectedCourse == null || selectedCourse.courseCode().isBlank() || canonicalCourse(selectedCourse.courseCode()).equalsIgnoreCase(row.courseId))
                .filter(row -> selectedStatus == null || "All Status".equalsIgnoreCase(selectedStatus) || row.status.equalsIgnoreCase(selectedStatus))
                .filter(row -> search.isBlank()
                        || normalized(row.examId).contains(search)
                        || normalized(row.examTitle).contains(search)
                        || normalized(row.courseName).contains(search))
                .sorted(Comparator.comparing((ExamDisplayRow row) -> row.examDate == null ? LocalDate.MIN : row.examDate).reversed())
                .collect(Collectors.toList());

        renderExamCards(rows);
        updateMetrics(rows);
        updateAcademicIndicator(selectedTerm);
    }

    private ExamDisplayRow toDisplayRow(ExamLibraryModels.ExamRecord exam) {
        ExamLibraryModels.CourseRecord course = findCourseByCode(exam.courseId());
        ExamLibraryModels.TermRecord term = findTermById(exam.termId());

        String courseName = course == null ? exam.courseId() : course.display();
        String termName = term == null ? exam.termId() : term.display();

        return new ExamDisplayRow(
                exam.examId(),
                exam.examTitle(),
                exam.courseId(),
                courseName,
                exam.termId(),
                termName,
                exam.questionCount(),
                exam.durationMinutes(),
                exam.status(),
                exam.examDate()
        );
    }

    private void renderExamCards(List<ExamDisplayRow> rows) {
        examCardContainer.getChildren().clear();
        examCountLabel.setText(rows.size() + " found");

        for (ExamDisplayRow row : rows) {
            Label id = new Label(row.examId);
            id.getStyleClass().add("el-exam-id");

            Label title = new Label(row.examTitle);
            title.getStyleClass().add("el-exam-title");

            String metaText = row.termName + " • " + row.courseName + " • Q=" + row.questionCount + " • " + row.durationMinutes + " min";
            Label meta = new Label(metaText);
            meta.getStyleClass().add("el-exam-meta");

            VBox left = new VBox(4, id, title, meta);
            HBox.setHgrow(left, Priority.ALWAYS);

            Label status = new Label(row.status);
            status.getStyleClass().addAll("el-status-pill", statusStyleClass(row.status));

            Button edit = new Button("Edit");
            edit.getStyleClass().add("el-icon-btn");
            edit.setOnAction(event -> {
                if (onNewExamRequested != null) {
                    onNewExamRequested.run();
                }
            });

            Button view = new Button("View");
            view.getStyleClass().add("el-icon-btn");
            view.setOnAction(event -> {
                if (onScheduleRequested != null) {
                    onScheduleRequested.run();
                }
            });

            Button menu = new Button("...");
            menu.getStyleClass().add("el-icon-btn");

            HBox right = new HBox(8, status, edit, view, menu);
            right.setAlignment(Pos.CENTER_RIGHT);

            HBox rowNode = new HBox(10, left, right);
            rowNode.setAlignment(Pos.CENTER_LEFT);
            rowNode.getStyleClass().add("el-exam-card");

            examCardContainer.getChildren().add(rowNode);
        }
    }

    private void updateMetrics(List<ExamDisplayRow> rows) {
        int total = rows.size();
        int upcoming = (int) rows.stream().filter(row -> "UPCOMING".equalsIgnoreCase(row.status)
                && row.examDate != null
                && row.examDate.isAfter(LocalDate.now())).count();
        int unscheduled = (int) rows.stream().filter(row -> "NOT SCHEDULED".equalsIgnoreCase(row.status) || row.examDate == null).count();

        totalExamsLabel.setText(String.valueOf(total));
        upcomingExamsLabel.setText(String.valueOf(upcoming));
        unscheduledExamsLabel.setText(String.valueOf(unscheduled));
        totalDeltaLabel.setText(total + " in library");
    }

    private void updateAcademicIndicator(ExamLibraryModels.TermRecord term) {
        if (term == null) {
            academicYearLabel.setText("All Academic Sessions");
            return;
        }
        String year = term.academicYear() == null ? "" : term.academicYear().trim();
        if (year.isBlank()) {
            academicYearLabel.setText(term.display());
        } else {
            academicYearLabel.setText(term.display() + " | " + year);
        }
    }

    private ExamLibraryModels.CourseRecord findCourseByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = canonicalCourse(code);
        return courses.stream()
                .filter(course -> canonicalCourse(course.courseCode()).equalsIgnoreCase(normalized)
                        || canonicalCourse(course.courseId()).equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    private ExamLibraryModels.TermRecord findTermById(String termId) {
        if (termId == null) {
            return null;
        }
        return terms.stream()
                .filter(term -> term.termId() != null && term.termId().equalsIgnoreCase(termId))
                .findFirst()
                .orElse(null);
    }

    private String statusStyleClass(String status) {
        if ("UPCOMING".equalsIgnoreCase(status)) {
            return "el-status-upcoming";
        }
        if ("COMPLETE".equalsIgnoreCase(status)) {
            return "el-status-complete";
        }
        return "el-status-notscheduled";
    }

    private String normalizeStatus(String status) {
        String value = normalized(status);
        if (value.contains("upcoming") || value.contains("scheduled")) {
            return "UPCOMING";
        }
        if (value.contains("complete") || value.contains("completed")) {
            return "COMPLETE";
        }
        if (value.contains("not") && value.contains("schedule")) {
            return "NOT SCHEDULED";
        }
        if (value.isBlank()) {
            return "NOT SCHEDULED";
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String resolveStatusFromDate(LocalDate date) {
        if (date == null) {
            return "NOT SCHEDULED";
        }
        if (date.isAfter(LocalDate.now())) {
            return "UPCOMING";
        }
        return "COMPLETE";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );
        for (DateTimeFormatter formatter : formats) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String canonicalCourse(String value) {
        return value == null ? "" : value.trim().replace(" ", "");
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Path resolveFirstExisting(String... candidates) {
        for (String candidate : candidates) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private List<List<String>> readCsv(Path path) {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                rows.add(parseCsvLine(line));
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return rows;
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

    private static final class CsvHeader {
        private final List<String> keys;

        private CsvHeader(List<String> keys) {
            this.keys = keys;
        }

        static CsvHeader from(List<String> values) {
            List<String> normalized = values.stream()
                    .map(CsvHeader::normalizeKey)
                    .collect(Collectors.toList());
            return new CsvHeader(normalized);
        }

        String get(List<String> values, String... aliases) {
            for (String alias : aliases) {
                int index = keys.indexOf(normalizeKey(alias));
                if (index >= 0 && index < values.size()) {
                    return Objects.toString(values.get(index), "").trim();
                }
            }
            return "";
        }

        boolean containsAny(String... aliases) {
            for (String alias : aliases) {
                if (keys.contains(normalizeKey(alias))) {
                    return true;
                }
            }
            return false;
        }

        private static String normalizeKey(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        }
    }

    private static final class ExamDisplayRow {
        private final String examId;
        private final String examTitle;
        private final String courseId;
        private final String courseName;
        private final String termId;
        private final String termName;
        private final int questionCount;
        private final int durationMinutes;
        private final String status;
        private final LocalDate examDate;

        private ExamDisplayRow(String examId, String examTitle, String courseId, String courseName,
                               String termId, String termName, int questionCount, int durationMinutes,
                               String status, LocalDate examDate) {
            this.examId = examId;
            this.examTitle = examTitle;
            this.courseId = courseId;
            this.courseName = courseName;
            this.termId = termId;
            this.termName = termName;
            this.questionCount = questionCount;
            this.durationMinutes = durationMinutes;
            this.status = status;
            this.examDate = examDate;
        }
    }
}
