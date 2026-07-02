package com.fairpilot.exhibition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BoothRequest(

        @NotNull
        Long exhibitorId,

        Long categoryId,

        @NotBlank
        String name,

        String description,

        String tags,

        Integer posX,

        Integer posY,

        Integer floor
) {}