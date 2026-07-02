package com.fairpilot.tracking.dto;

import java.util.List;

/** GET /api/checkin/reservations/{id}/status 응답 */
public record TeamCheckinStatusResponse(
        Long reservationId,
        String reservationStatus,
        int groupSize,
        int checkedInCount,
        List<AttendeeCheckinStatusResponse> attendees
) {}
