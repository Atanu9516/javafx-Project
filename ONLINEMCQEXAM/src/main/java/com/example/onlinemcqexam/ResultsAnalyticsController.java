package com.example.onlinemcqexam;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ResultsAnalyticsController {
    private static final double PASS_THRESHOLD = 40.0;

    @FXML
    private Label sessionIndicatorLabel;
    @FXML
    private Button backButton;
    @FXML
    private ChoiceBox<String> termFilterChoice;
    @FXML
    private ChoiceBox<String> courseFilterChoice;
    @FXML
    private Button applyFiltersButton;
    @FXML
    private Label avgScoreLabel;
    @FXML
    private Label highestScoreLabel;
    @FXML
    private Label lowestScoreLabel;
    @FXML
    private Label passRateLabel;
    @FXML
    private Label avgScoreDeltaLabel;
    @FXML
    private Label highestScoreDeltaLabel;
    @FXML
    private Label lowestScoreDeltaLabel;
    @FXML
    private Label passRateDeltaLabel;
    @FXML
    private BarChart<String, Number> scoreDistributionChart;
    @FXML
    private Label insightOneLabel;
    @FXML
    private Label insightTwoLabel;
    @FXML
    private Label insightThreeLabel;
    @FXML
    private TableView<StudentPerformanceRow> studentTable;
    @FXML
    private TableColumn<StudentPerformanceRow, String> studentNameColumn;
    @FXML
    private TableColumn<StudentPerformanceRow, String> studentIdColumn;
    @FXML
    private TableColumn<StudentPerformanceRow, String> examDateColumn;
    @FXML
    private TableColumn<StudentPerformanceRow, String> gradeBandColumn;
    @FXML
    private TableColumn<StudentPerformanceRow, Double> percentageColumn;
    @FXML
    private TableColumn<StudentPerformanceRow, String> performanceStatusColumn;
    @FXML
    private TableColumn<StudentPerformanceRow, Double> scoreColumn;

    private final List<ResultsAnalyticsModels.TermRecord> terms = new ArrayList<>();
    private final List<ResultsAnalyticsModels.CourseRecord> courses = new ArrayList<>();
    private final List<ResultsAnalyticsModels.ExamRecord> exams = new ArrayList<>();
    private final List<ResultsAnalyticsModels.StudentRecord> students = new ArrayList<>();
    private final List<ResultsAnalyticsModels.ResultRecord> results = new ArrayList<>();

    private final Map<String, String> yearByDisplay = new LinkedHashMap<>();
    private final Map<String, ResultsAnalyticsModels.CourseRecord> courseByDisplay = new LinkedHashMap<>();

    private final ObservableList<StudentPerformanceRow> filteredRows = FXCollections.observableArrayList();
    private Runnable onBackRequested;

    @FXML
    private void initialize() {
        configureTable();
        wireActions();
        refreshFromData();
    }

    public void refreshFromData() {
        loadAllData();
        populateFilters();
        applyFilters();
    }

    public void setOnBackRequested(Runnable onBackRequested) {
        this.onBackRequested = onBackRequested;
    }

    private void configureTable() {
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        examDateColumn.setCellValueFactory(new PropertyValueFactory<>("examDate"));
        gradeBandColumn.setCellValueFactory(new PropertyValueFactory<>("gradeBand"));
        percentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        performanceStatusColumn.setCellValueFactory(new PropertyValueFactory<>("performanceStatus"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));

        percentageColumn.setComparator(Comparator.naturalOrder());
        scoreColumn.setComparator(Comparator.naturalOrder());
        studentNameColumn.setComparator(String::compareToIgnoreCase);

        studentTable.setItems(filteredRows);
    }

    private void wireActions() {
        backButton.setOnAction(event -> {
            if (onBackRequested != null) {
                onBackRequested.run();
            }
        });
        applyFiltersButton.setOnAction(event -> applyFilters());
        termFilterChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            repopulateCourseFilter(newValue);
            applyFilters();
        });
        courseFilterChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadAllData() {
        terms.clear();
        courses.clear();
        exams.clear();
        students.clear();
        results.clear();

        loadTerms();
        loadCourses();
        loadExams();
        loadStudents();
        loadResults();

        deriveMissingTermsAndCoursesFromResults();
        ensureStudentFallbacks();
    }

    private void loadTerms() {
        Path path = AppPaths.packageResourceFile("terms.csv");
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
            String termId = header.get(values, "term_id", "termid", "term", "id");
            String termName = header.get(values, "term_name", "termname", "name");
            String year = header.get(values, "academic_year", "academicyear", "year");
            if (!termId.isBlank() || !termName.isBlank()) {
                terms.add(new ResultsAnalyticsModels.TermRecord(termId, termName, year));
            }
        }
    }

    private void loadCourses() {
        Path path = AppPaths.packageResourceFile("courses.csv");
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
                courses.add(new ResultsAnalyticsModels.CourseRecord(
                        courseId.isBlank() ? courseCode : courseId,
                        courseName,
                        courseCode.isBlank() ? courseId : courseCode,
                        termId
                ));
            }
        }
    }

    private void loadExams() {
        Path path = AppPaths.resourceFile("exams.csv");
        if (path == null) {
            return;
        }
        List<List<String>> rows = readCsv(path);
        if (rows.isEmpty()) {
            return;
        }

        List<String> headerRow = rows.get(0);
        CsvHeader header = CsvHeader.from(headerRow);
        boolean modern = header.containsAny("examid", "examtitle", "courseid", "templatename");

        int serial = 0;
        for (int i = 1; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.isEmpty()) {
                continue;
            }
            serial++;
            if (modern) {
                String examId = header.get(values, "exam_id", "examid", "id");
                String title = header.get(values, "exam_title", "examtitle", "title", "template_name", "templatename");
                String courseId = header.get(values, "course_id", "courseid", "course", "course_code", "coursecode");
                String termId = header.get(values, "term_id", "termid", "term");
                String date = header.get(values, "date", "exam_date", "examdate");
                exams.add(new ResultsAnalyticsModels.ExamRecord(
                        examId.isBlank() ? formattedExamId(serial) : examId,
                        title,
                        courseId,
                        termId,
                        date
                ));
            } else {
                String term = valueAt(values, 0);
                String courseCode = valueAt(values, 1);
                String courseName = valueAt(values, 2);
                String examId = formattedExamId(serial);
                exams.add(new ResultsAnalyticsModels.ExamRecord(examId, courseName, courseCode, term, ""));
            }
        }
    }

    private void loadStudents() {
        Path studentsPath = resolveFirstExisting(
                "src/main/resources/students.csv",
                "data/students.csv"
        );
        if (studentsPath != null) {
            List<List<String>> rows = readCsv(studentsPath);
            if (!rows.isEmpty()) {
                CsvHeader header = CsvHeader.from(rows.get(0));
                for (int i = 1; i < rows.size(); i++) {
                    List<String> values = rows.get(i);
                    String studentId = header.get(values, "student_id", "studentid", "id", "username");
                    String name = header.get(values, "name", "student_name", "studentname", "username");
                    String email = header.get(values, "email");
                    String batch = header.get(values, "batch", "section");
                    if (!studentId.isBlank() || !name.isBlank()) {
                        students.add(new ResultsAnalyticsModels.StudentRecord(
                                studentId.isBlank() ? name : studentId,
                                name.isBlank() ? studentId : name,
                                email,
                                batch
                        ));
                    }
                }
            }
        }

        Path usersPath = resolveFirstExisting("data/users.csv");
        if (usersPath != null) {
            List<List<String>> rows = readCsv(usersPath);
            for (List<String> values : rows) {
                if (values.isEmpty()) {
                    continue;
                }
                String raw = valueAt(values, 0);
                if (raw.startsWith("#") || raw.equalsIgnoreCase("username")) {
                    continue;
                }
                String id = raw.trim();
                if (!id.isBlank() && students.stream().noneMatch(s -> s.studentId().equalsIgnoreCase(id))) {
                    students.add(new ResultsAnalyticsModels.StudentRecord(id, id, "", ""));
                }
            }
        }
    }

    private void loadResults() {
        Path path = resolveFirstExisting("src/main/resources/results.csv");
        if (path == null) {
            return;
        }
        List<List<String>> rows = readCsv(path);
        if (rows.isEmpty()) {
            return;
        }

        CsvHeader header = CsvHeader.from(rows.get(0));
        boolean hasHeader = header.containsAny("studentid", "username", "examid", "percentage");

        for (int i = hasHeader ? 1 : 0; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.isEmpty()) {
                continue;
            }

            String studentId = header.get(values, "student_id", "studentid", "username", "name");
            String examId = header.get(values, "exam_id", "examid");
            String term = header.get(values, "term", "term_id", "termid");
            String course = header.get(values, "course", "course_id", "courseid", "course_code", "coursecode");
            String scoreRaw = header.get(values, "score", "correctanswers", "correct", "marks_obtained");
            String totalRaw = header.get(values, "total_marks", "totalmarks", "totalquestions", "total");
            String pctRaw = header.get(values, "percentage", "percent");
            String grade = header.get(values, "grade", "grade_band", "gradeband");
            String date = header.get(values, "exam_date", "examdate", "date");
            String type = header.get(values, "type", "session", "attempt_type");

            if (!hasHeader) {
                studentId = valueAt(values, 0);
                term = valueAt(values, 1);
                course = valueAt(values, 2);
                examId = valueAt(values, 3);
                totalRaw = valueAt(values, 4);
                scoreRaw = valueAt(values, 5);
                pctRaw = valueAt(values, 6);
                date = valueAt(values, 8);
                type = valueAt(values, 9);
            }

            double score = parseDouble(scoreRaw, 0.0);
            double total = parseDouble(totalRaw, 0.0);
            double pct = parseDouble(pctRaw, total > 0 ? (score / total) * 100.0 : 0.0);

            if (studentId.isBlank()) {
                continue;
            }

            if (grade.isBlank()) {
                grade = gradeFromPercentage(pct);
            }

            results.add(new ResultsAnalyticsModels.ResultRecord(
                    studentId,
                    examId,
                    term,
                    canonicalCourse(course),
                    score,
                    total,
                    pct,
                    grade,
                    date,
                    type
            ));
        }
    }

    private void deriveMissingTermsAndCoursesFromResults() {
        Set<String> knownTerms = terms.stream().map(ResultsAnalyticsModels.TermRecord::termId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> knownCourses = courses.stream().map(c -> canonicalCourse(c.courseCode())).collect(Collectors.toCollection(LinkedHashSet::new));

        for (ResultsAnalyticsModels.ResultRecord result : results) {
            if (!result.termId().isBlank() && knownTerms.add(result.termId())) {
                terms.add(new ResultsAnalyticsModels.TermRecord(result.termId(), "Term " + result.termId(), ""));
            }
            if (!result.courseId().isBlank() && knownCourses.add(result.courseId())) {
                courses.add(new ResultsAnalyticsModels.CourseRecord(result.courseId(), result.courseId(), result.courseId(), result.termId()));
            }
        }

        for (ResultsAnalyticsModels.ExamRecord exam : exams) {
            String term = exam.termId();
            String course = canonicalCourse(exam.courseId());
            if (!term.isBlank() && knownTerms.add(term)) {
                terms.add(new ResultsAnalyticsModels.TermRecord(term, "Term " + term, ""));
            }
            if (!course.isBlank() && knownCourses.add(course)) {
                courses.add(new ResultsAnalyticsModels.CourseRecord(course, exam.examTitle(), course, term));
            }
        }
    }

    private void ensureStudentFallbacks() {
        Set<String> knownIds = students.stream().map(s -> s.studentId().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        for (ResultsAnalyticsModels.ResultRecord result : results) {
            String key = result.studentId().toLowerCase(Locale.ROOT);
            if (!knownIds.contains(key)) {
                students.add(new ResultsAnalyticsModels.StudentRecord(result.studentId(), result.studentId(), "", ""));
                knownIds.add(key);
            }
        }
    }

    private void populateFilters() {
        yearByDisplay.clear();
        courseByDisplay.clear();

        termFilterChoice.getItems().clear();
        termFilterChoice.getItems().add("All Years");
        buildAvailableYears().forEach(year -> {
            yearByDisplay.put(year, year);
            termFilterChoice.getItems().add(year);
        });
        termFilterChoice.getSelectionModel().selectFirst();

        repopulateCourseFilter(termFilterChoice.getValue());
        updateSessionIndicator();
    }

    private void repopulateCourseFilter(String yearDisplay) {
        courseByDisplay.clear();
        String selectedYear = yearByDisplay.get(yearDisplay);

        courseFilterChoice.getItems().clear();
        courseFilterChoice.getItems().add("All Courses");

        courses.stream()
                .filter(course -> selectedYear == null || selectedYear.isBlank() || selectedYearMatchesTerm(selectedYear, course.termId()))
                .sorted(Comparator.comparing(ResultsAnalyticsModels.CourseRecord::display, String::compareToIgnoreCase))
                .forEach(course -> {
                    String display = course.display();
                    courseByDisplay.put(display, course);
                    courseFilterChoice.getItems().add(display);
                });

        courseFilterChoice.getSelectionModel().selectFirst();
        updateSessionIndicator();
    }

    private void applyFilters() {
        List<StudentPerformanceRow> allRows = buildJoinedRows();
        String selectedYear = yearByDisplay.get(termFilterChoice.getValue());
        ResultsAnalyticsModels.CourseRecord selectedCourse = courseByDisplay.get(courseFilterChoice.getValue());

        List<StudentPerformanceRow> rows = allRows.stream()
                .filter(row -> selectedYear == null || selectedYear.isBlank() || selectedYear.equalsIgnoreCase(row.getYearId()))
                .filter(row -> selectedCourse == null || selectedCourse.courseCode().isBlank() || selectedCourse.courseCode().replace(" ", "").equalsIgnoreCase(row.getCourseId()))
                .collect(Collectors.toList());

        filteredRows.setAll(rows);
        updateStats(allRows, rows);
        updateDistribution(rows);
        updateInsights(rows);
        updateSessionIndicator();
    }

    private List<StudentPerformanceRow> buildJoinedRows() {
        Map<String, ResultsAnalyticsModels.StudentRecord> studentById = new HashMap<>();
        for (ResultsAnalyticsModels.StudentRecord student : students) {
            studentById.put(student.studentId().toLowerCase(Locale.ROOT), student);
        }

        Map<String, ResultsAnalyticsModels.ExamRecord> examById = new HashMap<>();
        for (ResultsAnalyticsModels.ExamRecord exam : exams) {
            examById.put(exam.examId().toLowerCase(Locale.ROOT), exam);
        }

        List<StudentPerformanceRow> rows = new ArrayList<>();
        for (ResultsAnalyticsModels.ResultRecord result : results) {
            ResultsAnalyticsModels.StudentRecord student = studentById.get(result.studentId().toLowerCase(Locale.ROOT));
            ResultsAnalyticsModels.ExamRecord exam = examById.get(result.examId().toLowerCase(Locale.ROOT));

            String studentName = student == null || student.name().isBlank() ? result.studentId() : student.name();
            String studentId = result.studentId();
            String examDate = !result.examDate().isBlank() ? result.examDate() : (exam == null ? "" : exam.examDate());
            String gradeBand = result.grade().isBlank() ? gradeFromPercentage(result.percentage()) : result.grade();
            String status = result.percentage() >= PASS_THRESHOLD ? "PASS" : "REVIEW";
            double score = result.score();
            String termId = !result.termId().isBlank() ? result.termId() : (exam == null ? "" : exam.termId());
            String courseId = !result.courseId().isBlank() ? result.courseId() : (exam == null ? "" : canonicalCourse(exam.courseId()));
            String yearId = deriveYearId(termId, examDate);
            if (courseId.isBlank() && exam != null) {
                courseId = canonicalCourse(exam.courseId());
            }

            rows.add(new StudentPerformanceRow(
                    studentName,
                    studentId,
                    examDate,
                    gradeBand,
                    round1(result.percentage()),
                    status,
                    round1(score),
                    termId,
                    yearId,
                    canonicalCourse(courseId)
            ));
        }

        rows.sort(Comparator.comparing(StudentPerformanceRow::getPercentage).reversed());
        return rows;
    }

    private void updateStats(List<StudentPerformanceRow> allRows, List<StudentPerformanceRow> currentRows) {
        Metrics current = computeMetrics(currentRows);
        Metrics baseline = computeMetrics(allRows);

        avgScoreLabel.setText(formatPct(current.average()));
        highestScoreLabel.setText(formatPct(current.highest()));
        lowestScoreLabel.setText(formatPct(current.lowest()));
        passRateLabel.setText(formatPct(current.passRate()));

        avgScoreDeltaLabel.setText(deltaLabel(current.average(), baseline.average()));
        highestScoreDeltaLabel.setText(deltaLabel(current.highest(), baseline.highest()));
        lowestScoreDeltaLabel.setText(deltaLabel(current.lowest(), baseline.lowest()));
        passRateDeltaLabel.setText(deltaLabel(current.passRate(), baseline.passRate()));
    }

    private Metrics computeMetrics(List<StudentPerformanceRow> rows) {
        if (rows.isEmpty()) {
            return new Metrics(0.0, 0.0, 0.0, 0.0);
        }
        double avg = rows.stream().mapToDouble(StudentPerformanceRow::getPercentage).average().orElse(0.0);
        double max = rows.stream().mapToDouble(StudentPerformanceRow::getPercentage).max().orElse(0.0);
        double min = rows.stream().mapToDouble(StudentPerformanceRow::getPercentage).min().orElse(0.0);
        long pass = rows.stream().filter(row -> row.getPercentage() >= PASS_THRESHOLD).count();
        double passRate = rows.isEmpty() ? 0.0 : (pass * 100.0) / rows.size();
        return new Metrics(avg, max, min, passRate);
    }

    private void updateDistribution(List<StudentPerformanceRow> rows) {
        int[] bins = new int[5];
        for (StudentPerformanceRow row : rows) {
            double p = row.getPercentage();
            if (p < 20.0) {
                bins[0]++;
            } else if (p < 40.0) {
                bins[1]++;
            } else if (p < 60.0) {
                bins[2]++;
            } else if (p < 80.0) {
                bins[3]++;
            } else {
                bins[4]++;
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("0-20%", bins[0]));
        series.getData().add(new XYChart.Data<>("20-40%", bins[1]));
        series.getData().add(new XYChart.Data<>("40-60%", bins[2]));
        series.getData().add(new XYChart.Data<>("60-80%", bins[3]));
        series.getData().add(new XYChart.Data<>("80-100%", bins[4]));
        scoreDistributionChart.getData().setAll(series);
    }

    private void updateInsights(List<StudentPerformanceRow> rows) {
        if (rows.isEmpty()) {
            insightOneLabel.setText("No result records match the current filters.");
            insightTwoLabel.setText("Load exam attempts into results.csv to generate insights.");
            insightThreeLabel.setText("Insights update live whenever year or course changes.");
            return;
        }

        double avg = rows.stream().mapToDouble(StudentPerformanceRow::getPercentage).average().orElse(0.0);

        List<StudentPerformanceRow> topRows = rows.stream()
                .sorted(Comparator.comparing(StudentPerformanceRow::getPercentage).reversed())
                .limit(Math.max(1, rows.size() / 4))
                .collect(Collectors.toList());
        double topAvg = topRows.stream().mapToDouble(StudentPerformanceRow::getPercentage).average().orElse(avg);

        Map<String, Double> courseAvg = rows.stream()
                .collect(Collectors.groupingBy(StudentPerformanceRow::getCourseId,
                        Collectors.averagingDouble(StudentPerformanceRow::getPercentage)));
        Map.Entry<String, Double> hardest = courseAvg.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElse(Map.entry("N/A", avg));

        List<StudentPerformanceRow> morning = rows.stream()
                .filter(row -> parseHour(row.getExamDate()) >= 0 && parseHour(row.getExamDate()) < 12)
                .collect(Collectors.toList());
        List<StudentPerformanceRow> later = rows.stream()
                .filter(row -> parseHour(row.getExamDate()) >= 12)
                .collect(Collectors.toList());

        insightOneLabel.setText(String.format(Locale.US,
                "Top performers exceed average by %.1f%%.",
                Math.max(0.0, topAvg - avg)));

        insightTwoLabel.setText(String.format(Locale.US,
                "Course %s has the lowest average at %.1f%%.",
                hardest.getKey().isBlank() ? "N/A" : hardest.getKey(),
                hardest.getValue()));

        if (!morning.isEmpty() && !later.isEmpty()) {
            double morningAvg = morning.stream().mapToDouble(StudentPerformanceRow::getPercentage).average().orElse(avg);
            double laterAvg = later.stream().mapToDouble(StudentPerformanceRow::getPercentage).average().orElse(avg);
            insightThreeLabel.setText(String.format(Locale.US,
                    "Morning sessions score %.1f%% %s later sessions.",
                    Math.abs(morningAvg - laterAvg),
                    morningAvg >= laterAvg ? "higher than" : "lower than"));
        } else {
            long passCount = rows.stream().filter(row -> row.getPercentage() >= PASS_THRESHOLD).count();
            insightThreeLabel.setText(String.format(Locale.US,
                    "%d out of %d students are currently above the pass threshold.",
                    passCount,
                    rows.size()));
        }
    }

    private void updateSessionIndicator() {
        String year = yearByDisplay.get(termFilterChoice.getValue());
        String course = courseFilterChoice.getValue();
        if (year == null || year.isBlank()) {
            sessionIndicatorLabel.setText(course == null || course.isBlank() || "All Courses".equals(course)
                    ? "All Years | All Courses"
                    : "All Years | " + course);
            return;
        }
        if (course == null || course.isBlank() || "All Courses".equals(course)) {
            sessionIndicatorLabel.setText(year + " | All Courses");
            return;
        }
        sessionIndicatorLabel.setText(year + " | " + course);
    }

    private String formatPct(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String deltaLabel(double current, double baseline) {
        if (baseline == 0.0) {
            return "Baseline N/A";
        }
        double delta = current - baseline;
        String direction = delta >= 0 ? "+" : "";
        return String.format(Locale.US, "%s%.1f%% vs baseline", direction, delta);
    }

    private String formattedExamId(int serial) {
        return String.format(Locale.US, "EX%03d", Math.max(1, serial));
    }

    private String canonicalCourse(String value) {
        return value == null ? "" : value.trim().replace(" ", "");
    }

    private int parseHour(String examDate) {
        if (examDate == null || examDate.isBlank()) {
            return -1;
        }
        int t = examDate.indexOf('T');
        if (t < 0 || t + 3 > examDate.length()) {
            return -1;
        }
        try {
            return Integer.parseInt(examDate.substring(t + 1, t + 3));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
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

    private String gradeFromPercentage(double percentage) {
        if (percentage >= 80.0) {
            return "A";
        }
        if (percentage >= 70.0) {
            return "B";
        }
        if (percentage >= 60.0) {
            return "C";
        }
        if (percentage >= 50.0) {
            return "D";
        }
        return "F";
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> buildAvailableYears() {
        Set<String> years = new LinkedHashSet<>();
        for (ResultsAnalyticsModels.TermRecord term : terms) {
            String year = deriveYearId(term.termId(), term.academicYear());
            if (!year.isBlank()) {
                years.add(year);
            }
        }
        for (ResultsAnalyticsModels.CourseRecord course : courses) {
            String year = deriveYearId(course.termId(), "");
            if (!year.isBlank()) {
                years.add(year);
            }
        }
        for (ResultsAnalyticsModels.ResultRecord result : results) {
            String year = deriveYearId(result.termId(), result.examDate());
            if (!year.isBlank()) {
                years.add(year);
            }
        }
        return years.stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList());
    }

    private boolean selectedYearMatchesTerm(String selectedYear, String termId) {
        return selectedYear.equalsIgnoreCase(deriveYearId(termId, ""));
    }

    private String deriveYearId(String termId, String fallbackDateOrYear) {
        String cleanedTerm = termId == null ? "" : termId.trim();
        if (!cleanedTerm.isBlank()) {
            int dash = cleanedTerm.indexOf('-');
            String prefix = dash > 0 ? cleanedTerm.substring(0, dash) : cleanedTerm;
            if (prefix.chars().allMatch(Character::isDigit)) {
                return "Year " + prefix;
            }
            return cleanedTerm;
        }

        String fallback = fallbackDateOrYear == null ? "" : fallbackDateOrYear.trim();
        if (fallback.matches("\\d{4}.*")) {
            return fallback.substring(0, 4);
        }
        return "";
    }

    private record Metrics(double average, double highest, double lowest, double passRate) {
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
                String normalizedAlias = normalizeKey(alias);
                int index = keys.indexOf(normalizedAlias);
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

    public static final class StudentPerformanceRow {
        private final String studentName;
        private final String studentId;
        private final String examDate;
        private final String gradeBand;
        private final Double percentage;
        private final String performanceStatus;
        private final Double score;
        private final String termId;
        private final String yearId;
        private final String courseId;

        public StudentPerformanceRow(String studentName, String studentId, String examDate, String gradeBand,
                                     Double percentage, String performanceStatus, Double score,
                                     String termId, String yearId, String courseId) {
            this.studentName = studentName;
            this.studentId = studentId;
            this.examDate = examDate;
            this.gradeBand = gradeBand;
            this.percentage = percentage;
            this.performanceStatus = performanceStatus;
            this.score = score;
            this.termId = termId;
            this.yearId = yearId;
            this.courseId = courseId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getExamDate() {
            return examDate;
        }

        public String getGradeBand() {
            return gradeBand;
        }

        public Double getPercentage() {
            return percentage;
        }

        public String getPerformanceStatus() {
            return performanceStatus;
        }

        public Double getScore() {
            return score;
        }

        public String getTermId() {
            return termId;
        }

        public String getYearId() {
            return yearId;
        }

        public String getCourseId() {
            return courseId;
        }
    }
}
