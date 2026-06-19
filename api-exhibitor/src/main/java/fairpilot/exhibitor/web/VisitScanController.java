package fairpilot.exhibitor.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.tracking.dto.ScanRequest;
import com.fairpilot.tracking.dto.ScanResult;
import com.fairpilot.tracking.service.ScanProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 부스/세션 셀프 스캔 API (개발자 4번). EXHIBITOR 권한·부스 소유 검증은 2번 보안 모듈과 연동.
 */
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitScanController {

    private final ScanProcessingService scanProcessingService;

    /** 네임태그 QR 셀프 스캔. scanType 미지정 시 서버가 ENTRY/EXIT 자동 판정. */
    @PostMapping("/scan")
    public ApiResponse<ScanResult> scan(@RequestHeader("X-User-Id") Long userId,
                                        @Valid @RequestBody ScanRequest req) {
        return ApiResponse.ok(scanProcessingService.scan(req, userId));
    }

    /** 관리자 수동 종료(미종결 체류). */
    @PostMapping("/open/{dwellId}/close")
    public ApiResponse<Void> closeOpen(@PathVariable Long dwellId) {
        scanProcessingService.manualClose(dwellId);
        return ApiResponse.ok(null);
    }
}
