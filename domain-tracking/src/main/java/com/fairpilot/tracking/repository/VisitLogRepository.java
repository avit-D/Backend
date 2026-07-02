package com.fairpilot.tracking.repository;

import com.fairpilot.tracking.domain.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    /** 통계 재집계/동선 흐름용: 방문자(attendee)별·시간순 정렬. */
    List<VisitLog> findByExhibitionIdOrderByAttendeeIdAscScannedAtAsc(Long exhibitionId);

    List<VisitLog> findByExhibitionId(Long exhibitionId);

    /** 사후 리포트: attendee 본인 동선 이력, 시간순 */
    List<VisitLog> findByAttendeeIdOrderByScannedAtAsc(Long attendeeId);
}
