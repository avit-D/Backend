package com.fairpilot.reservation.repository;

import com.fairpilot.reservation.domain.ReservationAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationAttendeeRepository extends JpaRepository<ReservationAttendee, Long> {
    List<ReservationAttendee> findByReservationId(Long reservationId);
    Optional<ReservationAttendee> findByTicketQrToken(String ticketQrToken);

    /**
     * 체크인 참석자 검색 — 이름·전화번호·이메일·예약번호 부분 일치.
     * CheckinService.lookupAttendees() 에서 사용.
     */
    @Query("""
            SELECT a FROM ReservationAttendee a
            JOIN Reservation r ON r.id = a.reservationId
            WHERE r.exhibitionId = :exhibitionId
              AND (a.name LIKE %:query%
                OR a.phone LIKE %:query%
                OR a.email LIKE %:query%
                OR CAST(a.reservationId AS string) LIKE %:query%)
            """)
    List<ReservationAttendee> searchByExhibitionIdAndQuery(
            @Param("exhibitionId") Long exhibitionId,
            @Param("query") String query);
}
