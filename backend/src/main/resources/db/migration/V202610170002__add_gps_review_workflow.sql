-- V202610170002: Add GPS geofencing config to sessions + admin review workflow
-- This migration is backward-compatible:
--   * All new columns on attendance_sessions are nullable (old sessions keep working)
--   * attendance_review_attempts is a NEW table, does not affect existing attendance_records

-- 1. Add geofence configuration to attendance_sessions
ALTER TABLE attendance_sessions
    ADD COLUMN IF NOT EXISTS geofence_lat         DOUBLE PRECISION,   -- center latitude
    ADD COLUMN IF NOT EXISTS geofence_lng         DOUBLE PRECISION,   -- center longitude
    ADD COLUMN IF NOT EXISTS geofence_radius_m    INTEGER,            -- allowed radius in metres (e.g. 150)
    ADD COLUMN IF NOT EXISTS gps_enforcement      BOOLEAN NOT NULL DEFAULT FALSE; -- disabled unless TA sets it up

-- 2. Attendance review attempts table
--    Stores every student submission that cannot be automatically confirmed.
--    Does NOT replace attendance_records; an approved attempt updates attendance_records.
CREATE TABLE IF NOT EXISTS attendance_review_attempts (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL REFERENCES attendance_sessions(id) ON DELETE CASCADE,
    student_id          BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    stage               VARCHAR(20) NOT NULL,                   -- CHECK_IN | CHECK_OUT
    submitted_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- GPS data (nullable – student may not have location)
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    gps_accuracy_m      DOUBLE PRECISION,                       -- reported accuracy in metres
    distance_from_school DOUBLE PRECISION,                      -- calculated server-side (metres); null if location unavailable
    gps_result          VARCHAR(30),                            -- VERIFIED | OUTSIDE_GEOFENCE | POOR_ACCURACY | BOUNDARY | NO_LOCATION | N/A
    gps_reason          TEXT,                                   -- human-readable explanation stored server-side

    -- Review workflow state
    review_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by         BIGINT REFERENCES users(id),
    reviewed_at         TIMESTAMP WITH TIME ZONE,
    admin_decision      VARCHAR(20),                            -- APPROVED | REJECTED
    admin_note          TEXT,

    -- Prevent duplicate pending attempts for the same student/session/stage
    -- Allow multiple rows (e.g. if first was rejected and student tries again), but
    -- only one PENDING_REVIEW row per (session, student, stage) at a time (enforced in service).
    CONSTRAINT chk_stage       CHECK (stage IN ('CHECK_IN', 'CHECK_OUT')),
    CONSTRAINT chk_review_status CHECK (review_status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'REVIEWED')),
    CONSTRAINT chk_gps_result  CHECK (gps_result IN ('VERIFIED', 'OUTSIDE_GEOFENCE', 'POOR_ACCURACY', 'BOUNDARY', 'NO_LOCATION', 'DISABLED') OR gps_result IS NULL),
    CONSTRAINT chk_admin_decision CHECK (admin_decision IN ('APPROVED', 'REJECTED') OR admin_decision IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_review_attempts_session_id    ON attendance_review_attempts(session_id);
CREATE INDEX IF NOT EXISTS idx_review_attempts_student_id    ON attendance_review_attempts(student_id);
CREATE INDEX IF NOT EXISTS idx_review_attempts_review_status ON attendance_review_attempts(review_status);
CREATE INDEX IF NOT EXISTS idx_review_attempts_stage         ON attendance_review_attempts(stage);
