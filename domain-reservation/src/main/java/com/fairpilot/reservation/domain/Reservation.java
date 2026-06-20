package com.fairpilot.reservation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 예약 헤더(결제·정원 차감 단위). v2.4: movement_mode/group_size/reservation_source 추가,
 * ticket_qr_token 제거(참석자별 reservation_attendee 로 이동). Soft Delete 적용.
 */
@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE reservation SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
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

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
    }

    public void markPaid()      { this.status = ReservationStatus.PAID; }
    public void markCancelled() { this.status = ReservationStatus.CANCELLED; }
    public void markCheckedIn() { this.status = ReservationStatus.CHECKED_IN; }
    public void decreaseGroupSize(int by) { this.groupSize = Math.max(0, this.groupSize - by); }
}
