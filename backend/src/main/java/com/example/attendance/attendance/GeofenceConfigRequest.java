package com.example.attendance.attendance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GeofenceConfigRequest(
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    Double geofenceLat,

    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    Double geofenceLng,

    @Min(value = 10, message = "Radius must be at least 10 metres")
    @Max(value = 10000, message = "Radius cannot exceed 10000 metres")
    Integer geofenceRadiusM,

    Boolean gpsEnforcement
) {}
