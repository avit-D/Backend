package com.fairpilot.tracking.stats.repository;

import com.fairpilot.tracking.stats.domain.StatCongestionHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatCongestionHourlyRepository extends JpaRepository<StatCongestionHourly, Long> {

    @Modifying
    @Query("delete from StatCongestionHourly s where s.exhibitionId = :exhibitionId")
    void deleteByExhibitionId(@Param("exhibitionId") Long exhibitionId);

    List<StatCongestionHourly> findByExhibitionId(Long exhibitionId);
}
