package com.example.onlinemcqexam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestionBank {
    private static final Map<String, Set<String>> ALLOWED_COURSES_BY_TERM = Map.of(
            "1-1", Set.of("CSE101", "CSE103"),
            "1-2", Set.of("CSE105", "CSE107"),
            "2-1", Set.of("CSE205", "CSE207", "CSE215"),
            "2-2", Set.of("CSE200", "CSE209", "CSE211", "CSE213", "CSE219"),
            "3-1", Set.of("CSE301", "CSE309", "CSE313", "CSE315", "CSE317"),
            "3-2", Set.of("CSE311", "CSE321", "CSE325", "CSE329")
    );

    public static final class CourseInfo {
        private final String courseCode;
        private final String courseName;

        public CourseInfo(String courseCode, String courseName) {
            this.courseCode = courseCode;
            this.courseName = courseName;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public String getCourseName() {
            return courseName;
        }

        public String getDisplayLabel() {
            return courseCode + " - " + courseName;
        }
    }

    private QuestionBank() {
    }

    public static List<Question> loadQuestions(Level level) throws IOException {
        List<Question> allQuestions = loadFromCsv("questions.csv");
        List<Question> filtered = new ArrayList<>();
        for (Question question : allQuestions) {
            if (question.getLevel() == level) {
                filtered.add(question);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public static List<String> loadTerms() throws IOException {
        List<Question> allQuestions = loadFromCsv("questions.csv");
        List<String> terms = new ArrayList<>();
        for (Question question : allQuestions) {
            String term = question.getTerm();
            if (!isAllowedCourse(term, question.getCourseCode())) {
                continue;
            }
            if (term == null || term.isBlank() || terms.contains(term)) {
                continue;
            }
            terms.add(term);
        }
        terms.sort(Comparator.comparingInt(QuestionBank::termSortKey));
        return Collections.unmodifiableList(terms);
    }

    public static List<CourseInfo> loadCoursesForTerm(String term) throws IOException {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        List<Question> allQuestions = loadFromCsv("questions.csv");
        Map<String, String> byCode = new LinkedHashMap<>();
        for (Question question : allQuestions) {
            if (!term.equals(question.getTerm())) {
                continue;
            }
            String code = question.getCourseCode();
            if (!isAllowedCourse(term, code)) {
                continue;
            }
            String name = question.getCourseName();
            if (code == null || code.isBlank() || byCode.containsKey(code)) {
                continue;
            }
            byCode.put(code, name == null ? "" : name);
        }
        List<CourseInfo> courses = new ArrayList<>();
        for (Map.Entry<String, String> entry : byCode.entrySet()) {
            courses.add(new CourseInfo(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableList(courses);
    }

    public static List<Question> loadQuestions(String term, String courseCode) throws IOException {
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            return List.of();
        }
        if (!isAllowedCourse(term, courseCode)) {
            return List.of();
        }
        List<Question> allQuestions = loadFromCsv("questions.csv");
        List<Question> filtered = new ArrayList<>();
        for (Question question : allQuestions) {
            if (term.equals(question.getTerm())
                    && normalizeCourseCode(courseCode).equals(normalizeCourseCode(question.getCourseCode()))
                    && isAllowedCourse(question.getTerm(), question.getCourseCode())) {
                filtered.add(question);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public static List<String> loadExamIdsForCourse(String term, String courseCode) throws IOException {
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank()) {
            return List.of();
        }
        List<Question> allQuestions = loadFromCsv("questions.csv");
        List<String> examIds = new ArrayList<>();
        for (Question question : allQuestions) {
            if (!term.equals(question.getTerm()) || !courseCode.equals(question.getCourseCode())) {
                continue;
            }
            String examId = question.getExamId();
            if (examId == null || examId.isBlank() || examIds.contains(examId)) {
                continue;
            }
            examIds.add(examId);
        }
        return Collections.unmodifiableList(examIds);
    }

    public static List<Question> loadQuestions(String term, String courseCode, String examId) throws IOException {
        if (term == null || term.isBlank() || courseCode == null || courseCode.isBlank() || examId == null || examId.isBlank()) {
            return List.of();
        }
        List<Question> allQuestions = loadFromCsv("questions.csv");
        List<Question> filtered = new ArrayList<>();
        for (Question question : allQuestions) {
            if (term.equals(question.getTerm())
                    && courseCode.equals(question.getCourseCode())
                    && examId.equals(question.getExamId())) {
                filtered.add(question);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    private static List<Question> loadFromCsv(String resourceName) throws IOException {
        List<String> candidates = csvNameCandidates(resourceName);
        IOException lastReadError = null;

        for (String candidate : candidates) {
            try (InputStream in = openBundledCsvStream(candidate)) {
                if (in == null) {
                    continue;
                }
                return readQuestions(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            } catch (IOException ex) {
                lastReadError = ex;
                System.err.println("Failed to load bundled question CSV '" + candidate + "': " + ex.getMessage());
            }
        }

        for (String candidate : candidates) {
            Path writablePath = AppPaths.packageResourceFile(candidate);
            if (!Files.exists(writablePath) || isEffectivelyEmpty(writablePath)) {
                continue;
            }
            try (BufferedReader reader = Files.newBufferedReader(writablePath, StandardCharsets.UTF_8)) {
                return readQuestions(reader);
            } catch (IOException ex) {
                lastReadError = ex;
                System.err.println("Failed to load writable question CSV '" + writablePath + "': " + ex.getMessage());
            }
        }

        StringBuilder message = new StringBuilder("Question CSV not found. Checked classpath resources: ");
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) {
                message.append(", ");
            }
            message.append("/com/example/onlinemcqexam/").append(candidates.get(i));
        }
        message.append(" and writable files under ").append(AppPaths.appRoot());
        IOException failure = new IOException(message.toString());
        if (lastReadError != null) {
            failure.initCause(lastReadError);
        }
        throw failure;
    }

    private static List<Question> readQuestions(BufferedReader reader) throws IOException {
        List<Question> questions = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            try {
                    // New strict format: term,courseCode,courseName,examId,questionId,question,optionA,optionB,optionC,optionD,correctIndex
                    if (values.size() >= 11) {
                        String term = values.get(0).trim();
                        String courseCode = values.get(1).trim();
                        String courseName = values.get(2).trim();
                        String examId = values.get(3).trim();
                        if (examId.isBlank()) {
                            continue;
                        }
                        String id = values.get(4).trim();
                        String text = values.get(5).trim();
                        List<String> options = List.of(
                                values.get(6).trim(),
                                values.get(7).trim(),
                                values.get(8).trim(),
                                values.get(9).trim()
                        );
                        int correctIndex = Integer.parseInt(values.get(10).trim()) - 1;
                        if (correctIndex < 0 || correctIndex > 3) {
                            continue;
                        }
                        questions.add(new Question(term, courseCode, courseName, examId, id, mapLevelFromTerm(term), text, options, correctIndex));
                        continue;
                    }

                    // Transitional format without examId.
                    if (values.size() >= 10) {
                        String term = values.get(0).trim();
                        String courseCode = values.get(1).trim();
                        String courseName = values.get(2).trim();
                        String id = values.get(3).trim();
                        String text = values.get(4).trim();
                        List<String> options = List.of(
                                values.get(5).trim(),
                                values.get(6).trim(),
                                values.get(7).trim(),
                                values.get(8).trim()
                        );
                        int correctIndex = Integer.parseInt(values.get(9).trim()) - 1;
                        if (correctIndex < 0 || correctIndex > 3) {
                            continue;
                        }
                        questions.add(new Question(term, courseCode, courseName, "DEFAULT", id, mapLevelFromTerm(term), text, options, correctIndex));
                        continue;
                    }

                    if (values.size() < 8) {
                        continue;
                    }
                    Level level = Level.fromCsv(values.get(0));
                    if (level == null) {
                        continue;
                    }
                    String id = values.get(1).trim();
                    String text = values.get(2).trim();
                    List<String> options = List.of(
                            values.get(3).trim(),
                            values.get(4).trim(),
                            values.get(5).trim(),
                            values.get(6).trim()
                    );
                    int correctIndex = Integer.parseInt(values.get(7).trim()) - 1;
                    if (correctIndex < 0 || correctIndex > 3) {
                        continue;
                    }
                    questions.add(new Question(id, level, text, options, correctIndex));
            } catch (RuntimeException ex) {
                // Skip malformed rows and continue loading valid questions.
                continue;
            }
        }
        return Collections.unmodifiableList(questions);
    }

    private static InputStream openBundledCsvStream(String resourceName) {
        InputStream in = AppPaths.bundledPackageResourceStream(resourceName);
        if (in != null) {
            return in;
        }
        return AppPaths.bundledResourceStream(resourceName);
    }

    private static List<String> csvNameCandidates(String resourceName) {
        if ("questions.csv".equalsIgnoreCase(resourceName)) {
            return List.of("questions.csv", "question.csv");
        }
        if ("question.csv".equalsIgnoreCase(resourceName)) {
            return List.of("question.csv", "questions.csv");
        }
        return List.of(resourceName);
    }

    private static boolean isEffectivelyEmpty(Path path) {
        try {
            return Files.size(path) == 0L;
        } catch (IOException ex) {
            return true;
        }
    }

    private static Level mapLevelFromTerm(String term) {
        if (term == null) {
            return Level.BEGINNER;
        }
        return switch (term.trim()) {
            case "2-1", "2-2" -> Level.INTERMEDIATE;
            case "3-1", "3-2", "4-1", "4-2" -> Level.ADVANCED;
            default -> Level.BEGINNER;
        };
    }

    private static int termSortKey(String term) {
        if (term == null || term.isBlank()) {
            return Integer.MAX_VALUE;
        }
        String[] parts = term.split("-");
        if (parts.length != 2) {
            return Integer.MAX_VALUE;
        }
        try {
            int level = Integer.parseInt(parts[0].trim());
            int section = Integer.parseInt(parts[1].trim());
            return level * 10 + section;
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private static boolean isAllowedCourse(String term, String courseCode) {
        if (term == null || courseCode == null) {
            return false;
        }
        Set<String> allowed = ALLOWED_COURSES_BY_TERM.get(term.trim());
        if (allowed == null) {
            return !normalizeCourseCode(courseCode).isBlank();
        }
        return allowed.contains(normalizeCourseCode(courseCode));
    }

    private static String normalizeCourseCode(String courseCode) {
        return courseCode == null ? "" : courseCode.replace(" ", "").trim().toUpperCase();
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
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
}
