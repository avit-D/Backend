package com.fairpilot.tracking.stats.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 박람회 전체 시간대별 최대 동시 입장 인원 스냅샷 (v2.4 스키마).
 * 지점별이 아닌 exhibition 단위 집계. 배치 재적재.
 * UNIQUE: (exhibition_id, stat_date, stat_hour)
 */
@Entity
@Table(name = "stat_congestion_hourly",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_stat_cong",
                columnNames = {"exhibition_id", "stat_date", "stat_hour"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatCongestionHourly {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** 집계 기준 시(0-23) */
    @Column(name = "stat_hour", nullable = false)
    private byte statHour;

    /** 해당 시간대 최대 동시 입장 인원(head_count 합산 추정) */
    @Column(name = "head_count", nullable = false)
    private int headCount;

    @Builder
    private StatCongestionHourly(Long exhibitionId, LocalDate statDate, byte statHour, int headCount) {
        this.exhibitionId = exhibitionId;
        this.statDate = statDate;
        this.statHour = statHour;
        this.headCount = headCount;
    }
}
