package com.fairpilot.tracking.dto;

import com.fairpilot.reservation.domain.ReservationAttendee;

/** 개별 참석자 체크인 상태 */
public record AttendeeCheckinStatusResponse(
        Long attendeeId,
        String name,
        boolean groupLeader,
        String checkinStatus,
        String attendeeStatus,
        Long nameTagId,
        String nameTagToken
) {
    public static AttendeeCheckinStatusResponse of(ReservationAttendee a, Long nameTagId, String nameTagToken) {
        return new AttendeeCheckinStatusResponse(
                a.getId(), a.getName(), a.isGroupLeader(),
                a.getCheckinStatus().name(), a.getAttendeeStatus().name(),
                nameTagId, nameTagToken
        );
    }
}
