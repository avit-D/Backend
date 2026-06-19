package com.fairpilot.reservation.repository;

import com.fairpilot.reservation.domain.ReservationAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationAttendeeRepository extends JpaRepository<ReservationAttendee, Long> {
    List<ReservationAttendee> findByReservationId(Long reservationId);
    Optional<ReservationAttendee> findByTicketQrToken(String ticketQrToken);
}
