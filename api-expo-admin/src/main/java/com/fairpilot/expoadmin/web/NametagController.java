package com.fairpilot.expoadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.tracking.dto.NametagBatchRequest;
import com.fairpilot.tracking.dto.NametagStockResponse;
import com.fairpilot.tracking.service.CheckinService;
import com.fairpilot.tracking.service.NametagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 네임태그 재고 관리 API (기획안 §6.4 — EXPO_ADMIN 전용).
 *
 * POST   /api/exhibitions/{id}/nametags/batch   — 배치 생성
 * GET    /api/exhibitions/{id}/nametags          — 재고 목록 (status 필터)
 * GET    /api/exhibitions/{id}/nametags/summary  — 재고 요약 (건수)
 * POST   /api/checkin/nametags/{tagId}/revoke     — 회수(REVOKED)
 */
@PreAuthorize("hasRole('EXPO_ADMIN')")
@RestController
@RequiredArgsConstructor
public class NametagController {

    private final NametagService nametagService;
    private final CheckinService checkinService;

    @PostMapping("/api/exhibitions/{exhibitionId}/nametags/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<NametagStockResponse>> batchCreate(
            @PathVariable Long exhibitionId,
            @Valid @RequestBody NametagBatchRequest request) {
        return ApiResponse.ok(nametagService.batchCreate(exhibitionId, request));
    }

    @GetMapping("/api/exhibitions/{exhibitionId}/nametags")
    public ApiResponse<List<NametagStockResponse>> getStock(
            @PathVariable Long exhibitionId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(nametagService.getStock(exhibitionId, status));
    }

    @GetMapping("/api/exhibitions/{exhibitionId}/nametags/summary")
    public ApiResponse<NametagService.StockSummary> getStockSummary(
            @PathVariable Long exhibitionId) {
        return ApiResponse.ok(nametagService.getStockSummary(exhibitionId));
    }

    @PostMapping("/api/checkin/nametags/{nameTagId}/revoke")
    public ApiResponse<Void> revokeNametag(
            @PathVariable Long nameTagId,
            @RequestHeader("X-User-Id") Long staffUserId) {
        checkinService.revokeNametag(nameTagId, staffUserId);
        return ApiResponse.ok(null);
    }
}
