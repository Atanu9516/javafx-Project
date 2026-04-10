package com.example.onlinemcqexam;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Launcher {
    private static final int SERVER_READY_RETRIES = 20;
    private static final long SERVER_READY_DELAY_MS = 150;
    private static final int SERVER_READY_CONNECT_TIMEOUT_MS = 250;

    private static volatile ExamServer embeddedServer;
    private static volatile Throwable serverStartupError;

    public static void main(String[] args) {
        initializeAppStorage();
        startEmbeddedServer();
        waitForServerReadiness();
        ExamApplication.main(args);
    }

    private static void initializeAppStorage() {
        try {
            AppPaths.initialize();
        } catch (RuntimeException ex) {
            System.err.println("Application storage initialization failed: " + ex.getMessage());
            throw ex;
        }
    }

    private static void startEmbeddedServer() {
        String bindAddress = NetworkConfig.resolveBindAddress();
        int port = NetworkConfig.resolvePort();
        int readTimeoutMs = NetworkConfig.resolveReadTimeoutMs();

        embeddedServer = new ExamServer(new UserStore(), readTimeoutMs);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (embeddedServer != null) {
                embeddedServer.close();
            }
        }, "exam-server-shutdown"));

        Thread serverThread = new Thread(() -> {
            try {
                embeddedServer.start(bindAddress, port);
            } catch (IOException ex) {
                serverStartupError = ex;
                System.err.println("Embedded ExamServer failed to start on " + bindAddress + ":" + port + " (" + ex.getMessage() + ")");
            }
        }, "exam-server-main");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void waitForServerReadiness() {
        String host = NetworkConfig.resolveHost();
        int port = NetworkConfig.resolvePort();

        for (int attempt = 1; attempt <= SERVER_READY_RETRIES; attempt++) {
            if (canConnect(host, port, SERVER_READY_CONNECT_TIMEOUT_MS)) {
                return;
            }

            Throwable startupError = serverStartupError;
            if (startupError != null) {
                // If another process already serves this port, UI can still proceed.
                if (canConnect(host, port, SERVER_READY_CONNECT_TIMEOUT_MS)) {
                    return;
                }
                System.err.println("Server startup error detected before UI launch: " + startupError.getMessage());
                return;
            }

            try {
                Thread.sleep(SERVER_READY_DELAY_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        System.err.println("Embedded ExamServer not ready after " + SERVER_READY_RETRIES
                + " attempts; launching UI anyway.");
    }

    private static boolean canConnect(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
