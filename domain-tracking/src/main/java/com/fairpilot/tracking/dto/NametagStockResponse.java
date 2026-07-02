package com.fairpilot.tracking.dto;

import com.fairpilot.tracking.domain.NameTag;

/** 네임태그 재고 단건 */
public record NametagStockResponse(
        Long id,
        String token,
        String status,
        Long attendeeId,
        Long issuedByUserId
) {
    public static NametagStockResponse of(NameTag t) {
        return new NametagStockResponse(
                t.getId(), t.getToken(), t.getStatus().name(),
                t.getAttendeeId(), t.getIssuedByUserId()
        );
    }
}
