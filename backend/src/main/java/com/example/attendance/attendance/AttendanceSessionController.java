package com.example.attendance.attendance;

import com.example.attendance.auth.User;
import com.example.attendance.auth.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AttendanceSessionController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    public AttendanceSessionController(AttendanceService attendanceService, UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/classes/{classId}/attendance-sessions")
    public ResponseEntity<List<SessionResponse>> getSessionsByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(attendanceService.getSessionsByClass(classId));
    }

    @PostMapping("/api/classes/{classId}/attendance-sessions")
    public ResponseEntity<SessionResponse> createSession(
            @PathVariable Long classId,
            @Valid @RequestBody SessionRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createSession(classId, request, user.getId()));
    }

    @GetMapping("/api/attendance-sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getSession(sessionId));
    }

    @PostMapping("/api/attendance-sessions/{sessionId}/check-in/open")
    public ResponseEntity<SessionResponse> openCheckIn(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.openCheckIn(sessionId));
    }

    @PostMapping("/api/attendance-sessions/{sessionId}/check-in/close")
    public ResponseEntity<SessionResponse> closeCheckIn(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.closeCheckIn(sessionId));
    }

    @PostMapping("/api/attendance-sessions/{sessionId}/check-out/open")
    public ResponseEntity<SessionResponse> openCheckOut(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.openCheckOut(sessionId));
    }

    @PostMapping("/api/attendance-sessions/{sessionId}/check-out/close")
    public ResponseEntity<SessionResponse> closeCheckOut(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.closeCheckOut(sessionId));
    }

    @PostMapping("/api/attendance-sessions/{sessionId}/cancel")
    public ResponseEntity<SessionResponse> cancelSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.cancelSession(sessionId));
    }

    @GetMapping("/api/attendance-sessions/{sessionId}/records")
    public ResponseEntity<List<AttendanceRecordResponse>> getRecords(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getAttendanceRecords(sessionId));
    }

    @DeleteMapping("/api/attendance-sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        attendanceService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}

