package com.fairpilot.tracking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체크인 이력 (기획안 §4.5.3 checkin_log).
 * 모든 체크인 처리(바인딩·워크인·재발급)마다 1건 기록.
 */
@Entity
@Table(name = "checkin_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckinLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    /** nullable: 워크인은 예약 생성 후 채워짐, REISSUE 시에도 기존 예약 ID 참조 */
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "attendee_id")
    private Long attendeeId;

    /** nullable: 워크인에서 nametag 바인딩 전 단계 로그 등 */
    @Column(name = "name_tag_id")
    private Long nameTagId;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkin_method", nullable = false)
    private CheckinMethod checkinMethod;

    /** 처리한 스태프 userId */
    @Column(name = "checked_in_by_user_id")
    private Long checkedInByUserId;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "memo")
    private String memo;

    // -------------------------------------------------------
    // 팩토리
    // -------------------------------------------------------

    public static CheckinLog of(Long exhibitionId,
                                Long reservationId,
                                Long attendeeId,
                                Long nameTagId,
                                CheckinMethod method,
                                Long staffUserId,
                                String memo) {
        CheckinLog log = new CheckinLog();
        log.exhibitionId = exhibitionId;
        log.reservationId = reservationId;
        log.attendeeId = attendeeId;
        log.nameTagId = nameTagId;
        log.checkinMethod = method;
        log.checkedInByUserId = staffUserId;
        log.checkedInAt = LocalDateTime.now();
        log.memo = memo;
        return log;
    }
}
