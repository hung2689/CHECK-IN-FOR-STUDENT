package com.example.attendance.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SessionRequest(
    @NotBlank(message = "Session name is required")
    String name,

    @NotNull(message = "Attendance date is required")
    LocalDate attendanceDate,

    String description
) {}
