package com.fairpilot.exhibition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExhibitionRequest(

        @NotBlank
        String title,

        @NotBlank
        String slug,

        String venue,

        String address,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {}