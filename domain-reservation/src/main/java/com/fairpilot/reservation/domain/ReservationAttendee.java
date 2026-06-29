package com.fairpilot.reservation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_attendee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE reservation_attendee SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
public class ReservationAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "linked_user_id")
    private Long linkedUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "is_group_leader", nullable = false)
    private boolean groupLeader;

    @Column(name = "ticket_qr_token", length = 64)
    private String ticketQrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkin_status", nullable = false, length = 20)
    private CheckinStatus checkinStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendee_status", nullable = false, length = 12)
    private AttendeeStatus attendeeStatus;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder
    private ReservationAttendee(Long reservationId, Long exhibitionId, Long linkedUserId,
                                String name, String phone, String email, boolean groupLeader,
                                String ticketQrToken) {
        this.reservationId = reservationId;
        this.exhibitionId = exhibitionId;
        this.linkedUserId = linkedUserId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.groupLeader = groupLeader;
        this.ticketQrToken = ticketQrToken;
        this.checkinStatus = CheckinStatus.NOT_CHECKED_IN;
        this.attendeeStatus = AttendeeStatus.ACTIVE;
        this.isDeleted = false;
    }

    public void cancel() {
        this.attendeeStatus = AttendeeStatus.CANCELLED;
        this.ticketQrToken = null;
    }

    public void checkIn() {
        this.checkinStatus = CheckinStatus.CHECKED_IN;
        this.checkedInAt = LocalDateTime.now();
    }

    public void update(String name, String phone, String email) {
        if (name  != null) this.name  = name;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
    }
}