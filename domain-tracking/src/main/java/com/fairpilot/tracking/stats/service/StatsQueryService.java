package com.fairpilot.tracking.stats.service;

import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.domain.ScanType;
import com.fairpilot.tracking.domain.VisitLog;
import com.fairpilot.tracking.stats.dto.StatDtos.DwellAvgResult;
import com.fairpilot.tracking.stats.dto.StatDtos.FlowEdge;
import com.fairpilot.tracking.stats.dto.StatDtos.HeatmapCell;
import com.fairpilot.tracking.stats.dto.StatDtos.PointStatResponse;
import com.fairpilot.tracking.stats.repository.StatCongestionHourlyRepository;
import com.fairpilot.tracking.stats.repository.StatMapper;
import com.fairpilot.tracking.stats.repository.StatVisitPointRepository;
import com.fairpilot.tracking.repository.VisitLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** 정형화된 통계 조회 + 동선 흐름(전이행렬) API (1번 리포트 화면용). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsQueryService {

    private final StatVisitPointRepository statVisitPointRepository;
    private final StatCongestionHourlyRepository statCongestionHourlyRepository;
    private final VisitLogRepository visitLogRepository;
    private final StatMapper statMapper;

    /** 일별 상위 구간 체류 평균 — SUM(sum_dwell_sec)/SUM(dwell_count) */
    public DwellAvgResult dailyDwellAvg(Long exhibitionId, LocalDate statDate) {
        return statMapper.dailyDwellAvg(exhibitionId, statDate);
    }

    /** 박람회 전체 기간 체류 평균 */
    public DwellAvgResult totalDwellAvg(Long exhibitionId) {
        return statMapper.totalDwellAvg(exhibitionId);
    }

    public List<PointStatResponse> pointStats(Long exhibitionId, ScanPointType typeFilter) {
        return statVisitPointRepository.findByExhibitionId(exhibitionId).stream()
                .filter(s -> typeFilter == null || s.getScanPointType() == typeFilter)
                .map(PointStatResponse::from).toList();
    }

    public List<HeatmapCell> heatmap(Long exhibitionId) {
        return statCongestionHourlyRepository.findByExhibitionId(exhibitionId).stream()
                .map(HeatmapCell::from).toList();
    }

    /** 동선 흐름: attendee별 시간순 ENTRY 시퀀스에서 인접 지점쌍 전이를 인원(head_count)으로 가중 집계. */
    public List<FlowEdge> flow(Long exhibitionId) {
        List<VisitLog> logs = visitLogRepository.findByExhibitionIdOrderByAttendeeIdAscScannedAtAsc(exhibitionId);
        record Edge(ScanPointType ft, Long fp, ScanPointType tt, Long tp) {}
        Map<Edge, Integer> matrix = new HashMap<>();

        Long curAttendee = null;
        VisitLog prev = null;
        for (VisitLog l : logs) {
            if (l.getScanType() != ScanType.ENTRY || l.getScanPointType() == ScanPointType.GATE) continue;
            if (!l.getAttendeeId().equals(curAttendee)) {
                curAttendee = l.getAttendeeId();
                prev = null;
            }
            if (prev != null) {
                Edge e = new Edge(prev.getScanPointType(), prev.getScanPointId(),
                                  l.getScanPointType(), l.getScanPointId());
                matrix.merge(e, l.getHeadCount(), Integer::sum);
            }
            prev = l;
        }

        return matrix.entrySet().stream()
                .map(en -> new FlowEdge(
                        en.getKey().ft(), en.getKey().fp(),
                        en.getKey().tt(), en.getKey().tp(),
                        en.getValue()))
                .toList();
    }
}
