package com.example.onlinemcqexam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestionBank {
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

    private static List<Question> loadFromCsv(String resourceName) throws IOException {
        InputStream stream = QuestionBank.class.getResourceAsStream(resourceName);
        if (stream == null) {
            throw new IOException("Missing resource: " + resourceName);
        }

        List<Question> questions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
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
            }
        }
        return Collections.unmodifiableList(questions);
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
