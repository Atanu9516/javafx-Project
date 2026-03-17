package com.example.onlinemcqexam;

import java.io.IOException;
import java.util.List;

public interface MessagingService {
    FriendStore.FriendRequestStatus sendFriendRequest(String from, String to) throws IOException;

    List<String> fetchFriendRequests(String user) throws IOException;

    boolean acceptFriendRequest(String user, String from) throws IOException;

    boolean declineFriendRequest(String user, String from) throws IOException;

    List<String> fetchFriends(String user) throws IOException;

    List<String> fetchChat(String user, String friend) throws IOException;

    void sendChat(String from, String to, String text) throws IOException;
}
