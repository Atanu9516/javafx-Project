package com.example.onlinemcqexam;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class NetworkProtocol {
    public static final String DELIMITER = "|";

    private NetworkProtocol() {
    }

    public static String encode(String value) {
        if (value == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public static String[] split(String line) {
        return line.split("\\|", -1);
    }
}
