package fairpilot.expoadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.tracking.dto.*;
import com.fairpilot.tracking.service.CheckinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 체크인 처리 API (기획안 §6.4 — EXPO_ADMIN / STAFF 전용).
 *
 * POST /api/checkin/verify             — QR 티켓 검증
 * POST /api/checkin/nametag            — QR 티켓 + 네임태그 바인딩
 * POST /api/checkin/nametag/manual     — 참석자 ID로 수동 바인딩
 * POST /api/checkin/nametag/reissue    — 네임태그 재발급
 * POST /api/checkin/walk-in            — 현장 워크인
 * GET  /api/checkin/lookup             — 참석자 검색 (이름/전화번호)
 * GET  /api/checkin/reservations/{id}/status — 팀 체크인 현황
 */
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /**
     * QR 티켓 검증 — 바인딩 전 참석자/예약 정보 확인.
     */
    @PostMapping("/verify")
    public ApiResponse<TicketVerifyResponse> verify(
            @RequestParam Long exhibitionId,
            @RequestParam String ticketQrToken) {
        return ApiResponse.ok(checkinService.verifyTicket(exhibitionId, ticketQrToken));
    }

    /**
     * QR 티켓 → 네임태그 바인딩.
     * 이미 바인딩된 참석자라면 자동으로 REISSUE 분기.
     */
    @PostMapping("/nametag")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResultResponse> bindNametag(
            @Valid @RequestBody BindNametagRequest request,
            @RequestHeader("X-User-Id") Long staffUserId) {
        return ApiResponse.ok(checkinService.bindNametag(request, staffUserId));
    }

    /**
     * 수동 검색 바인딩 — 스태프가 참석자 ID를 직접 지정.
     */
    @PostMapping("/nametag/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResultResponse> manualBind(
            @Valid @RequestBody ManualBindRequest request,
            @RequestHeader("X-User-Id") Long staffUserId) {
        return ApiResponse.ok(checkinService.manualBind(request, staffUserId));
    }

    /**
     * 네임태그 재발급 — 기존 ISSUED 회수 + 새 AVAILABLE 태그 발급.
     * GATE ENTRY 미기록.
     */
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

    /**
     * 현장 워크인 — 사전 예약 없이 현장 즉시 등록 + 네임태그 배포 + GATE ENTRY.
     */
    @PostMapping("/walk-in")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResultResponse> walkIn(
            @Valid @RequestBody WalkInRequest request,
            @RequestHeader("X-User-Id") Long staffUserId) {
        return ApiResponse.ok(checkinService.walkIn(request, staffUserId));
    }

    /**
     * 참석자 검색 — 이름 또는 전화번호 부분 일치.
     */
    @GetMapping("/lookup")
    public ApiResponse<List<TicketVerifyResponse>> lookup(
            @RequestParam Long exhibitionId,
            @RequestParam String query) {
        return ApiResponse.ok(checkinService.lookupAttendees(exhibitionId, query));
    }

    /**
     * 팀 체크인 현황 — 예약 단위 전체 참석자 상태 조회.
     */
    @GetMapping("/reservations/{reservationId}/status")
    public ApiResponse<TeamCheckinStatusResponse> teamStatus(
            @PathVariable Long reservationId) {
        return ApiResponse.ok(checkinService.teamCheckinStatus(reservationId));
    }
}
