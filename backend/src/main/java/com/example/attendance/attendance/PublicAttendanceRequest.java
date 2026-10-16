package com.example.attendance.attendance;

import jakarta.validation.constraints.NotBlank;

public record PublicAttendanceRequest(
    @NotBlank(message = "Student code is required")
    String studentCode
) {}
