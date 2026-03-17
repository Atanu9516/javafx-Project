package com.example.onlinemcqexam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ExamClient {
    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public ExamClient() {
        this(NetworkConfig.resolveHost(),
                NetworkConfig.resolvePort(),
                NetworkConfig.resolveConnectTimeoutMs(),
                NetworkConfig.resolveReadTimeoutMs());
    }

    public ExamClient(String host, int port, int connectTimeoutMs, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public ServerResponse register(String username, String password) throws IOException {
        return sendCommand("REGISTER", username, password);
    }

    public ServerResponse login(String username, String password) throws IOException {
        return sendCommand("LOGIN", username, password);
    }

    public ServerResponse ping() throws IOException {
        return sendCommand("PING");
    }

    public ServerResponse postDiscussion(String author, String text) throws IOException {
        return sendCommand("DISCUSS_POST", author, text);
    }

    public List<String> fetchDiscussion() throws IOException {
        String responseLine = sendCommandRaw("DISCUSS_LIST");
        return parseListResponse(responseLine);
    }

    public ServerResponse commentDiscussion(int index) throws IOException {
        return sendCommand("DISCUSS_COMMENT", String.valueOf(index));
    }

    public ServerResponse sendFriendRequest(String from, String to) throws IOException {
        return sendCommand("FRIEND_REQUEST", from, to);
    }

    public List<String> fetchFriendRequests(String user) throws IOException {
        String responseLine = sendCommandRaw("FRIEND_PENDING", user);
        return parseListResponse(responseLine);
    }

    public ServerResponse acceptFriendRequest(String user, String from) throws IOException {
        return sendCommand("FRIEND_ACCEPT", user, from);
    }

    public ServerResponse declineFriendRequest(String user, String from) throws IOException {
        return sendCommand("FRIEND_DECLINE", user, from);
    }

    public List<String> fetchFriends(String user) throws IOException {
        String responseLine = sendCommandRaw("FRIEND_LIST", user);
        return parseListResponse(responseLine);
    }

    public ServerResponse sendChat(String from, String to, String text) throws IOException {
        return sendCommand("CHAT_SEND", from, to, text);
    }

    public List<String> fetchChat(String user, String friend) throws IOException {
        String responseLine = sendCommandRaw("CHAT_HISTORY", user, friend);
        return parseListResponse(responseLine);
    }

    private ServerResponse sendCommand(String command, String... args) throws IOException {
        String responseLine = sendCommandRaw(command, args);
        return parseResponse(responseLine);
    }

    private String sendCommandRaw(String command, String... args) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String payload = buildPayload(command, args);
                writer.write(payload);
                writer.newLine();
                writer.flush();
                String responseLine = reader.readLine();
                if (responseLine == null) {
                    throw new IOException("Server closed the connection.");
                }
                return responseLine;
            }
        }
    }

    private String buildPayload(String command, String... args) {
        StringBuilder builder = new StringBuilder(command);
        if (args != null) {
            for (String arg : args) {
                builder.append(NetworkProtocol.DELIMITER).append(NetworkProtocol.encode(arg));
            }
        }
        return builder.toString();
    }

    private ServerResponse parseResponse(String line) {
        String[] parts = NetworkProtocol.split(line);
        if (parts.length == 0 || parts[0].isBlank()) {
            return new ServerResponse(false, "EMPTY_RESPONSE");
        }
        String status = parts[0].trim().toUpperCase();
        if ("OK".equals(status)) {
            String message = parts.length > 1 ? NetworkProtocol.decode(parts[1]) : "";
            return new ServerResponse(true, message);
        }
        if ("ERROR".equals(status)) {
            String message = parts.length > 1 ? NetworkProtocol.decode(parts[1]) : "ERROR";
            return new ServerResponse(false, message);
        }
        return new ServerResponse(false, "UNKNOWN_RESPONSE");
    }

    private List<String> parseListResponse(String line) throws IOException {
        String[] parts = NetworkProtocol.split(line);
        if (parts.length == 0 || parts[0].isBlank()) {
            throw new IOException("EMPTY_RESPONSE");
        }
        String status = parts[0].trim().toUpperCase();
        if ("OK".equals(status)) {
            List<String> messages = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                messages.add(NetworkProtocol.decode(parts[i]));
            }
            return messages;
        }
        if ("ERROR".equals(status)) {
            String message = parts.length > 1 ? NetworkProtocol.decode(parts[1]) : "ERROR";
            throw new IOException(message);
        }
        throw new IOException("UNKNOWN_RESPONSE");
    }

    public record ServerResponse(boolean ok, String message) {
    }
}
