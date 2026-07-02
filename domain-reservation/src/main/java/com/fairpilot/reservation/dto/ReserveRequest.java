package com.fairpilot.reservation.dto;

import com.fairpilot.reservation.domain.MovementMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 개인/팀 예약 요청 (v2.4). attendees 는 GROUP=대표 1명, INDIVIDUAL=group_size 명. */
public record ReserveRequest(
        @NotNull Long exhibitionId,
        @NotNull Long timeSlotId,
        Long ticketTypeId,
        @NotNull MovementMode movementMode,
        @Min(1) int groupSize,
        boolean useQueue,
        @NotEmpty @Valid List<AttendeeRequest> attendees
) {
    public record AttendeeRequest(
            @NotNull String name,
            String phone,
            String email,
            boolean isGroupLeader
    ) {}
}
