package com.fairpilot.tracking.stats.repository;

import com.fairpilot.tracking.stats.domain.StatVisitPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatVisitPointRepository extends JpaRepository<StatVisitPoint, Long> {

    @Modifying
    @Query("delete from StatVisitPoint s where s.exhibitionId = :exhibitionId")
    void deleteByExhibitionId(@Param("exhibitionId") Long exhibitionId);

    List<StatVisitPoint> findByExhibitionId(Long exhibitionId);
}
