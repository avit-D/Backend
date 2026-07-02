package com.fairpilot.expoadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.stats.dto.StatDtos.FlowEdge;
import com.fairpilot.tracking.stats.dto.StatDtos.HeatmapCell;
import com.fairpilot.tracking.stats.dto.StatDtos.PointStatResponse;
import com.fairpilot.tracking.stats.dto.StatDtos.RebuildResult;
import com.fairpilot.tracking.stats.service.StatsBatchService;
import com.fairpilot.tracking.stats.service.StatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 통계 API (개발자 4번, v2.4). */
@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsBatchService statsBatchService;
    private final StatsQueryService statsQueryService;

    @PreAuthorize("hasRole('EXPO_ADMIN')")
    @PostMapping("/rebuild")
    public ApiResponse<RebuildResult> rebuild(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(statsBatchService.rebuild(exhibitionId));
    }

    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'PLATFORM_ADMIN')")
    @GetMapping("/booths")
    public ApiResponse<List<PointStatResponse>> boothStats(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(statsQueryService.pointStats(exhibitionId, ScanPointType.BOOTH));
    }

    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'PLATFORM_ADMIN')")
    @GetMapping("/sessions")
    public ApiResponse<List<PointStatResponse>> sessionStats(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(statsQueryService.pointStats(exhibitionId, ScanPointType.SESSION));
    }

    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'PLATFORM_ADMIN')")
    @GetMapping("/heatmap")
    public ApiResponse<List<HeatmapCell>> heatmap(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(statsQueryService.heatmap(exhibitionId));
    }

    /** 동선 흐름/전이행렬. */
    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'PLATFORM_ADMIN')")
    @GetMapping("/flow")
    public ApiResponse<List<FlowEdge>> flow(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(statsQueryService.flow(exhibitionId));
    }
}
