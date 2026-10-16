package com.example.attendance.student;

import java.time.Instant;

public record StudentResponse(
    Long id,
    String studentCode,
    String fullName,
    String email,
    Boolean active,
    Instant createdAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getStudentCode(),
            student.getFullName(),
            student.getEmail(),
            student.getActive(),
            student.getCreatedAt()
        );
    }
}
