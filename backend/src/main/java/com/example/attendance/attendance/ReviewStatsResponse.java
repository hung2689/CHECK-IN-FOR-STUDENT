package com.example.attendance.attendance;

public record ReviewStatsResponse(
    long totalStudents,
    long confirmedCheckIns,
    long confirmedCheckOuts,
    long pendingReviews,
    long rejectedAttempts,
    long suspiciousAttempts
) {}
