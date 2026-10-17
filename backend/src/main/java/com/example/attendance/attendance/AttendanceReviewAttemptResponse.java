package com.example.attendance.attendance;

import java.time.Instant;

public record AttendanceReviewAttemptResponse(
    Long id,
    Long sessionId,
    String sessionName,
    Long studentId,
    String studentCode,
    String studentName,
    String stage,
    Instant submittedAt,
    Double latitude,
    Double longitude,
    Double gpsAccuracyM,
    Double distanceFromSchool,
    String gpsResult,
    String gpsReason,
    String reviewStatus,
    Long reviewedBy,
    String reviewerUsername,
    Instant reviewedAt,
    String adminDecision,
    String adminNote
) {}
