package com.fairpilot.exhibition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findAllByExhibitionId(Long exhibitionId);
}