package com.fairpilot.expoadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.invite.InviteRequest;
import com.fairpilot.core.invite.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expo/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    /**
     * EXPO_ADMIN → STAFF / EXHIBITOR / ACCOUNTANT 초대
     */
    @PostMapping
    @PreAuthorize("hasRole('EXPO_ADMIN')")
    public ApiResponse<Void> invite(@RequestBody @Valid InviteRequest req) {
        inviteService.invite(req);
        return ApiResponse.ok(null);
    }
}