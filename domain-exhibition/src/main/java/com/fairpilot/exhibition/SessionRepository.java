package com.fairpilot.exhibition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findAllByExhibitionId(Long exhibitionId);
}