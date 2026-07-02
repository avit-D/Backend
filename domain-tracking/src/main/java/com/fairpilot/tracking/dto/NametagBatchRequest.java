package com.fairpilot.tracking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** POST /api/exhibitions/{id}/nametags/batch */
public record NametagBatchRequest(
        @NotNull @Min(1) @Max(1000) Integer count
) {}
