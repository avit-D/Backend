package com.fairpilot.platformadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.invite.InviteRequest;
import com.fairpilot.core.invite.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    /**
     * PLATFORM_ADMIN → EXPO_ADMIN 초대
     */
    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Void> invite(@RequestBody @Valid InviteRequest req) {
        inviteService.invite(req);
        return ApiResponse.ok(null);
    }
}