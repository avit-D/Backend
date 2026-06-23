package com.fairpilot.visitor.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.user.AuthService;
import com.fairpilot.core.user.LoginRequest;
import com.fairpilot.core.user.SignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 회원가입 (VISITOR만 가능) */
    @PostMapping("/signup")
    public ApiResponse<Void> signUp(@Valid @RequestBody SignUpRequest req) {
        authService.signUp(req.email(), req.password(), req.name(), req.phone());
        return ApiResponse.ok(null);
    }

    /** 로그인 → AccessToken + RefreshToken 반환 */
    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req.email(), req.password()));
    }

    /** Access Token 재발급 */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@RequestParam String refreshToken) {
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestParam String refreshToken) {
        authService.logout(refreshToken);
        return ApiResponse.ok(null);
    }
}