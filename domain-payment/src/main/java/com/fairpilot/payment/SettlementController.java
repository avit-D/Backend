package com.fairpilot.payment;

import com.fairpilot.core.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /** 정산 생성 (ACCOUNTANT / EXPO_ADMIN 전용) */
    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'EXPO_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<Settlement> create(
            @PathVariable Long exhibitionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ApiResponse.ok(settlementService.create(exhibitionId, periodStart, periodEnd));
    }

    /** 정산 목록 조회 (ACCOUNTANT / EXPO_ADMIN 전용) */
    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'EXPO_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<List<Settlement>> list(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(settlementService.findAll(exhibitionId));
    }

    /** 엑셀 다운로드 (ACCOUNTANT / EXPO_ADMIN 전용) */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'EXPO_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<byte[]> export(@PathVariable Long exhibitionId) {
        byte[] excel = settlementService.exportExcel(exhibitionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=settlement_" + exhibitionId + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}