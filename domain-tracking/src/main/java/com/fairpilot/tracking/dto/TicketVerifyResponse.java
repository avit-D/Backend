package com.fairpilot.tracking.dto;

import com.fairpilot.reservation.domain.CheckinStatus;
import com.fairpilot.reservation.domain.Reservation;
import com.fairpilot.reservation.domain.ReservationAttendee;

/** POST /api/checkin/verify 응답 — 티켓 QR 검증 결과 */
public record TicketVerifyResponse(
        Long reservationId,
        Long attendeeId,
        String attendeeName,
        String attendeePhone,
        boolean groupLeader,
        String reservationStatus,
        String checkinStatus,
        boolean alreadyCheckedIn
) {
    public static TicketVerifyResponse of(Reservation r, ReservationAttendee a) {
        return new TicketVerifyResponse(
                r.getId(),
                a.getId(),
                a.getName(),
                a.getPhone(),
                a.isGroupLeader(),
                r.getStatus().name(),
                a.getCheckinStatus().name(),
                a.getCheckinStatus() == CheckinStatus.CHECKED_IN
        );
    }
}
