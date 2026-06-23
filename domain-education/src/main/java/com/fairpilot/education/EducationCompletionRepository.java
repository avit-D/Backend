package com.fairpilot.education;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EducationCompletionRepository extends JpaRepository<EducationCompletion, Long> {

    Optional<EducationCompletion> findByGuideIdAndUserId(Long guideId, Long userId);

    /** 이수 완료된 필수 가이드 수 조회 */
    @Query("SELECT COUNT(c) FROM EducationCompletion c " +
            "JOIN EducationGuide g ON g.id = c.guideId " +
            "WHERE c.userId = :userId AND c.passed = true " +
            "AND g.isRequired = true AND g.status = 'ACTIVE' " +
            "AND (g.exhibitionId = :exhibitionId OR g.exhibitionId IS NULL)")
    long countCompleted(@Param("userId") Long userId,
                        @Param("exhibitionId") Long exhibitionId);
}