package com.example.attendance.attendance;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.DRAFT;

    @Column(name = "check_in_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StageStatus checkInStatus = StageStatus.NOT_OPEN;

    @Column(name = "check_out_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StageStatus checkOutStatus = StageStatus.NOT_OPEN;

    @Column(name = "created_by")
    private Long createdBy;

    // ── Geofence (optional, configured by TA when opening check-in) ──────────
    @Column(name = "geofence_lat")
    private Double geofenceLat;

    @Column(name = "geofence_lng")
    private Double geofenceLng;

    @Column(name = "geofence_radius_m")
    private Integer geofenceRadiusM;

    @Column(name = "gps_enforcement", nullable = false)
    private Boolean gpsEnforcement = false;
    // ─────────────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public StageStatus getCheckInStatus() { return checkInStatus; }
    public void setCheckInStatus(StageStatus checkInStatus) { this.checkInStatus = checkInStatus; }
    public StageStatus getCheckOutStatus() { return checkOutStatus; }
    public void setCheckOutStatus(StageStatus checkOutStatus) { this.checkOutStatus = checkOutStatus; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Double getGeofenceLat() { return geofenceLat; }
    public void setGeofenceLat(Double geofenceLat) { this.geofenceLat = geofenceLat; }
    public Double getGeofenceLng() { return geofenceLng; }
    public void setGeofenceLng(Double geofenceLng) { this.geofenceLng = geofenceLng; }
    public Integer getGeofenceRadiusM() { return geofenceRadiusM; }
    public void setGeofenceRadiusM(Integer geofenceRadiusM) { this.geofenceRadiusM = geofenceRadiusM; }
    public Boolean getGpsEnforcement() { return gpsEnforcement != null && gpsEnforcement; }
    public void setGpsEnforcement(Boolean gpsEnforcement) { this.gpsEnforcement = gpsEnforcement; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

