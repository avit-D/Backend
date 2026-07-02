package com.fairpilot.reservation.dto;

import com.fairpilot.reservation.domain.Reservation;
import com.fairpilot.reservation.domain.ReservationAttendee;

import java.util.List;

public record ReservationResponse(
        Long reservationId, String status, String movementMode, int groupSize,
        List<AttendeeResponse> attendees
) {
    public record AttendeeResponse(Long attendeeId, String name, boolean groupLeader,
                                   String ticketQrToken, String checkinStatus, String attendeeStatus) {
        public static AttendeeResponse from(ReservationAttendee a) {
            return new AttendeeResponse(a.getId(), a.getName(), a.isGroupLeader(),
                    a.getTicketQrToken(), a.getCheckinStatus().name(), a.getAttendeeStatus().name());
        }
    }

    public static ReservationResponse of(Reservation r, List<ReservationAttendee> attendees) {
        return new ReservationResponse(
                r.getId(), r.getStatus().name(), r.getMovementMode().name(), r.getGroupSize(),
                attendees.stream().map(AttendeeResponse::from).toList());
    }
}
