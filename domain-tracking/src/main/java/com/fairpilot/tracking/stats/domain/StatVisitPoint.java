package com.fairpilot.tracking.stats.domain;

import com.fairpilot.tracking.domain.ScanPointType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 부스/세션별 시간대 방문 통계 집계 (v2.7 스키마).
 * UNIQUE: (exhibition_id, scan_point_type, scan_point_id, stat_date, stat_hour)
 * 상위 구간 평균 = SUM(sum_dwell_sec) / SUM(dwell_count) — avg_dwell_sec는 단일 버킷 표시 전용.
 */
@Entity
@Table(name = "stat_visit_point",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_stat_point",
                columnNames = {"exhibition_id", "scan_point_type", "scan_point_id", "stat_date", "stat_hour"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatVisitPoint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_point_type", nullable = false, length = 10)
    private ScanPointType scanPointType;

    @Column(name = "scan_point_id", nullable = false)
    private Long scanPointId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** 집계 기준 시(0-23) */
    @Column(name = "stat_hour", nullable = false)
    private byte statHour;

    /** ENTRY 건수 */
    @Column(name = "visit_count", nullable = false)
    private int visitCount;

    /** head_count 합산 방문 인원 */
    @Column(name = "visitor_count", nullable = false)
    private int visitorCount;

    /** 순방문자(distinct attendee_id) */
    @Column(name = "unique_attendee", nullable = false)
    private int uniqueAttendee;

    /** 마감된 체류시간 합(초) — 상위 구간 평균 분자: SUM(sum_dwell_sec)/SUM(dwell_count) */
    @Column(name = "sum_dwell_sec", nullable = false)
    private long sumDwellSec;

    /** 마감된 체류 쌍 수 — 상위 구간 평균 분모 */
    @Column(name = "dwell_count", nullable = false)
    private int dwellCount;

    /** 단일 버킷 평균(초) — 표시 편의값, 상위 구간 집계에 직접 쓰지 않음 */
    @Column(name = "avg_dwell_sec", nullable = false)
    private int avgDwellSec;

    @Builder
    private StatVisitPoint(Long exhibitionId, ScanPointType scanPointType, Long scanPointId,
                           LocalDate statDate, byte statHour,
                           int visitCount, int visitorCount, int uniqueAttendee,
                           long sumDwellSec, int dwellCount, int avgDwellSec) {
        this.exhibitionId = exhibitionId;
        this.scanPointType = scanPointType;
        this.scanPointId = scanPointId;
        this.statDate = statDate;
        this.statHour = statHour;
        this.visitCount = visitCount;
        this.visitorCount = visitorCount;
        this.uniqueAttendee = uniqueAttendee;
        this.sumDwellSec = sumDwellSec;
        this.dwellCount = dwellCount;
        this.avgDwellSec = avgDwellSec;
    }
}
