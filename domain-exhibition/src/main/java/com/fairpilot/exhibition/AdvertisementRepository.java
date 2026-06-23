package com.fairpilot.exhibition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    List<Advertisement> findAllByAdSlotId(Long adSlotId);

    List<Advertisement> findAllByAdSlotIdAndStatus(Long adSlotId, AdStatus status);
}