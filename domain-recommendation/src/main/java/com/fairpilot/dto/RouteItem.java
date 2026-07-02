package com.fairpilot.dto;

public record RouteItem(
        int order,
        Long boothId,
        String name,
        int posX,
        int posY,
        String congestionLevel,
        String reason
) {}
