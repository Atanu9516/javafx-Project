package com.example.onlinemcqexam;

public record SemesterChangeRequest(
        String username,
        String currentSemester,
        String requestedSemester,
        String status,
        String requestedAt,
        String reviewedAt
) {
    public SemesterChangeRequest {
        username = safe(username);
        currentSemester = safe(currentSemester);
        requestedSemester = safe(requestedSemester);
        status = safe(status);
        requestedAt = safe(requestedAt);
        reviewedAt = safe(reviewedAt);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isDeclined() {
        return "DECLINED".equalsIgnoreCase(status);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
