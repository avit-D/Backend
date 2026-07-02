package com.fairpilot.tracking.dto;

public record ScanResult(
        Long attendeeId, String movementMode, int headCount,
        String scanType, boolean autoClosedPrevious, String message
) {}
