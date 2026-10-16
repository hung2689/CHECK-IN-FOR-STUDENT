package com.example.attendance.attendance;

import java.time.Instant;
import java.time.LocalDate;

public record SessionResponse(
    Long id,
    Long classId,
    String className,
    String classCode,
    String name,
    String description,
    LocalDate attendanceDate,
    SessionStatus status,
    StageStatus checkInStatus,
    StageStatus checkOutStatus,
    String checkInToken,
    String checkOutToken,
    long totalStudents,
    long checkedInCount,
    long checkedOutCount,
    Instant createdAt
) {}
