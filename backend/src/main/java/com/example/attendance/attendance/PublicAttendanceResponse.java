package com.example.attendance.attendance;

public record PublicAttendanceResponse(
    boolean success,
    String studentName,
    String stage,
    String message
) {}
