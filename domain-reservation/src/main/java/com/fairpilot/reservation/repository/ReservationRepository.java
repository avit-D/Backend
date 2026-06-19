package com.fairpilot.reservation.repository;

import com.fairpilot.reservation.domain.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Page<Reservation> findByUserId(Long userId, Pageable pageable);
    Optional<Reservation> findByUserIdAndExhibitionId(Long userId, Long exhibitionId);
    boolean existsByUserIdAndExhibitionId(Long userId, Long exhibitionId);
}
