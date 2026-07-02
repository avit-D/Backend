package com.fairpilot.exhibition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {

    Optional<Exhibition> findBySlug(String slug);

    boolean existsBySlug(String slug);
}