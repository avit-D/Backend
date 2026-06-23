package com.fairpilot.tracking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** POST /api/checkin/walk-in — 현장 워크인 체크인 */
public record WalkInRequest(
        @NotNull Long exhibitionId,
        @NotBlank String leaderName,
        @NotBlank String leaderPhone,
        @Min(1) int groupSize,
        @NotBlank String nametagToken,
        String memo
) {}
