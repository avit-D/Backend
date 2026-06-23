package com.fairpilot.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllByExhibitionId(Long exhibitionId);

    List<Settlement> findAllByExhibitionIdAndStatus(
            Long exhibitionId, SettlementStatus status);
}