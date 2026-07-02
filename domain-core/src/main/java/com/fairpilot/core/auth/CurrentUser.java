package com.fairpilot.core.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * 컨트롤러 파라미터에 붙이면 현재 로그인한 userId(Long)를 자동 주입.
 * 예: public ApiResponse<?> myInfo(@CurrentUser Long userId)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}