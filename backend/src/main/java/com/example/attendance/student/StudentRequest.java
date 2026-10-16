package com.example.attendance.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRequest(
    @NotBlank(message = "Student code is required")
    @Size(max = 50, message = "Student code must be at most 50 characters")
    String studentCode,

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must be at most 200 characters")
    String fullName,

    String email
) {}
