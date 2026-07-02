package com.fairpilot.exhibitor.web;

import com.fairpilot.core.auth.CurrentUser;
import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.exhibition.BoothRepository;
import com.fairpilot.exhibition.SessionRepository;
import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.dto.ScanPointResponse;
import com.fairpilot.tracking.dto.ScanRequest;
import com.fairpilot.tracking.dto.ScanResult;
import com.fairpilot.tracking.service.ScanProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * 부스/세션 셀프 스캔 API (개발자 4번). EXHIBITOR 권한·부스 소유 검증은 2번 보안 모듈과 연동.
 */
@RestController
@RequiredArgsConstructor
public class VisitScanController {

    private final ScanProcessingService scanProcessingService;
    private final BoothRepository boothRepository;
    private final SessionRepository sessionRepository;

    /** 내가 스캔 가능한 부스/세션 목록(§6.5). exhibitorId = JWT userId. */
    @GetMapping("/api/exhibitor/scan-points")
    public ApiResponse<List<ScanPointResponse>> scanPoints(@CurrentUser Long userId) {
        List<ScanPointResponse> booths = boothRepository.findAllByExhibitorId(userId).stream()
                .map(b -> new ScanPointResponse(ScanPointType.BOOTH, b.getId()))
                .toList();
        List<ScanPointResponse> sessions = sessionRepository.findAllByHostExhibitorId(userId).stream()
                .map(s -> new ScanPointResponse(ScanPointType.SESSION, s.getId()))
                .toList();
        return ApiResponse.ok(Stream.concat(booths.stream(), sessions.stream()).toList());
    }

    /** 네임태그 QR 셀프 스캔. scanType 미지정 시 서버가 ENTRY/EXIT 자동 판정. */
    @PostMapping("/api/visits/scan")
    public ApiResponse<ScanResult> scan(@RequestHeader("X-User-Id") Long userId,
                                        @Valid @RequestBody ScanRequest req) {
        return ApiResponse.ok(scanProcessingService.scan(req, userId));
    }

    /** 관리자 수동 종료(미종결 체류). */
    @PostMapping("/api/visits/open/{dwellId}/close")
    public ApiResponse<Void> closeOpen(@PathVariable Long dwellId) {
        scanProcessingService.manualClose(dwellId);
        return ApiResponse.ok(null);
    }
}
