package com.example.attendance.student;

import java.util.List;

public record ImportResult(
    int totalRows,
    int successCount,
    int skippedCount,
    int errorCount,
    List<ImportError> errors
) {
    public record ImportError(int row, String studentCode, String reason) {}
}
