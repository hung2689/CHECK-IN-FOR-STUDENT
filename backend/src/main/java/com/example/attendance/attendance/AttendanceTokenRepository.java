package com.example.attendance.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceTokenRepository extends JpaRepository<AttendanceToken, Long> {
    Optional<AttendanceToken> findByTokenValue(String tokenValue);
    List<AttendanceToken> findBySessionIdAndStage(Long sessionId, AttendanceStage stage);

    @Modifying
    @Query("UPDATE AttendanceToken t SET t.revokedAt = :now WHERE t.sessionId = :sessionId AND t.stage = :stage AND t.revokedAt IS NULL")
    void revokeTokensBySessionAndStage(Long sessionId, AttendanceStage stage, Instant now);

    @Modifying
    @Query("DELETE FROM AttendanceToken t WHERE t.sessionId = :sessionId")
    void deleteBySessionId(Long sessionId);
}
