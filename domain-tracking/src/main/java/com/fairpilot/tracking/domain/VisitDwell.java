package com.fairpilot.tracking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 체류시간 집계(v2.4). ENTRY 시 open 행 생성(exit_at NULL), EXIT/자동마감 시 close.
 * head_count 로 인-분 가중. close_reason 으로 마감 원인을 구분한다.
 */
@Entity
@Table(name = "visit_dwell")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitDwell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "attendee_id", nullable = false)
    private Long attendeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_point_type", nullable = false)
    private ScanPointType scanPointType;

    @Column(name = "scan_point_id", nullable = false)
    private Long scanPointId;

    @Column(name = "entry_at", nullable = false)
    private LocalDateTime entryAt;

    @Column(name = "exit_at")
    private LocalDateTime exitAt;

    @Column(name = "dwell_seconds", nullable = false)
    private int dwellSeconds;

    @Column(name = "head_count", nullable = false)
    private int headCount;

    @Column(name = "is_estimated", nullable = false)
    private boolean estimated;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason", length = 20)
    private CloseReason closeReason;

    @Builder
    private VisitDwell(Long exhibitionId, Long attendeeId, ScanPointType scanPointType, Long scanPointId,
                       LocalDateTime entryAt, LocalDateTime exitAt, int dwellSeconds, int headCount,
                       boolean estimated, CloseReason closeReason) {
        this.exhibitionId = exhibitionId;
        this.attendeeId = attendeeId;
        this.scanPointType = scanPointType;
        this.scanPointId = scanPointId;
        this.entryAt = entryAt;
        this.exitAt = exitAt;
        this.dwellSeconds = dwellSeconds;
        this.headCount = headCount;
        this.estimated = estimated;
        this.closeReason = closeReason;
    }

    /** ENTRY 시 미종결 open 행 생성. */
    public static VisitDwell open(Long exhibitionId, Long attendeeId, ScanPointType type,
                                  Long pointId, LocalDateTime entryAt, int headCount) {
        return VisitDwell.builder()
                .exhibitionId(exhibitionId).attendeeId(attendeeId)
                .scanPointType(type).scanPointId(pointId)
                .entryAt(entryAt).headCount(headCount).build();
    }

    /** EXIT/자동마감 시 종료 처리. */
    public void close(LocalDateTime exitAt, CloseReason reason, boolean estimated) {
        this.exitAt = exitAt;
        this.dwellSeconds = (int) Math.max(0, Duration.between(entryAt, exitAt).getSeconds());
        this.closeReason = reason;
        this.estimated = estimated;
    }

    public boolean isOpen() { return exitAt == null; }
}
