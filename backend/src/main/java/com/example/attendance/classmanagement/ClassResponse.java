package com.example.attendance.classmanagement;

import java.time.Instant;

public record ClassResponse(
    Long id,
    String classCode,
    String name,
    String description,
    Boolean active,
    long studentCount,
    Instant createdAt
) {
    public static ClassResponse from(ClassEntity entity, long studentCount) {
        return new ClassResponse(
            entity.getId(),
            entity.getClassCode(),
            entity.getName(),
            entity.getDescription(),
            entity.getActive(),
            studentCount,
            entity.getCreatedAt()
        );
    }
}
