package com.example.attendance.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<AttendanceRecord> findBySessionId(Long sessionId);
    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query("SELECT COUNT(r) FROM AttendanceRecord r WHERE r.sessionId = :sessionId AND r.checkInAt IS NOT NULL")
    long countCheckedIn(Long sessionId);

    @Query("SELECT COUNT(r) FROM AttendanceRecord r WHERE r.sessionId = :sessionId AND r.checkOutAt IS NOT NULL")
    long countCheckedOut(Long sessionId);

    @Modifying
    @Query("DELETE FROM AttendanceRecord r WHERE r.sessionId = :sessionId")
    void deleteBySessionId(Long sessionId);

    @Modifying
    @Query("DELETE FROM AttendanceRecord r WHERE r.studentId = :studentId")
    void deleteByStudentId(Long studentId);
}
