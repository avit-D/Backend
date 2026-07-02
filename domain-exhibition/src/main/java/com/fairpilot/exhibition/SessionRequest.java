package com.fairpilot.exhibition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SessionRequest(

        Long hostExhibitorId,

        @NotBlank
        String title,

        String description,

        String location,

        @NotNull
        LocalDateTime startAt,

        @NotNull
        LocalDateTime endAt,

        @NotNull
        Integer capacity
) {}