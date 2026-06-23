package com.fairpilot.tracking.domain;

/** 체크인 방식 (기획안 §6.4) */
public enum CheckinMethod {
    /** 참석자가 QR 셀프 스캔 */
    QR_SELF,
    /** 스태프가 참석자 이름/연락처로 검색 후 수동 바인딩 */
    MANUAL_SEARCH,
    /** 현장 스태프 직접 입력 (예약 없는 현장 등록) */
    ONSITE_MANUAL,
    /** 사전 예약 없이 현장 워크인 */
    WALK_IN,
    /** 네임태그 재발급 (기존 ISSUED → REVOKED 후 새 태그 ISSUED) */
    REISSUE
}
