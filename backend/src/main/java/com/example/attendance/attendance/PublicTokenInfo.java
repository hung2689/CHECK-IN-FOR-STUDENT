package com.example.attendance.attendance;

public record PublicTokenInfo(
    boolean valid,
    String className,
    String sessionName,
    String stage,
    String attendanceDate,
    String message
) {}
