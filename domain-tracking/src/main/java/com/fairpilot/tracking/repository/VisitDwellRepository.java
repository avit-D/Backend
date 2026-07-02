package com.fairpilot.tracking.repository;

import com.fairpilot.tracking.domain.VisitDwell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VisitDwellRepository extends JpaRepository<VisitDwell, Long> {

    /** 스캔 엔진: attendee 의 미종결(open) 체류 행. (정상상 1건) */
    Optional<VisitDwell> findFirstByAttendeeIdAndExitAtIsNullOrderByEntryAtDesc(Long attendeeId);

    List<VisitDwell> findByExhibitionId(Long exhibitionId);

    /** 사후 리포트: attendee 본인 체류 이력, 입장순 */
    List<VisitDwell> findByAttendeeIdOrderByEntryAtAsc(Long attendeeId);

    @Modifying
    @Query("delete from VisitDwell d where d.exhibitionId = :exhibitionId")
    void deleteByExhibitionId(@Param("exhibitionId") Long exhibitionId);
}
