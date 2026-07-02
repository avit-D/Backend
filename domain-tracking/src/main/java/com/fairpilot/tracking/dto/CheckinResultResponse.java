package com.fairpilot.tracking.dto;

/** 체크인 처리 결과 (바인딩·워크인·재발급 공통) */
public record CheckinResultResponse(
        Long checkinLogId,
        Long attendeeId,
        String attendeeName,
        Long nameTagId,
        String nameTagToken,
        String checkinMethod,
        boolean gateEntryCreated
) {}
