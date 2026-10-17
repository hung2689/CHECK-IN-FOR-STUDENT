package com.example.attendance.attendance;

import com.example.attendance.auth.User;
import com.example.attendance.auth.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AttendanceReviewController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    public AttendanceReviewController(AttendanceService attendanceService, UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/attendance-sessions/{sessionId}/review-attempts")
    public ResponseEntity<List<AttendanceReviewAttemptResponse>> getReviewAttempts(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) AttendanceStage stage) {
        return ResponseEntity.ok(attendanceService.getReviewAttempts(sessionId, status, stage));
    }

    @GetMapping("/api/attendance-sessions/{sessionId}/review-stats")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getReviewStats(sessionId));
    }

    @PostMapping("/api/attendance-review-attempts/{id}/approve")
    public ResponseEntity<AttendanceReviewAttemptResponse> approveAttempt(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewActionRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        String note = request != null ? request.note() : null;
        return ResponseEntity.ok(attendanceService.approveReviewAttempt(id, user.getId(), note));
    }

    @PostMapping("/api/attendance-review-attempts/{id}/reject")
    public ResponseEntity<AttendanceReviewAttemptResponse> rejectAttempt(
            @PathVariable Long id,
            @RequestBody ReviewActionRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(attendanceService.rejectReviewAttempt(id, user.getId(), reason));
    }

    @PostMapping("/api/attendance-review-attempts/{id}/mark-reviewed")
    public ResponseEntity<AttendanceReviewAttemptResponse> markReviewed(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewActionRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        String note = request != null ? request.note() : null;
        return ResponseEntity.ok(attendanceService.markReviewed(id, user.getId(), note));
    }
}
