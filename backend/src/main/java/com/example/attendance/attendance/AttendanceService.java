package com.example.attendance.attendance;

import com.example.attendance.auth.User;
import com.example.attendance.auth.UserRepository;
import com.example.attendance.classmanagement.ClassEntity;
import com.example.attendance.classmanagement.ClassRepository;
import com.example.attendance.exception.BadRequestException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.student.ClassStudentRepository;
import com.example.attendance.student.Student;
import com.example.attendance.student.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceTokenRepository tokenRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceReviewAttemptRepository reviewAttemptRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Value("${app.token.expiry-hours:24}")
    private int tokenExpiryHours;

    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             AttendanceTokenRepository tokenRepository,
                             AttendanceRecordRepository recordRepository,
                             AttendanceReviewAttemptRepository reviewAttemptRepository,
                             ClassRepository classRepository,
                             ClassStudentRepository classStudentRepository,
                             StudentRepository studentRepository,
                             UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
        this.recordRepository = recordRepository;
        this.reviewAttemptRepository = reviewAttemptRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    public List<SessionResponse> getSessionsByClass(Long classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        return sessionRepository.findByClassIdOrderByAttendanceDateDesc(classId)
                .stream().map(this::toSessionResponse).toList();
    }

    public SessionResponse getSession(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse createSession(Long classId, SessionRequest request, Long createdBy) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        AttendanceSession session = new AttendanceSession();
        session.setClassId(classId);
        session.setName(request.name().trim());
        session.setDescription(request.description());
        session.setAttendanceDate(request.attendanceDate());
        session.setCreatedBy(createdBy);

        if (request.geofenceLat() != null && request.geofenceLng() != null) {
            session.setGeofenceLat(request.geofenceLat());
            session.setGeofenceLng(request.geofenceLng());
            session.setGeofenceRadiusM(request.geofenceRadiusM() != null ? request.geofenceRadiusM() : 150);
            session.setGpsEnforcement(request.gpsEnforcement() != null && request.gpsEnforcement());
        } else {
            session.setGpsEnforcement(false);
        }

        session = sessionRepository.save(session);
        log.info("Session created: id={}, classId={}, name={}, gpsEnforced={}",
                session.getId(), classId, session.getName(), session.getGpsEnforcement());
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse updateGeofence(Long sessionId, GeofenceConfigRequest request) {
        AttendanceSession session = getSessionEntity(sessionId);
        validateSessionNotCancelled(session);

        session.setGeofenceLat(request.geofenceLat());
        session.setGeofenceLng(request.geofenceLng());
        if (request.geofenceRadiusM() != null) {
            session.setGeofenceRadiusM(request.geofenceRadiusM());
        } else if (session.getGeofenceRadiusM() == null) {
            session.setGeofenceRadiusM(150);
        }
        if (request.gpsEnforcement() != null) {
            session.setGpsEnforcement(request.gpsEnforcement());
        }

        session = sessionRepository.save(session);
        log.info("Geofence updated for session id={}: lat={}, lng={}, radius={}m, enforced={}",
                sessionId, session.getGeofenceLat(), session.getGeofenceLng(),
                session.getGeofenceRadiusM(), session.getGpsEnforcement());
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse openCheckIn(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);
        validateSessionNotCancelled(session);

        if (session.getCheckInStatus() != StageStatus.NOT_OPEN && session.getCheckInStatus() != StageStatus.CLOSED) {
            throw new BadRequestException("Check-in is already open");
        }

        session.setCheckInStatus(StageStatus.OPEN);
        session.setStatus(SessionStatus.IN_PROGRESS);

        // Generate token
        String tokenValue = generateToken();
        AttendanceToken token = new AttendanceToken();
        token.setSessionId(sessionId);
        token.setStage(AttendanceStage.CHECK_IN);
        token.setTokenValue(tokenValue);
        token.setTokenHash(hashToken(tokenValue));
        token.setExpiresAt(Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS));
        tokenRepository.save(token);

        session = sessionRepository.save(session);
        log.info("Check-in opened for session: id={}", sessionId);
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse closeCheckIn(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);

        if (session.getCheckInStatus() != StageStatus.OPEN) {
            throw new BadRequestException("Check-in is not open");
        }

        session.setCheckInStatus(StageStatus.CLOSED);
        tokenRepository.revokeTokensBySessionAndStage(sessionId, AttendanceStage.CHECK_IN, Instant.now());

        session = sessionRepository.save(session);
        log.info("Check-in closed for session: id={}", sessionId);
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse openCheckOut(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);
        validateSessionNotCancelled(session);

        if (session.getCheckInStatus() != StageStatus.CLOSED) {
            throw new BadRequestException("Check-in must be closed before opening check-out");
        }

        if (session.getCheckOutStatus() != StageStatus.NOT_OPEN && session.getCheckOutStatus() != StageStatus.CLOSED) {
            throw new BadRequestException("Check-out is already open");
        }

        session.setCheckOutStatus(StageStatus.OPEN);

        // Generate token
        String tokenValue = generateToken();
        AttendanceToken token = new AttendanceToken();
        token.setSessionId(sessionId);
        token.setStage(AttendanceStage.CHECK_OUT);
        token.setTokenValue(tokenValue);
        token.setTokenHash(hashToken(tokenValue));
        token.setExpiresAt(Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS));
        tokenRepository.save(token);

        session = sessionRepository.save(session);
        log.info("Check-out opened for session: id={}", sessionId);
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse closeCheckOut(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);

        if (session.getCheckOutStatus() != StageStatus.OPEN) {
            throw new BadRequestException("Check-out is not open");
        }

        session.setCheckOutStatus(StageStatus.CLOSED);
        session.setStatus(SessionStatus.COMPLETED);
        tokenRepository.revokeTokensBySessionAndStage(sessionId, AttendanceStage.CHECK_OUT, Instant.now());

        session = sessionRepository.save(session);
        log.info("Check-out closed for session: id={}", sessionId);
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse cancelSession(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);
        session.setStatus(SessionStatus.CANCELLED);
        session.setCheckInStatus(StageStatus.CLOSED);
        session.setCheckOutStatus(StageStatus.CLOSED);
        tokenRepository.revokeTokensBySessionAndStage(sessionId, AttendanceStage.CHECK_IN, Instant.now());
        tokenRepository.revokeTokensBySessionAndStage(sessionId, AttendanceStage.CHECK_OUT, Instant.now());

        session = sessionRepository.save(session);
        log.info("Session cancelled: id={}", sessionId);
        return toSessionResponse(session);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);
        reviewAttemptRepository.deleteBySessionId(sessionId);
        recordRepository.deleteBySessionId(sessionId);
        tokenRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
        log.info("Attendance session deleted: id={}", sessionId);
    }

    // Public attendance methods
    public PublicTokenInfo validateToken(String tokenValue) {
        AttendanceToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElse(null);

        if (token == null || !token.isValid()) {
            return new PublicTokenInfo(false, null, null, null, null,
                    "This attendance link is invalid or has expired.");
        }

        AttendanceSession session = sessionRepository.findById(token.getSessionId()).orElse(null);
        if (session == null || session.getStatus() == SessionStatus.CANCELLED) {
            return new PublicTokenInfo(false, null, null, null, null,
                    "This attendance session is no longer active.");
        }

        // Check stage status
        StageStatus stageStatus = token.getStage() == AttendanceStage.CHECK_IN
                ? session.getCheckInStatus() : session.getCheckOutStatus();
        if (stageStatus != StageStatus.OPEN) {
            return new PublicTokenInfo(false, null, null, null, null,
                    "This attendance stage is currently closed.");
        }

        ClassEntity classEntity = classRepository.findById(session.getClassId()).orElse(null);
        String className = classEntity != null ? classEntity.getName() : "Unknown";

        return new PublicTokenInfo(true, className, session.getName(),
                token.getStage().name(), session.getAttendanceDate().toString(), null);
    }

    public PublicAttendanceResponse submitAttendance(String tokenValue, String studentCode) {
        return submitAttendance(tokenValue, new PublicAttendanceRequest(studentCode, null, null, null, null));
    }

    @Transactional
    public PublicAttendanceResponse submitAttendance(String tokenValue, PublicAttendanceRequest request) {
        String studentCode = request.studentCode().trim();

        AttendanceToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid attendance token."));

        if (!token.isValid()) {
            throw new BadRequestException("This attendance link has expired or been revoked.");
        }

        AttendanceSession session = getSessionEntity(token.getSessionId());
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("This attendance session has been cancelled.");
        }

        // Validate stage is open
        AttendanceStage stage = token.getStage();
        StageStatus stageStatus = stage == AttendanceStage.CHECK_IN
                ? session.getCheckInStatus() : session.getCheckOutStatus();
        if (stageStatus != StageStatus.OPEN) {
            throw new BadRequestException("This attendance stage is currently closed.");
        }

        // Validate student enrollment
        Student student = studentRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new BadRequestException("Student code not found in this class."));

        if (!student.getActive()) {
            throw new BadRequestException("This student account is inactive.");
        }

        boolean enrolled = classStudentRepository.existsByClassIdAndStudentId(session.getClassId(), student.getId());
        if (!enrolled) {
            throw new BadRequestException("Student code not found in this class.");
        }

        // Check if student already has a confirmed record for this stage
        Optional<AttendanceRecord> existing = recordRepository
                .findBySessionIdAndStudentId(session.getId(), student.getId());

        if (stage == AttendanceStage.CHECK_IN) {
            if (existing.isPresent() && existing.get().getCheckInAt() != null) {
                return new PublicAttendanceResponse(true, student.getFullName(),
                        "CHECK_IN", "VERIFIED", false, "You have already checked in successfully.");
            }
        } else {
            // Check-out requires valid prior check-in
            if (existing.isEmpty() || existing.get().getCheckInAt() == null) {
                throw new BadRequestException("Bạn chưa hoàn thành Check-in, không thể Check-out.");
            }
            if (existing.get().getCheckOutAt() != null) {
                return new PublicAttendanceResponse(true, student.getFullName(),
                        "CHECK_OUT", "VERIFIED", false, "You have already checked out successfully.");
            }
        }

        // Evaluate GPS if enabled
        GpsEvaluationResult eval = evaluateGps(session, request);

        if (eval.autoAccept()) {
            // Case A or GPS disabled: accept immediately
            if (stage == AttendanceStage.CHECK_IN) {
                return processCheckIn(session, student, eval.result());
            } else {
                return processCheckOut(session, student, eval.result());
            }
        }

        // Case B / C / D: GPS uncertain, poor accuracy, boundary, outside, or missing location
        // Queue for administrative review
        Optional<AttendanceReviewAttempt> pendingOpt = reviewAttemptRepository
                .findBySessionIdAndStudentIdAndStageAndReviewStatus(
                        session.getId(), student.getId(), stage, "PENDING_REVIEW");

        AttendanceReviewAttempt attempt = pendingOpt.orElseGet(() -> {
            AttendanceReviewAttempt a = new AttendanceReviewAttempt();
            a.setSessionId(session.getId());
            a.setStudentId(student.getId());
            a.setStage(stage);
            return a;
        });

        attempt.setSubmittedAt(Instant.now());
        attempt.setLatitude(request.latitude());
        attempt.setLongitude(request.longitude());
        attempt.setGpsAccuracyM(request.gpsAccuracyM());
        attempt.setDistanceFromSchool(eval.distanceMetres());
        attempt.setGpsResult(eval.result());
        attempt.setGpsReason(eval.reason());
        attempt.setReviewStatus("PENDING_REVIEW");
        reviewAttemptRepository.save(attempt);

        log.warn("Attendance attempt queued for review: session={}, student={}, stage={}, result={}, reason={}",
                session.getId(), student.getStudentCode(), stage, eval.result(), eval.reason());

        return new PublicAttendanceResponse(
                false,
                student.getFullName(),
                stage.name(),
                eval.result(),
                true,
                "Yêu cầu điểm danh của bạn đã được gửi và đang chờ giảng viên xác nhận. Lượt điểm danh chưa được xác nhận chính thức."
        );
    }

    public record GpsEvaluationResult(
            String result,
            String reason,
            Double distanceMetres,
            boolean autoAccept
    ) {}

    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // metres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public GpsEvaluationResult evaluateGps(AttendanceSession session, PublicAttendanceRequest request) {
        if (!Boolean.TRUE.equals(session.getGpsEnforcement())) {
            return new GpsEvaluationResult("DISABLED", "GPS enforcement is disabled", null, true);
        }

        if (session.getGeofenceLat() == null || session.getGeofenceLng() == null) {
            return new GpsEvaluationResult("DISABLED", "Session geofence not configured", null, true);
        }

        // Case D: Location unavailable or denied
        if (request.latitude() == null || request.longitude() == null) {
            String reason = request.locationUnavailableReason();
            if (reason == null || reason.isBlank()) {
                reason = "LOCATION_UNAVAILABLE";
            }
            return new GpsEvaluationResult("NO_LOCATION", reason, null, false);
        }

        double centerLat = session.getGeofenceLat();
        double centerLng = session.getGeofenceLng();
        double studentLat = request.latitude();
        double studentLng = request.longitude();

        double distance = calculateHaversineDistance(centerLat, centerLng, studentLat, studentLng);
        double accuracy = request.gpsAccuracyM() != null ? request.gpsAccuracyM() : 0.0;
        int radius = session.getGeofenceRadiusM() != null ? session.getGeofenceRadiusM() : 150;

        // Case B1: Poor GPS accuracy (> 100 metres)
        if (accuracy > 100.0) {
            return new GpsEvaluationResult(
                    "POOR_ACCURACY",
                    String.format("Độ chính xác GPS quá thấp (±%.0fm > 100m, khoảng cách %.0fm)", accuracy, distance),
                    distance,
                    false
            );
        }

        // Case B2: Boundary uncertainty
        // Condition:
        // 1) Inside radius, but uncertainty circle extends outside (distance + accuracy > radius + 15m)
        // 2) Outside radius, but accuracy circle covers radius or very close (distance - accuracy <= radius || distance <= radius + 25m)
        boolean isNearBoundary = (distance <= radius && (distance + accuracy > radius + 15.0)) ||
                (distance > radius && ((distance - accuracy <= radius) || (distance <= radius + 25.0)));

        if (isNearBoundary) {
            return new GpsEvaluationResult(
                    "BOUNDARY",
                    String.format("Nằm gần ranh giới khu vực lớp học (khoảng cách: %.0fm, bán kính: %dm, sai số: ±%.0fm)",
                            distance, radius, accuracy),
                    distance,
                    false
            );
        }

        // Case A: Confidently inside
        if (distance <= radius) {
            return new GpsEvaluationResult(
                    "VERIFIED",
                    String.format("Vị trí hợp lệ trong khuôn viên (cách %.0fm)", distance),
                    distance,
                    true
            );
        }

        // Case C: Clearly outside
        return new GpsEvaluationResult(
                "OUTSIDE_GEOFENCE",
                String.format("Ngoài khuôn viên lớp học (khoảng cách: %.0fm, bán kính: %dm, sai số: ±%.0fm)",
                        distance, radius, accuracy),
                distance,
                false
        );
    }

    private PublicAttendanceResponse processCheckIn(AttendanceSession session, Student student, String gpsResult) {
        Optional<AttendanceRecord> existing = recordRepository
                .findBySessionIdAndStudentId(session.getId(), student.getId());

        if (existing.isPresent() && existing.get().getCheckInAt() != null) {
            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_IN", gpsResult, false, "You have already checked in successfully.");
        }

        try {
            AttendanceRecord record = existing.orElseGet(() -> {
                AttendanceRecord r = new AttendanceRecord();
                r.setSessionId(session.getId());
                r.setStudentId(student.getId());
                return r;
            });
            record.setCheckInAt(Instant.now());
            recordRepository.save(record);

            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_IN", gpsResult, false, "Check-in successful!");
        } catch (DataIntegrityViolationException e) {
            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_IN", gpsResult, false, "You have already checked in successfully.");
        }
    }

    private PublicAttendanceResponse processCheckOut(AttendanceSession session, Student student, String gpsResult) {
        try {
            AttendanceRecord record = recordRepository
                    .findBySessionIdAndStudentId(session.getId(), student.getId())
                    .orElseThrow(() -> new BadRequestException("Bạn chưa hoàn thành Check-in, không thể Check-out."));

            if (record.getCheckInAt() == null) {
                throw new BadRequestException("Bạn chưa hoàn thành Check-in, không thể Check-out.");
            }

            if (record.getCheckOutAt() != null) {
                return new PublicAttendanceResponse(true, student.getFullName(),
                        "CHECK_OUT", gpsResult, false, "You have already checked out successfully.");
            }

            record.setCheckOutAt(Instant.now());
            recordRepository.save(record);

            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_OUT", gpsResult, false, "Check-out successful!");
        } catch (DataIntegrityViolationException e) {
            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_OUT", gpsResult, false, "You have already checked out successfully.");
        }
    }

    // ── Administrative Review Workflow ─────────────────────────────────────────

    public List<AttendanceReviewAttemptResponse> getReviewAttempts(Long sessionId, String status, AttendanceStage stage) {
        AttendanceSession session = getSessionEntity(sessionId);
        List<AttendanceReviewAttempt> attempts;

        if (status != null && !status.isBlank() && stage != null) {
            attempts = reviewAttemptRepository
                    .findBySessionIdAndStageAndReviewStatusOrderBySubmittedAtDesc(sessionId, stage, status);
        } else if (status != null && !status.isBlank()) {
            attempts = reviewAttemptRepository
                    .findBySessionIdAndReviewStatusOrderBySubmittedAtDesc(sessionId, status);
        } else if (stage != null) {
            attempts = reviewAttemptRepository
                    .findBySessionIdAndStageOrderBySubmittedAtDesc(sessionId, stage);
        } else {
            attempts = reviewAttemptRepository.findBySessionIdOrderBySubmittedAtDesc(sessionId);
        }

        // Cache student & reviewer user lookups
        Map<Long, Student> studentMap = new HashMap<>();
        Map<Long, User> userMap = new HashMap<>();

        return attempts.stream().map(a -> {
            Student student = studentMap.computeIfAbsent(a.getStudentId(),
                    id -> studentRepository.findById(id).orElse(null));
            User reviewer = a.getReviewedBy() != null
                    ? userMap.computeIfAbsent(a.getReviewedBy(), id -> userRepository.findById(id).orElse(null))
                    : null;

            return new AttendanceReviewAttemptResponse(
                    a.getId(),
                    a.getSessionId(),
                    session.getName(),
                    a.getStudentId(),
                    student != null ? student.getStudentCode() : "N/A",
                    student != null ? student.getFullName() : "N/A",
                    a.getStage().name(),
                    a.getSubmittedAt(),
                    a.getLatitude(),
                    a.getLongitude(),
                    a.getGpsAccuracyM(),
                    a.getDistanceFromSchool(),
                    a.getGpsResult(),
                    a.getGpsReason(),
                    a.getReviewStatus(),
                    a.getReviewedBy(),
                    reviewer != null ? reviewer.getUsername() : null,
                    a.getReviewedAt(),
                    a.getAdminDecision(),
                    a.getAdminNote()
            );
        }).toList();
    }

    public ReviewStatsResponse getReviewStats(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);
        long totalStudents = classStudentRepository.countByClassId(session.getClassId());
        long confirmedCheckIns = recordRepository.countCheckedIn(sessionId);
        long confirmedCheckOuts = recordRepository.countCheckedOut(sessionId);
        long pendingReviews = reviewAttemptRepository.countBySessionIdAndReviewStatus(sessionId, "PENDING_REVIEW");
        long rejectedAttempts = reviewAttemptRepository.countBySessionIdAndReviewStatus(sessionId, "REJECTED");
        long suspiciousAttempts = reviewAttemptRepository.countBySessionIdAndGpsResult(sessionId, "OUTSIDE_GEOFENCE");

        return new ReviewStatsResponse(
                totalStudents,
                confirmedCheckIns,
                confirmedCheckOuts,
                pendingReviews,
                rejectedAttempts,
                suspiciousAttempts
        );
    }

    @Transactional
    public AttendanceReviewAttemptResponse approveReviewAttempt(Long attemptId, Long adminUserId, String note) {
        AttendanceReviewAttempt attempt = reviewAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Review attempt not found"));

        AttendanceSession session = getSessionEntity(attempt.getSessionId());
        Student student = studentRepository.findById(attempt.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // If stage is CHECK_OUT, check-out approval requires a valid prior check-in!
        if (attempt.getStage() == AttendanceStage.CHECK_OUT) {
            Optional<AttendanceRecord> priorRecord = recordRepository
                    .findBySessionIdAndStudentId(attempt.getSessionId(), attempt.getStudentId());
            if (priorRecord.isEmpty() || priorRecord.get().getCheckInAt() == null) {
                throw new BadRequestException("Check-out approval requires a valid prior check-in.");
            }
        }

        // Create or update official AttendanceRecord exactly once
        final Long sessionId = attempt.getSessionId();
        final Long studentId = attempt.getStudentId();
        AttendanceRecord record = recordRepository
                .findBySessionIdAndStudentId(sessionId, studentId)
                .orElseGet(() -> {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setSessionId(sessionId);
                    r.setStudentId(studentId);
                    return r;
                });

        Instant recordTime = attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : Instant.now();
        if (attempt.getStage() == AttendanceStage.CHECK_IN) {
            if (record.getCheckInAt() == null) {
                record.setCheckInAt(recordTime);
            }
        } else {
            if (record.getCheckOutAt() == null) {
                record.setCheckOutAt(recordTime);
            }
        }
        recordRepository.save(record);

        // Update attempt record
        attempt.setReviewStatus("APPROVED");
        attempt.setAdminDecision("APPROVED");
        attempt.setReviewedBy(adminUserId);
        attempt.setReviewedAt(Instant.now());
        attempt.setAdminNote(note);
        attempt = reviewAttemptRepository.save(attempt);

        log.info("Attendance review attempt approved: attemptId={}, studentId={}, stage={}, byAdmin={}",
                attemptId, student.getId(), attempt.getStage(), adminUserId);

        User reviewer = userRepository.findById(adminUserId).orElse(null);
        return new AttendanceReviewAttemptResponse(
                attempt.getId(),
                attempt.getSessionId(),
                session.getName(),
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                attempt.getStage().name(),
                attempt.getSubmittedAt(),
                attempt.getLatitude(),
                attempt.getLongitude(),
                attempt.getGpsAccuracyM(),
                attempt.getDistanceFromSchool(),
                attempt.getGpsResult(),
                attempt.getGpsReason(),
                attempt.getReviewStatus(),
                attempt.getReviewedBy(),
                reviewer != null ? reviewer.getUsername() : null,
                attempt.getReviewedAt(),
                attempt.getAdminDecision(),
                attempt.getAdminNote()
        );
    }

    @Transactional
    public AttendanceReviewAttemptResponse rejectReviewAttempt(Long attemptId, Long adminUserId, String reason) {
        if (reason == null || reason.trim().isBlank()) {
            throw new BadRequestException("Rejection reason is required.");
        }

        AttendanceReviewAttempt attempt = reviewAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Review attempt not found"));

        AttendanceSession session = getSessionEntity(attempt.getSessionId());
        Student student = studentRepository.findById(attempt.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        attempt.setReviewStatus("REJECTED");
        attempt.setAdminDecision("REJECTED");
        attempt.setReviewedBy(adminUserId);
        attempt.setReviewedAt(Instant.now());
        attempt.setAdminNote(reason.trim());
        attempt = reviewAttemptRepository.save(attempt);

        log.info("Attendance review attempt rejected: attemptId={}, studentId={}, stage={}, byAdmin={}, reason={}",
                attemptId, student.getId(), attempt.getStage(), adminUserId, reason);

        User reviewer = userRepository.findById(adminUserId).orElse(null);
        return new AttendanceReviewAttemptResponse(
                attempt.getId(),
                attempt.getSessionId(),
                session.getName(),
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                attempt.getStage().name(),
                attempt.getSubmittedAt(),
                attempt.getLatitude(),
                attempt.getLongitude(),
                attempt.getGpsAccuracyM(),
                attempt.getDistanceFromSchool(),
                attempt.getGpsResult(),
                attempt.getGpsReason(),
                attempt.getReviewStatus(),
                attempt.getReviewedBy(),
                reviewer != null ? reviewer.getUsername() : null,
                attempt.getReviewedAt(),
                attempt.getAdminDecision(),
                attempt.getAdminNote()
        );
    }

    @Transactional
    public AttendanceReviewAttemptResponse markReviewed(Long attemptId, Long adminUserId, String note) {
        AttendanceReviewAttempt attempt = reviewAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Review attempt not found"));

        AttendanceSession session = getSessionEntity(attempt.getSessionId());
        Student student = studentRepository.findById(attempt.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        attempt.setReviewStatus("REVIEWED");
        attempt.setReviewedBy(adminUserId);
        attempt.setReviewedAt(Instant.now());
        if (note != null && !note.trim().isBlank()) {
            attempt.setAdminNote(note.trim());
        }
        attempt = reviewAttemptRepository.save(attempt);

        User reviewer = userRepository.findById(adminUserId).orElse(null);
        return new AttendanceReviewAttemptResponse(
                attempt.getId(),
                attempt.getSessionId(),
                session.getName(),
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                attempt.getStage().name(),
                attempt.getSubmittedAt(),
                attempt.getLatitude(),
                attempt.getLongitude(),
                attempt.getGpsAccuracyM(),
                attempt.getDistanceFromSchool(),
                attempt.getGpsResult(),
                attempt.getGpsReason(),
                attempt.getReviewStatus(),
                attempt.getReviewedBy(),
                reviewer != null ? reviewer.getUsername() : null,
                attempt.getReviewedAt(),
                attempt.getAdminDecision(),
                attempt.getAdminNote()
        );
    }

    // Dashboard records
    public List<AttendanceRecordResponse> getAttendanceRecords(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);
        List<Student> students = studentRepository.findStudentsByClassId(session.getClassId());
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);
        List<AttendanceReviewAttempt> attempts = reviewAttemptRepository.findBySessionIdOrderBySubmittedAtDesc(sessionId);

        Map<Long, AttendanceRecord> recordMap = new HashMap<>();
        for (AttendanceRecord r : records) {
            recordMap.put(r.getStudentId(), r);
        }

        Map<Long, List<AttendanceReviewAttempt>> attemptMap = new HashMap<>();
        for (AttendanceReviewAttempt a : attempts) {
            attemptMap.computeIfAbsent(a.getStudentId(), k -> new ArrayList<>()).add(a);
        }

        return students.stream().map(student -> {
            AttendanceRecord record = recordMap.get(student.getId());
            List<AttendanceReviewAttempt> studentAttempts = attemptMap.getOrDefault(student.getId(), List.of());
            String status = computeAttendanceStatus(record, studentAttempts);

            return new AttendanceRecordResponse(
                    record != null ? record.getId() : null,
                    student.getId(),
                    student.getStudentCode(),
                    student.getFullName(),
                    record != null ? record.getCheckInAt() : null,
                    record != null ? record.getCheckOutAt() : null,
                    status
            );
        }).toList();
    }

    private String computeAttendanceStatus(AttendanceRecord record, List<AttendanceReviewAttempt> studentAttempts) {
        boolean hasCheckIn = record != null && record.getCheckInAt() != null;
        boolean hasCheckOut = record != null && record.getCheckOutAt() != null;

        if (hasCheckIn && hasCheckOut) return "PRESENT";
        if (hasCheckIn && !hasCheckOut) {
            boolean hasPendingCheckOut = studentAttempts.stream()
                    .anyMatch(a -> a.getStage() == AttendanceStage.CHECK_OUT && "PENDING_REVIEW".equals(a.getReviewStatus()));
            if (hasPendingCheckOut) return "PENDING_REVIEW";
            return "MISSING_CHECK_OUT";
        }
        if (!hasCheckIn && hasCheckOut) return "MISSING_CHECK_IN";

        // If not checked in officially, check review attempts
        boolean hasPending = studentAttempts.stream()
                .anyMatch(a -> "PENDING_REVIEW".equals(a.getReviewStatus()));
        if (hasPending) {
            return "PENDING_REVIEW";
        }

        boolean hasRejected = studentAttempts.stream()
                .anyMatch(a -> "REJECTED".equals(a.getReviewStatus()));
        if (hasRejected) {
            return "REJECTED";
        }

        return "ABSENT";
    }

    private SessionResponse toSessionResponse(AttendanceSession session) {
        ClassEntity classEntity = classRepository.findById(session.getClassId()).orElse(null);
        long totalStudents = classStudentRepository.countByClassId(session.getClassId());
        long checkedIn = recordRepository.countCheckedIn(session.getId());
        long checkedOut = recordRepository.countCheckedOut(session.getId());
        long pendingReviews = reviewAttemptRepository.countBySessionIdAndReviewStatus(session.getId(), "PENDING_REVIEW");

        // Get active tokens
        String checkInToken = getActiveToken(session.getId(), AttendanceStage.CHECK_IN);
        String checkOutToken = getActiveToken(session.getId(), AttendanceStage.CHECK_OUT);

        return new SessionResponse(
                session.getId(),
                session.getClassId(),
                classEntity != null ? classEntity.getName() : null,
                classEntity != null ? classEntity.getClassCode() : null,
                session.getName(),
                session.getDescription(),
                session.getAttendanceDate(),
                session.getStatus(),
                session.getCheckInStatus(),
                session.getCheckOutStatus(),
                checkInToken,
                checkOutToken,
                totalStudents,
                checkedIn,
                checkedOut,
                session.getCreatedAt(),
                session.getGeofenceLat(),
                session.getGeofenceLng(),
                session.getGeofenceRadiusM(),
                session.getGpsEnforcement(),
                pendingReviews
        );
    }

    private String getActiveToken(Long sessionId, AttendanceStage stage) {
        return tokenRepository.findBySessionIdAndStage(sessionId, stage).stream()
                .filter(AttendanceToken::isValid)
                .map(AttendanceToken::getTokenValue)
                .findFirst()
                .orElse(null);
    }

    private AttendanceSession getSessionEntity(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    }

    private void validateSessionNotCancelled(AttendanceSession session) {
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("This session has been cancelled");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
