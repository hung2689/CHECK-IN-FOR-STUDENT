package com.example.attendance.attendance;

/**
 * Response returned to student after submitting attendance code.
 */
public record PublicAttendanceResponse(
    boolean success,
    String studentName,
    String stage,
    String gpsResult,
    boolean reviewRequired,
    String message
) {
    /** Backward-compatible 4-argument constructor */
    public PublicAttendanceResponse(boolean success, String studentName, String stage, String message) {
        this(success, studentName, stage, null, false, message);
    }
}
