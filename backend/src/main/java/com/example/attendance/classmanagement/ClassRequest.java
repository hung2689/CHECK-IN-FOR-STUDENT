package com.example.attendance.classmanagement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassRequest(
    @NotBlank(message = "Class code is required")
    @Size(max = 20, message = "Class code must be at most 20 characters")
    String classCode,

    @NotBlank(message = "Class name is required")
    @Size(max = 200, message = "Class name must be at most 200 characters")
    String name,

    String description
) {}
