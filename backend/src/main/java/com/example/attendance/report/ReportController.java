package com.example.attendance.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance-sessions")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{sessionId}/export")
    public ResponseEntity<byte[]> exportAttendance(@PathVariable Long sessionId) {
        byte[] report = reportService.exportAttendance(sessionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_report_" + sessionId + ".xlsx")
                .body(report);
    }
}
