package com.fairpilot.exhibition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdRequest(

        @NotNull
        Long adSlotId,

        @NotBlank
        String advertiserName,

        Long exhibitorId,

        @NotBlank
        String title,

        String imageUrl,

        String linkUrl,

        @NotNull
        LocalDateTime startAt,

        @NotNull
        LocalDateTime endAt,

        @NotNull
        BigDecimal price
) {}