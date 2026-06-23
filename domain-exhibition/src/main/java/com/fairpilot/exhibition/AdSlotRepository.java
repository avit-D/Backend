package com.fairpilot.exhibition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdSlotRepository extends JpaRepository<AdSlot, Long> {

    List<AdSlot> findAllByExhibitionIdAndStatus(Long exhibitionId, AdSlotStatus status);
}