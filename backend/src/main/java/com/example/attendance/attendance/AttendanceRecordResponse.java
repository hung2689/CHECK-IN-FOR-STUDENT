package com.example.attendance.attendance;

import java.time.Instant;

public record AttendanceRecordResponse(
    Long id,
    Long studentId,
    String studentCode,
    String fullName,
    Instant checkInAt,
    Instant checkOutAt,
    String attendanceStatus
) {}
