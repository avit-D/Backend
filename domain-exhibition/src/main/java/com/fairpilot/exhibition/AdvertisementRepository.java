package com.fairpilot.exhibition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    List<Advertisement> findAllByAdSlotIdAndStatus(Long adSlotId, AdStatus status);

    @Modifying
    @Query("UPDATE Advertisement a SET a.impressions = a.impressions + 1 WHERE a.id = :id")
    void incrementImpressions(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Advertisement a SET a.clicks = a.clicks + 1 WHERE a.id = :id")
    void incrementClicks(@Param("id") Long id);
}
