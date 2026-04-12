package com.example.onlinemcqexam;

import java.io.IOException;

public class Launcher {
    private static ExamServer embeddedServer;

    public static void main(String[] args) {
        ensureLocalServerRunning();
        ExamApplication.main(args);
    }

    private static void ensureLocalServerRunning() {
        try {
            ExamClient.ServerResponse response = new ExamClient().ping();
            if (response.ok()) {
                return;
            }
        } catch (IOException ignored) {
            // No server reachable. Start embedded server below.
        }

        String bindAddress = NetworkConfig.resolveBindAddress();
        int port = NetworkConfig.resolvePort();
        int readTimeoutMs = NetworkConfig.resolveReadTimeoutMs();
        embeddedServer = new ExamServer(new UserStore(), readTimeoutMs);

        Thread serverThread = new Thread(() -> {
            try {
                embeddedServer.start(bindAddress, port);
            } catch (IOException ex) {
                System.err.println("Embedded ExamServer failed to start on " + bindAddress + ":" + port + " (" + ex.getMessage() + ")");
            }
        }, "embedded-exam-server");
        serverThread.setDaemon(true);
        serverThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (embeddedServer != null) {
                embeddedServer.close();
            }
        }));
    }
}
