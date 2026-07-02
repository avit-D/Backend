package com.fairpilot.exhibition;

import java.math.BigDecimal;

public record AdSlotResponse(
        Long id,
        Long exhibitionId,
        String placement,
        BigDecimal basePrice,
        String status
) {
    public static AdSlotResponse from(AdSlot s) {
        return new AdSlotResponse(s.getId(), s.getExhibitionId(),
                s.getPlacement(), s.getBasePrice(), s.getStatus().name());
    }
}
