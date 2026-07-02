package com.fairpilot.reservation.dto;

import jakarta.validation.constraints.NotNull;

/** PATCH /api/reservations/{id}/attendees — 참석자 정보 수정 단건 */
public record AttendeeUpdateRequest(
        @NotNull Long attendeeId,
        String name,
        String phone,
        String email
) {}
