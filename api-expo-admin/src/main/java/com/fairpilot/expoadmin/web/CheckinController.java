package com.fairpilot.expoadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.tracking.dto.*;
import com.fairpilot.tracking.service.CheckinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 체크인 처리 API (기획안 6.4 - EXPO_ADMIN / STAFF 전용).
 */
@PreAuthorize("hasAnyRole('EXPO_ADMIN', 'STAFF')")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/verify")
    public ApiResponse<TicketVerifyResponse> verify(
            @RequestParam Long exhibitionId,
            @RequestParam String ticketQrToken) {
        return ApiResponse.ok(checkinService.verifyTicket(exhibitionId, ticketQrToken));
    }

    @PostMapping("/nametag")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResultResponse> bindNametag(
            @Valid @RequestBody BindNametagRequest request,
            @RequestHeader("X-User-Id") Long staffUserId) {
        return ApiResponse.ok(checkinService.bindNametag(request, staffUserId));
    }

    @PostMapping("/nametag/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResultResponse> manualBind(
            @Valid @RequestBody ManualBindRequest request,
            @RequestHeader("X-User-Id") Long staffUserId) {
        return ApiResponse.ok(checkinService.manualBind(request, staffUserId));
    }

    @PostMapping("/nametag/reissue")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResultResponse> reissue(
            @RequestParam Long exhibitionId,
            @RequestParam Long attendeeId,
            @RequestParam String newNametagToken,
            @RequestParam(required = false) String memo,
            @RequestHeader("X-User-Id") Long staffUserId) {
        return ApiResponse.ok(
                checkinService.reissueNametag(exhibitionId, attendeeId, newNametagToken, staffUserId, memo));
    }

    @PostMapping("/walk-in")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResultResponse> walkIn(
            @Valid @RequestBody WalkInRequest request,
            @RequestHeader("X-User-Id") Long staffUserId) {
        return ApiResponse.ok(checkinService.walkIn(request, staffUserId));
    }

    @GetMapping("/lookup")
    public ApiResponse<List<TicketVerifyResponse>> lookup(
            @RequestParam Long exhibitionId,
            @RequestParam String query) {
        return ApiResponse.ok(checkinService.lookupAttendees(exhibitionId, query));
    }

    @GetMapping("/reservations/{reservationId}/status")
    public ApiResponse<TeamCheckinStatusResponse> teamStatus(
            @PathVariable Long reservationId) {
        return ApiResponse.ok(checkinService.teamCheckinStatus(reservationId));
    }
}
