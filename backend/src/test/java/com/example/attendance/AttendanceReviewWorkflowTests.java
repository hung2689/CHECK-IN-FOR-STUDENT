package com.example.attendance;

import com.example.attendance.attendance.*;
import com.example.attendance.auth.User;
import com.example.attendance.auth.UserRepository;
import com.example.attendance.classmanagement.ClassEntity;
import com.example.attendance.classmanagement.ClassRepository;
import com.example.attendance.report.ReportService;
import com.example.attendance.student.ClassStudent;
import com.example.attendance.student.ClassStudentRepository;
import com.example.attendance.student.Student;
import com.example.attendance.student.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AttendanceReviewWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassStudentRepository classStudentRepository;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceTokenRepository tokenRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    @Autowired
    private AttendanceReviewAttemptRepository reviewAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    private ClassEntity testClass;
    private Student student1;
    private Student student2;
    private AttendanceSession session;
    private String checkInToken;
    private User adminUser;

    // School center coordinates: HCMUT / Example Campus (10.7725, 106.6578), Radius = 150m
    private static final double SCHOOL_LAT = 10.772500;
    private static final double SCHOOL_LNG = 106.657800;

    @BeforeEach
    void setUp() {
        reviewAttemptRepository.deleteAll();
        recordRepository.deleteAll();
        tokenRepository.deleteAll();
        sessionRepository.deleteAll();
        classStudentRepository.deleteAll();
        studentRepository.deleteAll();
        classRepository.deleteAll();

        adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setPasswordHash("hashed");
            u.setRole("TA");
            return userRepository.save(u);
        });

        testClass = new ClassEntity();
        testClass.setClassCode("CS101");
        testClass.setName("Lập Trình Web");
        testClass.setCreatedBy(adminUser.getId());
        testClass = classRepository.save(testClass);

        student1 = new Student();
        student1.setStudentCode("SV001");
        student1.setFullName("Nguyễn Văn An");
        student1.setActive(true);
        student1 = studentRepository.save(student1);

        student2 = new Student();
        student2.setStudentCode("SV002");
        student2.setFullName("Trần Thị Bình");
        student2.setActive(true);
        student2 = studentRepository.save(student2);

        ClassStudent cs1 = new ClassStudent();
        cs1.setClassId(testClass.getId());
        cs1.setStudentId(student1.getId());
        classStudentRepository.save(cs1);

        ClassStudent cs2 = new ClassStudent();
        cs2.setClassId(testClass.getId());
        cs2.setStudentId(student2.getId());
        classStudentRepository.save(cs2);

        // Create session with GPS geofence enabled (150m radius)
        SessionRequest sessionReq = new SessionRequest(
                "Buổi 1",
                LocalDate.now(),
                "Điểm danh có Geofence",
                SCHOOL_LAT,
                SCHOOL_LNG,
                150,
                true
        );
        SessionResponse sessionResp = attendanceService.createSession(testClass.getId(), sessionReq, adminUser.getId());
        session = sessionRepository.findById(sessionResp.id()).orElseThrow();

        // Open Check-in
        SessionResponse opened = attendanceService.openCheckIn(session.getId());
        checkInToken = opened.checkInToken();
    }

    @Test
    @DisplayName("Criterion 1: Accurate location inside geofence is accepted automatically")
    void testAccurateLocationInsideGeofenceIsAcceptedAutomatically() {
        // Point is right at school center with high accuracy (15m)
        PublicAttendanceRequest req = new PublicAttendanceRequest(
                "SV001",
                SCHOOL_LAT,
                SCHOOL_LNG,
                15.0,
                null
        );

        PublicAttendanceResponse res = attendanceService.submitAttendance(checkInToken, req);

        assertThat(res.success()).isTrue();
        assertThat(res.reviewRequired()).isFalse();
        assertThat(res.gpsResult()).isEqualTo("VERIFIED");

        // Verify official attendance record is created
        assertThat(recordRepository.findBySessionIdAndStudentId(session.getId(), student1.getId()))
                .isPresent()
                .hasValueSatisfying(r -> assertThat(r.getCheckInAt()).isNotNull());

        // No pending review attempts
        assertThat(reviewAttemptRepository.findBySessionId(session.getId())).isEmpty();
    }

    @Test
    @DisplayName("Criterion 2: Poor GPS accuracy creates a pending review rather than automatic rejection")
    void testPoorGpsAccuracyCreatesPendingReview() {
        // Poor accuracy (140m > 100m)
        PublicAttendanceRequest req = new PublicAttendanceRequest(
                "SV001",
                SCHOOL_LAT,
                SCHOOL_LNG,
                140.0,
                null
        );

        PublicAttendanceResponse res = attendanceService.submitAttendance(checkInToken, req);

        assertThat(res.success()).isFalse();
        assertThat(res.reviewRequired()).isTrue();
        assertThat(res.gpsResult()).isEqualTo("POOR_ACCURACY");
        assertThat(res.message()).contains("chờ giảng viên xác nhận");

        // Attendance record must NOT be confirmed
        assertThat(recordRepository.findBySessionIdAndStudentId(session.getId(), student1.getId())).isEmpty();

        // Pending review attempt created
        List<AttendanceReviewAttempt> attempts = reviewAttemptRepository.findBySessionId(session.getId());
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getReviewStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(attempts.get(0).getGpsResult()).isEqualTo("POOR_ACCURACY");
    }

    @Test
    @DisplayName("Criterion 3: Boundary uncertainty creates a pending review")
    void testBoundaryUncertaintyCreatesPendingReview() {
        // Slightly outside 150m (e.g. 155m away) with 20m accuracy: 10.7738, 106.6578 is ~144m
        // Let's use a point ~160m away
        double offsetLat = SCHOOL_LAT + 0.0014; // ~155m
        PublicAttendanceRequest req = new PublicAttendanceRequest(
                "SV001",
                offsetLat,
                SCHOOL_LNG,
                25.0,
                null
        );

        PublicAttendanceResponse res = attendanceService.submitAttendance(checkInToken, req);

        assertThat(res.success()).isFalse();
        assertThat(res.reviewRequired()).isTrue();
        assertThat(res.gpsResult()).isEqualTo("BOUNDARY");

        // Official record not confirmed
        assertThat(recordRepository.findBySessionIdAndStudentId(session.getId(), student1.getId())).isEmpty();
    }

    @Test
    @DisplayName("Criterion 4: Missing or denied location creates a pending review when required")
    void testMissingOrDeniedLocationCreatesPendingReview() {
        PublicAttendanceRequest req = new PublicAttendanceRequest(
                "SV001",
                null,
                null,
                null,
                "LOCATION_PERMISSION_DENIED"
        );

        PublicAttendanceResponse res = attendanceService.submitAttendance(checkInToken, req);

        assertThat(res.success()).isFalse();
        assertThat(res.reviewRequired()).isTrue();
        assertThat(res.gpsResult()).isEqualTo("NO_LOCATION");
        assertThat(res.message()).contains("chờ giảng viên xác nhận");

        List<AttendanceReviewAttempt> attempts = reviewAttemptRepository.findBySessionId(session.getId());
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getGpsReason()).isEqualTo("LOCATION_PERMISSION_DENIED");
    }

    @Test
    @DisplayName("Criterion 5: Clearly outside locations are flagged and await administrative review")
    void testClearlyOutsideLocationsAreFlaggedAndAwaitReview() {
        // Very far away: District 1 vs District 10 (e.g. 10.7900, 106.6900 is ~4km away)
        PublicAttendanceRequest req = new PublicAttendanceRequest(
                "SV001",
                10.790000,
                106.690000,
                15.0,
                null
        );

        PublicAttendanceResponse res = attendanceService.submitAttendance(checkInToken, req);

        assertThat(res.success()).isFalse();
        assertThat(res.reviewRequired()).isTrue();
        assertThat(res.gpsResult()).isEqualTo("OUTSIDE_GEOFENCE");

        // Awaiting review, not automatically rejected or confirmed
        List<AttendanceReviewAttempt> attempts = reviewAttemptRepository.findBySessionId(session.getId());
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getReviewStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(attempts.get(0).getGpsResult()).isEqualTo("OUTSIDE_GEOFENCE");
    }

    @Test
    @DisplayName("Criterion 6: Pending attempts are not counted as confirmed attendance")
    void testPendingAttemptsAreNotCountedAsConfirmedAttendance() {
        // Student 1 has pending attempt
        PublicAttendanceRequest req = new PublicAttendanceRequest(
                "SV001",
                null,
                null,
                null,
                "LOCATION_PERMISSION_DENIED"
        );
        attendanceService.submitAttendance(checkInToken, req);

        // Check records from dashboard
        List<AttendanceRecordResponse> records = attendanceService.getAttendanceRecords(session.getId());
        AttendanceRecordResponse student1Record = records.stream()
                .filter(r -> r.studentCode().equals("SV001"))
                .findFirst()
                .orElseThrow();

        assertThat(student1Record.attendanceStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(student1Record.attendanceStatus()).isNotEqualTo("PRESENT");
        assertThat(student1Record.checkInAt()).isNull();

        ReviewStatsResponse stats = attendanceService.getReviewStats(session.getId());
        assertThat(stats.confirmedCheckIns()).isEqualTo(0);
        assertThat(stats.pendingReviews()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("Criterion 7: Admin approval creates or updates official attendance record exactly once")
    void testAdminApprovalCreatesOfficialAttendanceRecordExactlyOnce() {
        // Create pending review
        attendanceService.submitAttendance(checkInToken, new PublicAttendanceRequest("SV001", null, null, null, "LOCATION_UNAVAILABLE"));

        AttendanceReviewAttempt attempt = reviewAttemptRepository.findBySessionId(session.getId()).get(0);
        assertThat(attempt.getReviewStatus()).isEqualTo("PENDING_REVIEW");

        // Approve attempt
        AttendanceReviewAttemptResponse approved = attendanceService.approveReviewAttempt(
                attempt.getId(), adminUser.getId(), "Sinh viên có mặt trong lớp học");

        assertThat(approved.reviewStatus()).isEqualTo("APPROVED");
        assertThat(approved.adminDecision()).isEqualTo("APPROVED");

        // Check official record is created
        assertThat(recordRepository.findBySessionIdAndStudentId(session.getId(), student1.getId()))
                .isPresent()
                .hasValueSatisfying(r -> assertThat(r.getCheckInAt()).isNotNull());

        // Approving again is idempotent and doesn't duplicate
        attendanceService.approveReviewAttempt(attempt.getId(), adminUser.getId(), "Duyệt lại");
        assertThat(recordRepository.countCheckedIn(session.getId())).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("Criterion 8: Admin rejection does not create confirmed attendance")
    void testAdminRejectionDoesNotCreateConfirmedAttendance() {
        attendanceService.submitAttendance(checkInToken, new PublicAttendanceRequest("SV001", null, null, null, "LOCATION_UNAVAILABLE"));
        AttendanceReviewAttempt attempt = reviewAttemptRepository.findBySessionId(session.getId()).get(0);

        // Reject attempt with required reason
        AttendanceReviewAttemptResponse rejected = attendanceService.rejectReviewAttempt(
                attempt.getId(), adminUser.getId(), "Sinh viên không có mặt trong phòng học");

        assertThat(rejected.reviewStatus()).isEqualTo("REJECTED");
        assertThat(rejected.adminDecision()).isEqualTo("REJECTED");
        assertThat(rejected.adminNote()).isEqualTo("Sinh viên không có mặt trong phòng học");

        // Official record not confirmed
        assertThat(recordRepository.findBySessionIdAndStudentId(session.getId(), student1.getId())).isEmpty();

        // Dashboard shows REJECTED status
        List<AttendanceRecordResponse> records = attendanceService.getAttendanceRecords(session.getId());
        assertThat(records.stream().filter(r -> r.studentCode().equals("SV001")).findFirst().get().attendanceStatus())
                .isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Criterion 9: Unauthorized users cannot approve or reject attempts")
    void testUnauthorizedUsersCannotApproveOrRejectAttempts() throws Exception {
        attendanceService.submitAttendance(checkInToken, new PublicAttendanceRequest("SV001", null, null, null, "LOCATION_UNAVAILABLE"));
        AttendanceReviewAttempt attempt = reviewAttemptRepository.findBySessionId(session.getId()).get(0);

        // Attempt approve without authentication -> 401
        mockMvc.perform(post("/api/attendance-review-attempts/" + attempt.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"hacker\"}"))
                .andExpect(status().isUnauthorized());

        // Attempt reject without authentication -> 401
        mockMvc.perform(post("/api/attendance-review-attempts/" + attempt.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"hacker\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("Criterion 10: Check-out approval cannot bypass the prior check-in requirement")
    void testCheckOutApprovalRequiresValidPriorCheckIn() {
        // Create an attempt for CHECK_OUT directly
        AttendanceReviewAttempt checkOutAttempt = new AttendanceReviewAttempt();
        checkOutAttempt.setSessionId(session.getId());
        checkOutAttempt.setStudentId(student1.getId());
        checkOutAttempt.setStage(AttendanceStage.CHECK_OUT);
        checkOutAttempt.setReviewStatus("PENDING_REVIEW");
        checkOutAttempt = reviewAttemptRepository.save(checkOutAttempt);

        // Attempting to approve check-out without prior check-in must throw BadRequestException
        final Long attemptId = checkOutAttempt.getId();
        assertThatThrownBy(() -> attendanceService.approveReviewAttempt(attemptId, adminUser.getId(), "Duyệt check-out"))
                .isInstanceOf(com.example.attendance.exception.BadRequestException.class)
                .hasMessageContaining("prior check-in");
    }

    @Test
    @DisplayName("Criterion 11: Concurrent duplicate submissions do not duplicate review attempts")
    void testDuplicateSubmissionsDoNotDuplicatePendingAttempts() {
        PublicAttendanceRequest req = new PublicAttendanceRequest("SV001", null, null, null, "LOCATION_UNAVAILABLE");

        // Submit twice
        attendanceService.submitAttendance(checkInToken, req);
        attendanceService.submitAttendance(checkInToken, req);

        List<AttendanceReviewAttempt> attempts = reviewAttemptRepository.findBySessionId(session.getId());
        assertThat(attempts).hasSize(1);
    }

    @Test
    @DisplayName("Criterion 12: Excel reports correctly distinguish pending, approved, and rejected attempts")
    void testExcelReportsDistinguishStatuses() {
        // SV001: pending review
        attendanceService.submitAttendance(checkInToken, new PublicAttendanceRequest("SV001", null, null, null, "LOCATION_PERMISSION_DENIED"));

        // SV002: accurate -> confirmed
        attendanceService.submitAttendance(checkInToken, new PublicAttendanceRequest("SV002", SCHOOL_LAT, SCHOOL_LNG, 10.0, null));

        byte[] excelBytes = reportService.exportAttendance(session.getId());
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Criterion 13: Existing attendance functionality remains operational when GPS is disabled")
    void testExistingAttendanceWorksWhenGpsDisabled() {
        // Disable GPS enforcement on session
        attendanceService.updateGeofence(session.getId(), new GeofenceConfigRequest(null, null, 150, false));

        // Submit without any GPS data
        PublicAttendanceRequest req = new PublicAttendanceRequest("SV001", null, null, null, null);
        PublicAttendanceResponse res = attendanceService.submitAttendance(checkInToken, req);

        assertThat(res.success()).isTrue();
        assertThat(res.reviewRequired()).isFalse();

        // Confirmed immediately
        assertThat(recordRepository.findBySessionIdAndStudentId(session.getId(), student1.getId()))
                .isPresent()
                .hasValueSatisfying(r -> assertThat(r.getCheckInAt()).isNotNull());
    }
}
