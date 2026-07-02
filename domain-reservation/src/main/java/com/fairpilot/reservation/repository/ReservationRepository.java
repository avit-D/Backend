package com.fairpilot.reservation.repository;

import com.fairpilot.reservation.domain.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Page<Reservation> findByUserId(Long userId, Pageable pageable);
    Optional<Reservation> findByUserIdAndExhibitionId(Long userId, Long exhibitionId);
    boolean existsByUserIdAndExhibitionId(Long userId, Long exhibitionId);

    /** EXPO_ADMIN: 전시회 예약 목록 (페이징) */
    Page<Reservation> findByExhibitionId(Long exhibitionId, Pageable pageable);

    /** EXPO_ADMIN: 전시회 예약 전체 (엑셀 export) */
    List<Reservation> findByExhibitionId(Long exhibitionId);
}
