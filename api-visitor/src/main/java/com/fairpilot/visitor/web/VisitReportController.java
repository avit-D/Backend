package com.fairpilot.visitor.web;

import com.fairpilot.core.auth.CurrentUser;
import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.tracking.dto.VisitReportResponse;
import com.fairpilot.tracking.service.VisitReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 참관객 본인 동선 사후 리포트 API.
 * GET /api/my/report?exhibitionId={id}
 */
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class VisitReportController {

    private final VisitReportService visitReportService;

    @GetMapping("/report")
    public ApiResponse<VisitReportResponse> report(
            @CurrentUser Long userId,
            @RequestParam Long exhibitionId) {
        return ApiResponse.ok(visitReportService.report(exhibitionId, userId));
    }
}
