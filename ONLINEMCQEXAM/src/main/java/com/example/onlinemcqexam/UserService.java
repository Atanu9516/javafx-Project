package com.example.onlinemcqexam;

import java.io.IOException;
import java.util.List;

public interface UserService {
    boolean register(String username, String password, String semester) throws IOException;

    UserProfile authenticate(String username, String password) throws IOException;

    SemesterChangeStore.RequestOutcome requestSemesterChange(String username, String requestedSemester) throws IOException;

    List<SemesterChangeRequest> fetchPendingSemesterChanges() throws IOException;

    boolean reviewSemesterChange(String username, String requestedSemester, boolean approve) throws IOException;

    SemesterChangeRequest fetchSemesterChangeStatus(String username) throws IOException;

    String normalizeUsername(String username);
}
