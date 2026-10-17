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
    Instant createdAt,
    Double geofenceLat,
    Double geofenceLng,
    Integer geofenceRadiusM,
    Boolean gpsEnforcement,
    long pendingReviewCount
) {
    public SessionResponse(
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
    ) {
        this(id, classId, className, classCode, name, description, attendanceDate, status,
             checkInStatus, checkOutStatus, checkInToken, checkOutToken, totalStudents,
             checkedInCount, checkedOutCount, createdAt, null, null, null, false, 0L);
    }
}
