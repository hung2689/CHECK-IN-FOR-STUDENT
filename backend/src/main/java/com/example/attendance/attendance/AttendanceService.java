package com.example.attendance.attendance;

import com.example.attendance.classmanagement.ClassEntity;
import com.example.attendance.classmanagement.ClassRepository;
import com.example.attendance.exception.BadRequestException;
import com.example.attendance.exception.ConflictException;
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
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentRepository studentRepository;

    @Value("${app.token.expiry-hours:24}")
    private int tokenExpiryHours;

    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             AttendanceTokenRepository tokenRepository,
                             AttendanceRecordRepository recordRepository,
                             ClassRepository classRepository,
                             ClassStudentRepository classStudentRepository,
                             StudentRepository studentRepository) {
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
        this.recordRepository = recordRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.studentRepository = studentRepository;
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
        session = sessionRepository.save(session);

        log.info("Session created: id={}, classId={}, name={}", session.getId(), classId, session.getName());
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

    @Transactional
    public PublicAttendanceResponse submitAttendance(String tokenValue, String studentCode) {
        studentCode = studentCode.trim();

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

        // Process attendance
        if (stage == AttendanceStage.CHECK_IN) {
            return processCheckIn(session, student);
        } else {
            return processCheckOut(session, student);
        }
    }

    private PublicAttendanceResponse processCheckIn(AttendanceSession session, Student student) {
        // Try to create or get existing record
        Optional<AttendanceRecord> existing = recordRepository
                .findBySessionIdAndStudentId(session.getId(), student.getId());

        if (existing.isPresent() && existing.get().getCheckInAt() != null) {
            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_IN", "You have already checked in successfully.");
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
                    "CHECK_IN", "Check-in successful!");
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate - still treat as success
            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_IN", "You have already checked in successfully.");
        }
    }

    private PublicAttendanceResponse processCheckOut(AttendanceSession session, Student student) {
        try {
            AttendanceRecord record = recordRepository
                    .findBySessionIdAndStudentId(session.getId(), student.getId())
                    .orElseGet(() -> {
                        AttendanceRecord newRec = new AttendanceRecord();
                        newRec.setSessionId(session.getId());
                        newRec.setStudentId(student.getId());
                        return newRec;
                    });

            if (record.getCheckOutAt() != null) {
                return new PublicAttendanceResponse(true, student.getFullName(),
                        "CHECK_OUT", "You have already checked out successfully.");
            }

            record.setCheckOutAt(Instant.now());
            recordRepository.save(record);

            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_OUT", "Check-out successful!");
        } catch (DataIntegrityViolationException e) {
            return new PublicAttendanceResponse(true, student.getFullName(),
                    "CHECK_OUT", "You have already checked out successfully.");
        }
    }

    // Dashboard records
    public List<AttendanceRecordResponse> getAttendanceRecords(Long sessionId) {
        AttendanceSession session = getSessionEntity(sessionId);
        List<Student> students = studentRepository.findStudentsByClassId(session.getClassId());
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);

        Map<Long, AttendanceRecord> recordMap = new HashMap<>();
        for (AttendanceRecord r : records) {
            recordMap.put(r.getStudentId(), r);
        }

        return students.stream().map(student -> {
            AttendanceRecord record = recordMap.get(student.getId());
            String status = computeAttendanceStatus(record, session);

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

    private String computeAttendanceStatus(AttendanceRecord record, AttendanceSession session) {
        if (record == null) {
            return "ABSENT";
        }
        boolean hasCheckIn = record.getCheckInAt() != null;
        boolean hasCheckOut = record.getCheckOutAt() != null;

        if (hasCheckIn && hasCheckOut) return "PRESENT";
        if (hasCheckIn && !hasCheckOut) {
            // If check-out is still open or not opened, it's provisional
            if (session.getCheckOutStatus() == StageStatus.CLOSED) {
                return "MISSING_CHECK_OUT";
            }
            return "MISSING_CHECK_OUT";
        }
        if (!hasCheckIn && hasCheckOut) return "MISSING_CHECK_IN";
        return "ABSENT";
    }

    private SessionResponse toSessionResponse(AttendanceSession session) {
        ClassEntity classEntity = classRepository.findById(session.getClassId()).orElse(null);
        long totalStudents = classStudentRepository.countByClassId(session.getClassId());
        long checkedIn = recordRepository.countCheckedIn(session.getId());
        long checkedOut = recordRepository.countCheckedOut(session.getId());

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
                session.getCreatedAt()
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
