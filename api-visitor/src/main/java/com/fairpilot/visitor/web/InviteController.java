package com.fairpilot.visitor.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.invite.AcceptInviteRequest;
import com.fairpilot.core.invite.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    /**
     * 초대 수락 — 비밀번호 설정 + 계정 활성화
     * 초대받은 사람은 JWT 없음 → SecurityConfig에서 permitAll 처리됨
     */
    @PostMapping("/accept")
    public ApiResponse<Void> accept(@RequestBody @Valid AcceptInviteRequest req) {
        inviteService.accept(req);
        return ApiResponse.ok(null);
    }
}