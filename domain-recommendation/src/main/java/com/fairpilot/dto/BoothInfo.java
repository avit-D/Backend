package com.fairpilot.dto;

public record BoothInfo(
        Long id,
        String name,
        String description,
        String tags,
        Integer posX,
        Integer posY,
        String congestionLevel  // "여유" | "보통" | "혼잡"
) {}
