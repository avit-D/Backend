package com.fairpilot.exhibition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdSlotRequest(

        @NotNull
        Long exhibitionId,

        @NotBlank
        String placement,

        @NotNull
        BigDecimal basePrice
) {}
