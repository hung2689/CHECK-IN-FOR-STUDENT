package com.example.attendance.attendance;

/**
 * Payload for approving, rejecting, or marking review attempts.
 */
public record ReviewActionRequest(
    String reason,
    String note
) {}
