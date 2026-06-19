package com.fairpilot.tracking.stats.dto;

import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.stats.domain.StatCongestionHourly;
import com.fairpilot.tracking.stats.domain.StatVisitPoint;

import java.time.LocalDate;

public class StatDtos {

    /** 부스/세션별 시간대 방문 통계 응답 (v2.4 스키마 대응). */
    public record PointStatResponse(ScanPointType scanPointType, Long scanPointId,
                                    LocalDate statDate, byte statHour,
                                    int visitCount, int visitorCount,
                                    int uniqueAttendee, int avgDwellSec) {
        public static PointStatResponse from(StatVisitPoint s) {
            return new PointStatResponse(
                    s.getScanPointType(), s.getScanPointId(),
                    s.getStatDate(), s.getStatHour(),
                    s.getVisitCount(), s.getVisitorCount(),
                    s.getUniqueAttendee(), s.getAvgDwellSec());
        }
    }

    /** 박람회 전체 시간대 혼잡도 셀 (v2.4: exhibition 단위, 지점별 아님). */
    public record HeatmapCell(LocalDate statDate, byte statHour, int headCount) {
        public static HeatmapCell from(StatCongestionHourly s) {
            return new HeatmapCell(s.getStatDate(), s.getStatHour(), s.getHeadCount());
        }
    }

    /** 동선 흐름 전이행렬 간선 (from → to, 인원 가중). */
    public record FlowEdge(ScanPointType fromType, Long fromPointId,
                           ScanPointType toType, Long toPointId, int people) {}

    public record RebuildResult(int pointRows, int hourlyRows) {}

    /** MyBatis 조회 결과: 상위 구간 체류 평균 (평균의 함정 방지). */
    public record DwellAvgResult(long sumDwellSec, int dwellCount, long avgDwellSec) {}
}
