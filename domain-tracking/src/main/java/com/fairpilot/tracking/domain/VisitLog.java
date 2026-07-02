package com.fairpilot.tracking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 스캔 1건 = 1행(v2.4). 추적 단위는 attendee_id, head_count 로 인원 가중.
 * 셀프 스캔 ENTRY/EXIT는 서버가 open 상태로 자동 판정하고, 자동 EXIT는 is_auto_exit=true 합성 행.
 */
@Entity
@Table(name = "visit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "attendee_id", nullable = false)
    private Long attendeeId;

    @Column(name = "name_tag_id", nullable = false)
    private Long nameTagId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_point_type", nullable = false)
    private ScanPointType scanPointType;

    @Column(name = "scan_point_id")
    private Long scanPointId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false)
    private ScanType scanType;

    @Column(name = "head_count", nullable = false)
    private int headCount;

    @Column(name = "scanned_by_user_id")
    private Long scannedByUserId;

    @Column(name = "is_manual", nullable = false)
    private boolean manual;

    @Column(name = "is_auto_exit", nullable = false)
    private boolean autoExit;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @Builder
    private VisitLog(Long exhibitionId, Long attendeeId, Long nameTagId, ScanPointType scanPointType,
                     Long scanPointId, ScanType scanType, int headCount, Long scannedByUserId,
                     boolean manual, boolean autoExit, LocalDateTime scannedAt) {
        this.exhibitionId = exhibitionId;
        this.attendeeId = attendeeId;
        this.nameTagId = nameTagId;
        this.scanPointType = scanPointType;
        this.scanPointId = scanPointId;
        this.scanType = scanType;
        this.headCount = headCount;
        this.scannedByUserId = scannedByUserId;
        this.manual = manual;
        this.autoExit = autoExit;
        this.scannedAt = scannedAt == null ? LocalDateTime.now() : scannedAt;
    }
}
