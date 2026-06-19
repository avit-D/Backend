package com.fairpilot.tracking.repository;

import com.fairpilot.tracking.domain.NameTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NameTagRepository extends JpaRepository<NameTag, Long> {
    Optional<NameTag> findByToken(String token);
}
