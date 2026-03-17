package com.example.onlinemcqexam;

public final class NetworkConfig {
    public static final String HOST_PROPERTY = "exam.host";
    public static final String PORT_PROPERTY = "exam.port";
    public static final String CONNECT_TIMEOUT_PROPERTY = "exam.connectTimeoutMs";
    public static final String READ_TIMEOUT_PROPERTY = "exam.readTimeoutMs";
    public static final String BIND_PROPERTY = "exam.bind";

    public static final String HOST_ENV = "EXAM_HOST";
    public static final String PORT_ENV = "EXAM_PORT";
    public static final String CONNECT_TIMEOUT_ENV = "EXAM_CONNECT_TIMEOUT_MS";
    public static final String READ_TIMEOUT_ENV = "EXAM_READ_TIMEOUT_MS";
    public static final String BIND_ENV = "EXAM_BIND";

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_BIND = "0.0.0.0";
    public static final int DEFAULT_PORT = 5050;
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    private NetworkConfig() {
    }

    public static String resolveHost() {
        return firstNonBlank(System.getProperty(HOST_PROPERTY), System.getenv(HOST_ENV), DEFAULT_HOST);
    }

    public static String resolveBindAddress() {
        return firstNonBlank(System.getProperty(BIND_PROPERTY), System.getenv(BIND_ENV), DEFAULT_BIND);
    }

    public static int resolvePort() {
        return parseInt(firstNonBlank(System.getProperty(PORT_PROPERTY), System.getenv(PORT_ENV)), DEFAULT_PORT);
    }

    public static int resolveConnectTimeoutMs() {
        return parseInt(firstNonBlank(System.getProperty(CONNECT_TIMEOUT_PROPERTY), System.getenv(CONNECT_TIMEOUT_ENV)),
                DEFAULT_CONNECT_TIMEOUT_MS);
    }

    public static int resolveReadTimeoutMs() {
        return parseInt(firstNonBlank(System.getProperty(READ_TIMEOUT_PROPERTY), System.getenv(READ_TIMEOUT_ENV)),
                DEFAULT_READ_TIMEOUT_MS);
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return fallback;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
