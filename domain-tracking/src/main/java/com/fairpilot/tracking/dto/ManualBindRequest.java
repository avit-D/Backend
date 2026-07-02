package com.fairpilot.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** POST /api/checkin/nametag/manual — attendeeId로 직접 바인딩 */
public record ManualBindRequest(
        @NotNull Long exhibitionId,
        @NotNull Long attendeeId,
        @NotBlank String nametagToken,
        String memo
) {}
