package com.fairpilot.exhibition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TicketTypeRequest(

        @NotBlank
        String name,

        @NotNull
        BigDecimal price,

        Integer quota
) {}