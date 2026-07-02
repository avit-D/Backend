package com.fairpilot.core.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 형식/값 오류"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 실패"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한 또는 테넌트 불일치"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스 없음"),
    SLOT_SOLD_OUT(HttpStatus.CONFLICT, "잔여 정원이 없습니다"),
    DUPLICATE_RESERVATION(HttpStatus.CONFLICT, "이미 예약된 슬롯입니다"),
    CONFLICT(HttpStatus.CONFLICT, "상태 충돌"),
    HOLD_EXPIRED(HttpStatus.GONE, "임시 점유 또는 대기 토큰이 만료되었습니다"),
    QUEUE_NOT_ALLOWED(HttpStatus.TOO_MANY_REQUESTS, "대기열 입장 순번이 아닙니다"),
    UPSTREAM_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "외부 연동 실패");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}
