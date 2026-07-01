package com.fairpilot.education;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EducationGuideRepository extends JpaRepository<EducationGuide, Long> {

    List<EducationGuide> findAllByTargetRoleAndStatus(
            TargetRole targetRole, GuideStatus status);

    /** 필수 가이드 수 조회 */
    @Query("SELECT COUNT(g) FROM EducationGuide g " +
            "WHERE g.targetRole = :role AND g.isRequired = true " +
            "AND g.status = :status " +
            "AND (g.exhibitionId = :exhibitionId OR g.exhibitionId IS NULL)")
    long countRequired(@Param("role") TargetRole role,
                       @Param("status") GuideStatus status,
                       @Param("exhibitionId") Long exhibitionId);
}