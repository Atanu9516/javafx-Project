package com.example.onlinemcqexam;

import java.io.IOException;

public interface UserService {
    boolean register(String username, String password) throws IOException;

    boolean authenticate(String username, String password) throws IOException;

    String normalizeUsername(String username);
}
