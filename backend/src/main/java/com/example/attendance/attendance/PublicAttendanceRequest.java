package com.example.attendance.attendance;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for student attendance submission with optional GPS data.
 */
public record PublicAttendanceRequest(
    @NotBlank(message = "Student code is required")
    String studentCode,

    /** GPS latitude – may be null if browser location unavailable/denied */
    Double latitude,

    /** GPS longitude – may be null if browser location unavailable/denied */
    Double longitude,

    /** Reported GPS accuracy in metres from browser Geolocation API */
    Double gpsAccuracyM,

    /**
     * Reason string when location is not available, e.g.:
     *   "LOCATION_PERMISSION_DENIED" | "LOCATION_UNAVAILABLE" | "LOCATION_TIMEOUT" | null
     */
    String locationUnavailableReason
) {}
