package com.example.onlinemcqexam;

import java.io.IOException;
import java.util.List;

public final class NetworkMessagingService implements MessagingService {
    private final ExamClient client;

    public NetworkMessagingService(ExamClient client) {
        this.client = client;
    }

    @Override
    public FriendStore.FriendRequestStatus sendFriendRequest(String from, String to) throws IOException {
        ExamClient.ServerResponse response = client.sendFriendRequest(from, to);
        if (response.ok()) {
            String message = response.message();
            if ("ACCEPTED".equalsIgnoreCase(message)) {
                return FriendStore.FriendRequestStatus.ACCEPTED;
            }
            return FriendStore.FriendRequestStatus.SENT;
        }
        String message = response.message();
        if ("ALREADY".equalsIgnoreCase(message)) {
            return FriendStore.FriendRequestStatus.ALREADY;
        }
        if ("INVALID".equalsIgnoreCase(message)) {
            return FriendStore.FriendRequestStatus.INVALID;
        }
        throw new IOException(message);
    }

    @Override
    public List<String> fetchFriendRequests(String user) throws IOException {
        return client.fetchFriendRequests(user);
    }

    @Override
    public boolean acceptFriendRequest(String user, String from) throws IOException {
        ExamClient.ServerResponse response = client.acceptFriendRequest(user, from);
        if (response.ok()) {
            return true;
        }
        if ("NOT_FOUND".equalsIgnoreCase(response.message())) {
            return false;
        }
        throw new IOException(response.message());
    }

    @Override
    public boolean declineFriendRequest(String user, String from) throws IOException {
        ExamClient.ServerResponse response = client.declineFriendRequest(user, from);
        if (response.ok()) {
            return true;
        }
        if ("NOT_FOUND".equalsIgnoreCase(response.message())) {
            return false;
        }
        throw new IOException(response.message());
    }

    @Override
    public List<String> fetchFriends(String user) throws IOException {
        return client.fetchFriends(user);
    }

    @Override
    public List<String> fetchChat(String user, String friend) throws IOException {
        return client.fetchChat(user, friend);
    }

    @Override
    public void sendChat(String from, String to, String text) throws IOException {
        ExamClient.ServerResponse response = client.sendChat(from, to, text);
        if (!response.ok()) {
            throw new IOException(response.message());
        }
    }
}
