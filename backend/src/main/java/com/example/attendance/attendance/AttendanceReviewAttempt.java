package com.example.attendance.attendance;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Records every attendance submission that could not be automatically confirmed
 * because of GPS uncertainty, location denial, or out-of-bounds detection.
 * An approved attempt will update the official AttendanceRecord.
 */
@Entity
@Table(name = "attendance_review_attempts")
public class AttendanceReviewAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AttendanceStage stage;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    // GPS fields (all nullable)
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "gps_accuracy_m")
    private Double gpsAccuracyM;

    @Column(name = "distance_from_school")
    private Double distanceFromSchool;

    /** One of: VERIFIED, OUTSIDE_GEOFENCE, POOR_ACCURACY, BOUNDARY, NO_LOCATION, DISABLED */
    @Column(name = "gps_result", length = 30)
    private String gpsResult;

    @Column(name = "gps_reason", columnDefinition = "TEXT")
    private String gpsReason;

    // Review workflow
    /** One of: PENDING_REVIEW, APPROVED, REJECTED, REVIEWED */
    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = "PENDING_REVIEW";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** One of: APPROVED, REJECTED (set when admin acts) */
    @Column(name = "admin_decision", length = 20)
    private String adminDecision;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) submittedAt = Instant.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public AttendanceStage getStage() { return stage; }
    public void setStage(AttendanceStage stage) { this.stage = stage; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getGpsAccuracyM() { return gpsAccuracyM; }
    public void setGpsAccuracyM(Double gpsAccuracyM) { this.gpsAccuracyM = gpsAccuracyM; }
    public Double getDistanceFromSchool() { return distanceFromSchool; }
    public void setDistanceFromSchool(Double distanceFromSchool) { this.distanceFromSchool = distanceFromSchool; }
    public String getGpsResult() { return gpsResult; }
    public void setGpsResult(String gpsResult) { this.gpsResult = gpsResult; }
    public String getGpsReason() { return gpsReason; }
    public void setGpsReason(String gpsReason) { this.gpsReason = gpsReason; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getAdminDecision() { return adminDecision; }
    public void setAdminDecision(String adminDecision) { this.adminDecision = adminDecision; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}
