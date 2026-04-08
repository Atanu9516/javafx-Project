package com.example.onlinemcqexam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ExamServer implements AutoCloseable {
    private static final String RECORD_SEPARATOR = "\u001F";
    private final ExecutorService clientPool;
    private final UserStore userStore;
    private final SemesterChangeStore semesterChangeStore;
    private final DiscussionStore discussionStore;
    private final FriendStore friendStore;
    private final int readTimeoutMs;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public ExamServer(UserStore userStore, int readTimeoutMs) {
        this.userStore = userStore;
        this.readTimeoutMs = readTimeoutMs;
        this.semesterChangeStore = new SemesterChangeStore(userStore);
        this.discussionStore = new DiscussionStore();
        this.friendStore = new FriendStore();
        this.clientPool = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "exam-client-handler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start(String bindAddress, int port) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(bindAddress, port));
        System.out.println("ExamServer listening on " + bindAddress + ":" + port);
        running = true;
        while (running) {
            Socket socket = serverSocket.accept();
            socket.setSoTimeout(readTimeoutMs);
            clientPool.submit(() -> handleClient(socket));
        }
    }

    private void handleClient(Socket socket) {
        try (Socket client = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String response = handleCommand(line);
                writer.write(response);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException ignored) {
            // Client disconnected or network error.
        }
    }

    private String handleCommand(String line) {
        String[] parts = NetworkProtocol.split(line);
        if (parts.length == 0) {
            return error("BAD_REQUEST");
        }
        String command = parts[0].trim().toUpperCase(Locale.ROOT);
        try {
            return switch (command) {
                case "PING" -> ok("");
                case "REGISTER" -> handleRegister(parts);
                case "LOGIN" -> handleLogin(parts);
                case "DISCUSS_LIST" -> handleDiscussionList();
                case "DISCUSS_POST" -> handleDiscussionPost(parts);
                case "FRIEND_REQUEST" -> handleFriendRequest(parts);
                case "FRIEND_PENDING" -> handleFriendPending(parts);
                case "FRIEND_ACCEPT" -> handleFriendAccept(parts);
                case "FRIEND_DECLINE" -> handleFriendDecline(parts);
                case "FRIEND_LIST" -> handleFriendList(parts);
                case "CHAT_SEND" -> handleChatSend(parts);
                case "CHAT_HISTORY" -> handleChatHistory(parts);
                case "SEMESTER_CHANGE_REQUEST" -> handleSemesterChangeRequest(parts);
                case "SEMESTER_CHANGE_PENDING" -> handleSemesterChangePending();
                case "SEMESTER_CHANGE_REVIEW" -> handleSemesterChangeReview(parts);
                case "SEMESTER_CHANGE_STATUS" -> handleSemesterChangeStatus(parts);
                default -> error("UNKNOWN_COMMAND");
            };
        } catch (IOException ex) {
            return error("IO");
        }
    }

    private String handleRegister(String[] parts) throws IOException {
        if (parts.length < 4) {
            return error("BAD_REQUEST");
        }
        String username = NetworkProtocol.decode(parts[1]);
        String password = NetworkProtocol.decode(parts[2]);
        String semester = NetworkProtocol.decode(parts[3]);
        if (userStore.normalizeSemester(semester).isBlank()) {
            return error("INVALID");
        }
        boolean created = userStore.register(username, password, semester);
        return created ? ok("") : error("EXISTS");
    }

    private String handleLogin(String[] parts) throws IOException {
        if (parts.length < 3) {
            return error("BAD_REQUEST");
        }
        String username = NetworkProtocol.decode(parts[1]);
        String password = NetworkProtocol.decode(parts[2]);
        UserStore.StoredUser user = userStore.authenticate(username, password);
        if (user == null) {
            return error("INVALID");
        }
        if (user.semester() == null || user.semester().isBlank()) {
            return error("NO_SEMESTER");
        }
        return ok(user.semester());
    }

    private String handleDiscussionList() {
        return okList(discussionStore.getDisplayMessages());
    }

    private String handleDiscussionPost(String[] parts) {
        if (parts.length < 3) {
            return error("BAD_REQUEST");
        }
        String author = NetworkProtocol.decode(parts[1]);
        String text = NetworkProtocol.decode(parts[2]);
        if (author.isBlank()) {
            author = "Student";
        }
        if (text.isBlank()) {
            return error("BAD_REQUEST");
        }
        discussionStore.addMessage(author.trim(), text.trim());
        return ok("");
    }

    private String handleFriendRequest(String[] parts) {
        if (parts.length < 3) {
            return error("BAD_REQUEST");
        }
        String from = NetworkProtocol.decode(parts[1]);
        String to = NetworkProtocol.decode(parts[2]);
        try {
            String fromKey = userStore.normalizeUsername(from);
            String toKey = userStore.normalizeUsername(to);
            java.util.Map<String, UserStore.StoredUser> users = userStore.loadUsers();
            if (!users.containsKey(fromKey) || !users.containsKey(toKey)) {
                return error("INVALID");
            }
        } catch (IOException ex) {
            return error("IO");
        }
        FriendStore.FriendRequestStatus status = friendStore.sendRequest(from, to);
        return switch (status) {
            case SENT -> ok("");
            case ACCEPTED -> ok("ACCEPTED");
            case ALREADY -> error("ALREADY");
            case INVALID -> error("INVALID");
        };
    }

    private String handleFriendPending(String[] parts) {
        if (parts.length < 2) {
            return error("BAD_REQUEST");
        }
        String user = NetworkProtocol.decode(parts[1]);
        return okList(friendStore.listPending(user));
    }

    private String handleFriendAccept(String[] parts) {
        if (parts.length < 3) {
            return error("BAD_REQUEST");
        }
        String user = NetworkProtocol.decode(parts[1]);
        String from = NetworkProtocol.decode(parts[2]);
        boolean ok = friendStore.acceptRequest(user, from);
        return ok ? ok("") : error("NOT_FOUND");
    }

    private String handleFriendDecline(String[] parts) {
        if (parts.length < 3) {
            return error("BAD_REQUEST");
        }
        String user = NetworkProtocol.decode(parts[1]);
        String from = NetworkProtocol.decode(parts[2]);
        boolean ok = friendStore.declineRequest(user, from);
        return ok ? ok("") : error("NOT_FOUND");
    }

    private String handleFriendList(String[] parts) {
        if (parts.length < 2) {
            return error("BAD_REQUEST");
        }
        String user = NetworkProtocol.decode(parts[1]);
        return okList(friendStore.listFriends(user));
    }

    private String handleChatSend(String[] parts) {
        if (parts.length < 4) {
            return error("BAD_REQUEST");
        }
        String from = NetworkProtocol.decode(parts[1]);
        String to = NetworkProtocol.decode(parts[2]);
        String text = NetworkProtocol.decode(parts[3]);
        boolean ok = friendStore.sendChat(from, to, text);
        return ok ? ok("") : error("NOT_FRIENDS");
    }

    private String handleChatHistory(String[] parts) {
        if (parts.length < 3) {
            return error("BAD_REQUEST");
        }
        String user = NetworkProtocol.decode(parts[1]);
        String friend = NetworkProtocol.decode(parts[2]);
        return okList(friendStore.getChatHistory(user, friend));
    }

    private String handleSemesterChangeRequest(String[] parts) throws IOException {
        if (parts.length < 3) {
            return error("BAD_REQUEST");
        }
        SemesterChangeStore.RequestOutcome outcome = semesterChangeStore.requestChange(
                NetworkProtocol.decode(parts[1]),
                NetworkProtocol.decode(parts[2])
        );
        return switch (outcome) {
            case CREATED -> ok("");
            case INVALID -> error("INVALID");
            case NOT_FOUND -> error("NOT_FOUND");
            case NO_CHANGE -> error("NO_CHANGE");
            case ALREADY_PENDING -> error("ALREADY_PENDING");
        };
    }

    private String handleSemesterChangePending() throws IOException {
        List<String> records = semesterChangeStore.listPendingRequests().stream()
                .map(this::packSemesterChangeRequest)
                .toList();
        return okList(records);
    }

    private String handleSemesterChangeReview(String[] parts) throws IOException {
        if (parts.length < 4) {
            return error("BAD_REQUEST");
        }
        String username = NetworkProtocol.decode(parts[1]);
        String requestedSemester = NetworkProtocol.decode(parts[2]);
        String decision = NetworkProtocol.decode(parts[3]);
        SemesterChangeStore.DecisionOutcome outcome = semesterChangeStore.reviewRequest(
                username,
                requestedSemester,
                "APPROVE".equalsIgnoreCase(decision)
        );
        return switch (outcome) {
            case UPDATED -> ok("");
            case INVALID -> error("INVALID");
            case NOT_FOUND -> error("NOT_FOUND");
        };
    }

    private String handleSemesterChangeStatus(String[] parts) throws IOException {
        if (parts.length < 2) {
            return error("BAD_REQUEST");
        }
        SemesterChangeRequest request = semesterChangeStore.findLatestForUser(NetworkProtocol.decode(parts[1]));
        if (request == null) {
            return ok("");
        }
        return ok(packSemesterChangeRequest(request));
    }

    private String packSemesterChangeRequest(SemesterChangeRequest request) {
        return String.join(RECORD_SEPARATOR,
                request.username(),
                request.currentSemester(),
                request.requestedSemester(),
                request.status(),
                request.requestedAt(),
                request.reviewedAt());
    }

    private String ok(String message) {
        if (message == null || message.isBlank()) {
            return "OK";
        }
        return "OK" + NetworkProtocol.DELIMITER + NetworkProtocol.encode(message);
    }

    private String okList(java.util.List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "OK";
        }
        StringBuilder builder = new StringBuilder("OK");
        for (String message : messages) {
            builder.append(NetworkProtocol.DELIMITER).append(NetworkProtocol.encode(message));
        }
        return builder.toString();
    }

    private String error(String message) {
        return "ERROR" + NetworkProtocol.DELIMITER + NetworkProtocol.encode(message);
    }

    @Override
    public void close() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        clientPool.shutdown();
        try {
            clientPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws IOException {
        String bindAddress = NetworkConfig.resolveBindAddress();
        int port = NetworkConfig.resolvePort();
        int readTimeoutMs = NetworkConfig.resolveReadTimeoutMs();
        ExamServer server = new ExamServer(new UserStore(), readTimeoutMs);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        try {
            server.start(bindAddress, port);
        } catch (IOException ex) {
            System.err.println("Failed to start ExamServer on " + bindAddress + ":" + port + " (" + ex.getMessage() + ")");
            throw ex;
        }
    }
}
