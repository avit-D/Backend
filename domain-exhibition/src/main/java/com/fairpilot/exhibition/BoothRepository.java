package com.fairpilot.exhibition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothRepository extends JpaRepository<Booth, Long> {

    List<Booth> findAllByExhibitionId(Long exhibitionId);
}