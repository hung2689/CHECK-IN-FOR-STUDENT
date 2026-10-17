package com.example.attendance.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceReviewAttemptRepository extends JpaRepository<AttendanceReviewAttempt, Long> {

    List<AttendanceReviewAttempt> findBySessionId(Long sessionId);

    List<AttendanceReviewAttempt> findBySessionIdOrderBySubmittedAtDesc(Long sessionId);

    List<AttendanceReviewAttempt> findBySessionIdAndReviewStatusOrderBySubmittedAtDesc(Long sessionId, String reviewStatus);

    List<AttendanceReviewAttempt> findBySessionIdAndStageOrderBySubmittedAtDesc(Long sessionId, AttendanceStage stage);

    List<AttendanceReviewAttempt> findBySessionIdAndStageAndReviewStatusOrderBySubmittedAtDesc(
            Long sessionId, AttendanceStage stage, String reviewStatus);

    Optional<AttendanceReviewAttempt> findBySessionIdAndStudentIdAndStageAndReviewStatus(
            Long sessionId, Long studentId, AttendanceStage stage, String reviewStatus);

    List<AttendanceReviewAttempt> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    long countBySessionIdAndReviewStatus(Long sessionId, String reviewStatus);

    long countBySessionIdAndGpsResult(Long sessionId, String gpsResult);

    void deleteBySessionId(Long sessionId);

    void deleteByStudentId(Long studentId);

    @Query("SELECT COUNT(a) FROM AttendanceReviewAttempt a " +
           "JOIN AttendanceSession s ON a.sessionId = s.id " +
           "WHERE s.classId = :classId AND a.reviewStatus = 'PENDING_REVIEW'")
    long countPendingByClassId(@Param("classId") Long classId);
}
