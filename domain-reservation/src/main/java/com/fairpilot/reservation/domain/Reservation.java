package com.fairpilot.reservation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE reservation SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "time_slot_id")
    private Long timeSlotId;

    @Column(name = "ticket_type_id")
    private Long ticketTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_mode", nullable = false, length = 12)
    private MovementMode movementMode;

    @Column(name = "group_size", nullable = false)
    private int groupSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_source", nullable = false, length = 16)
    private ReservationSource reservationSource;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder
    private Reservation(Long userId, Long exhibitionId, Long timeSlotId, Long ticketTypeId,
                        MovementMode movementMode, int groupSize, ReservationStatus status,
                        ReservationSource reservationSource) {
        this.userId = userId;
        this.exhibitionId = exhibitionId;
        this.timeSlotId = timeSlotId;
        this.ticketTypeId = ticketTypeId;
        this.movementMode = movementMode;
        this.groupSize = groupSize;
        this.status = status;
        this.reservationSource = reservationSource;
        this.isDeleted = false;
    }

    public void markPaid()      { this.status = ReservationStatus.PAID; }
    public void markCancelled() { this.status = ReservationStatus.CANCELLED; }
    public void markCheckedIn() { this.status = ReservationStatus.CHECKED_IN; }
    public void decreaseGroupSize(int by) {
        this.groupSize = Math.max(0, this.groupSize - by);
    }
}