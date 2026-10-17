package com.example.attendance.attendance;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/attendance")
public class PublicAttendanceController {

    private final AttendanceService attendanceService;

    public PublicAttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicTokenInfo> validateToken(@PathVariable String token) {
        return ResponseEntity.ok(attendanceService.validateToken(token));
    }

    @PostMapping("/{token}")
    public ResponseEntity<PublicAttendanceResponse> submitAttendance(
            @PathVariable String token,
            @Valid @RequestBody PublicAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.submitAttendance(token, request));
    }
}
