package com.fairpilot.platformadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.reservation.repository.ReservationAttendeeRepository;
import com.fairpilot.reservation.repository.ReservationRepository;
import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.stats.dto.StatDtos.*;
import com.fairpilot.tracking.stats.service.StatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 통합 통계 API (PLATFORM_ADMIN 전용).
 *
 * GET /api/admin/exhibitions/{id}/stats/points   — 지점별 방문 통계
 * GET /api/admin/exhibitions/{id}/stats/heatmap  — 시간대 혼잡도 히트맵
 * GET /api/admin/exhibitions/{id}/stats/flow     — 동선 흐름 전이행렬
 * GET /api/admin/exhibitions/{id}/stats/dwell    — 체류 평균 (일별 / 전체)
 * GET /api/admin/exhibitions/{id}/stats/summary  — 예약·참석자 요약
 */
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RestController
@RequestMapping("/api/admin/exhibitions/{exhibitionId}/stats")
@RequiredArgsConstructor
public class PlatformAdminStatsController {

    private final StatsQueryService statsQueryService;
    private final ReservationRepository reservationRepository;
    private final ReservationAttendeeRepository attendeeRepository;

    /** 지점별(부스/세션) 방문 통계 — scanPointType 필터 선택 */
    @GetMapping("/points")
    public ApiResponse<List<PointStatResponse>> points(
            @PathVariable Long exhibitionId,
            @RequestParam(required = false) ScanPointType type) {
        return ApiResponse.ok(statsQueryService.pointStats(exhibitionId, type));
    }

    /** 시간대 혼잡도 히트맵 */
    @GetMapping("/heatmap")
    public ApiResponse<List<HeatmapCell>> heatmap(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(statsQueryService.heatmap(exhibitionId));
    }

    /** 동선 흐름 전이행렬 */
    @GetMapping("/flow")
    public ApiResponse<List<FlowEdge>> flow(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(statsQueryService.flow(exhibitionId));
    }

    /** 체류 평균 — statDate 지정 시 일별, 미지정 시 전체 기간 */
    @GetMapping("/dwell")
    public ApiResponse<DwellAvgResult> dwell(
            @PathVariable Long exhibitionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate) {
        if (statDate != null) {
            return ApiResponse.ok(statsQueryService.dailyDwellAvg(exhibitionId, statDate));
        }
        return ApiResponse.ok(statsQueryService.totalDwellAvg(exhibitionId));
    }

    /** 예약·참석자 요약 — 전체 예약 수, 확정 수, 총 참석자 수 */
    @GetMapping("/summary")
    public ApiResponse<ExhibitionSummary> summary(@PathVariable Long exhibitionId) {
        long totalReservations = reservationRepository.findByExhibitionId(exhibitionId).size();
        long totalAttendees = reservationRepository.findByExhibitionId(exhibitionId).stream()
                .mapToLong(r -> attendeeRepository.findByReservationId(r.getId()).size())
                .sum();
        return ApiResponse.ok(new ExhibitionSummary(exhibitionId, totalReservations, totalAttendees));
    }

    public record ExhibitionSummary(Long exhibitionId, long totalReservations, long totalAttendees) {}
}
