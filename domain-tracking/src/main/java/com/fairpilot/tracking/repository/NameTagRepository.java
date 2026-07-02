package com.fairpilot.tracking.repository;

import com.fairpilot.tracking.domain.NameTag;
import com.fairpilot.tracking.domain.NameTagStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NameTagRepository extends JpaRepository<NameTag, Long> {

    Optional<NameTag> findByToken(String token);

    /** 특정 참석자의 현재 ISSUED 태그 조회 (재발급 시 기존 태그 찾기) */
    Optional<NameTag> findByAttendeeIdAndStatus(Long attendeeId, NameTagStatus status);

    /** 전시회 전체 태그 조회 (재고 현황) */
    List<NameTag> findByExhibitionId(Long exhibitionId);

    /** 전시회 + 상태 필터 */
    List<NameTag> findByExhibitionIdAndStatus(Long exhibitionId, NameTagStatus status);

    long countByExhibitionIdAndStatus(Long exhibitionId, NameTagStatus status);
}
