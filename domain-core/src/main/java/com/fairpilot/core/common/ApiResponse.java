package com.fairpilot.core.common;

/**
 * 공통 응답 Wrapper (기획안 6장): { success, data, message }.
 * (전역 규격은 2번 소유. 4번 모듈 단독 실행/테스트를 위해 최소 정의 포함)
 */
public record ApiResponse<T>(boolean success, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
