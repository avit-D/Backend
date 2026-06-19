package com.fairpilot.reservation.service;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.reservation.domain.*;
import com.fairpilot.reservation.dto.ReserveRequest;
import com.fairpilot.reservation.repository.ReservationAttendeeRepository;
import com.fairpilot.reservation.repository.ReservationRepository;
import com.fairpilot.reservation.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 예약 동시성 핵심 서비스 (개발자 4번, v2.4).
 * 정원 차감 단위는 reservation.group_size, 추적 단위는 reservation_attendee.
 * 원자 조건부 UPDATE 단일 전략으로 초과 예약(Overbooking)을 원천 차단한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final TimeSlotRepository timeSlotRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationAttendeeRepository attendeeRepository;
    private final SeatHoldService seatHoldService;
    private final QueueService queueService;

    @Transactional
    public Reservation reserve(Long userId, ReserveRequest req) {
        validate(req);
        if (req.useQueue()) {
            queueService.requireAllowed(req.timeSlotId(), userId);
        }
        // 1) group_size 만큼 원자적 정원 선점
        int updated = timeSlotRepository.increaseReserved(req.timeSlotId(), req.groupSize());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SLOT_SOLD_OUT);
        }
        // 2) 예약 헤더
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .userId(userId).exhibitionId(req.exhibitionId())
                .timeSlotId(req.timeSlotId()).ticketTypeId(req.ticketTypeId())
                .movementMode(req.movementMode()).groupSize(req.groupSize())
                .status(ReservationStatus.PENDING).reservationSource(ReservationSource.ONLINE)
                .build());
        // 3) 참석자 + QR (INDIVIDUAL=각자, GROUP=대표만)
        createAttendees(reservation, req);
        // 4) 결제 전 임시 점유(미결제 시 스케줄러가 group_size 반납)
        seatHoldService.registerHold(reservation.getId(), req.timeSlotId());
        if (req.useQueue()) {
            queueService.leave(req.timeSlotId(), userId);
        }
        return reservation;
    }

    @Transactional
    public void confirmPaid(Long reservationId) {
        Reservation r = getReservation(reservationId);
        if (r.getStatus() == ReservationStatus.PENDING) {
            r.markPaid();
            seatHoldService.confirmHold(reservationId);
            if (r.getTimeSlotId() != null) queueService.leave(r.getTimeSlotId(), r.getUserId());
        }
    }

    /** 예약 전체 취소: group_size 만큼 정원 복원 + 참석자 전원 취소. */
    @Transactional
    public void cancel(Long reservationId, Long userId) {
        Reservation r = getReservation(reservationId);
        if (!r.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (r.getStatus() == ReservationStatus.CANCELLED || r.getStatus() == ReservationStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 취소된 예약입니다.");
        }
        restoreSeat(r.getTimeSlotId(), r.getGroupSize());
        attendeeRepository.findByReservationId(reservationId)
                .forEach(a -> { if (a.getAttendeeStatus() == AttendeeStatus.ACTIVE) a.cancel(); });
        seatHoldService.confirmHold(reservationId);
        r.markCancelled();
        reservationRepository.delete(r); // Soft Delete
    }

    /** 참석자 단위 부분 취소: 1명 정원 복원(INDIVIDUAL, 체크인 전만). */
    @Transactional
    public void cancelAttendee(Long reservationId, Long attendeeId, Long userId) {
        Reservation r = getReservation(reservationId);
        if (!r.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (r.getMovementMode() != MovementMode.INDIVIDUAL) {
            throw new BusinessException(ErrorCode.CONFLICT, "GROUP 예약은 부분 취소가 제한됩니다. 인원 수정으로 처리하세요.");
        }
        ReservationAttendee a = attendeeRepository.findById(attendeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!a.getReservationId().equals(reservationId)) throw new BusinessException(ErrorCode.INVALID_INPUT);
        if (a.getCheckinStatus() == CheckinStatus.CHECKED_IN) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 체크인한 참석자는 관리자 예외 처리만 가능합니다.");
        }
        if (a.getAttendeeStatus() != AttendeeStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 취소된 참석자입니다.");
        }
        a.cancel();
        restoreSeat(r.getTimeSlotId(), 1);
        r.decreaseGroupSize(1);
    }

    /** 스케줄러: 미결제 Hold 만료 → group_size 반납. */
    @Transactional
    public void releaseExpired(Long reservationId) {
        reservationRepository.findById(reservationId).ifPresent(r -> {
            if (r.getStatus() == ReservationStatus.PENDING) {
                restoreSeat(r.getTimeSlotId(), r.getGroupSize());
                attendeeRepository.findByReservationId(reservationId).forEach(ReservationAttendee::cancel);
                r.markCancelled();
                reservationRepository.delete(r);
                log.info("hold expired -> released reservation={} seats={}", reservationId, r.getGroupSize());
            }
        });
    }

    // ---- helpers ----
    private void validate(ReserveRequest req) {
        if (req.movementMode() == MovementMode.INDIVIDUAL && req.attendees().size() != req.groupSize()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "INDIVIDUAL 예약은 참석자 수와 group_size가 같아야 합니다.");
        }
        if (req.movementMode() == MovementMode.GROUP) {
            long leaders = req.attendees().stream().filter(ReserveRequest.AttendeeRequest::isGroupLeader).count();
            if (leaders != 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "GROUP 예약은 대표(isGroupLeader) 1명이 필요합니다.");
            }
        }
    }

    private void createAttendees(Reservation r, ReserveRequest req) {
        List<ReservationAttendee> list = new ArrayList<>();
        boolean group = req.movementMode() == MovementMode.GROUP;
        for (ReserveRequest.AttendeeRequest ar : req.attendees()) {
            boolean leader = group ? ar.isGroupLeader() : false;
            // GROUP은 대표만 QR, INDIVIDUAL은 각자 QR
            String qr = (group ? leader : true) ? newToken() : null;
            list.add(ReservationAttendee.builder()
                    .reservationId(r.getId()).exhibitionId(r.getExhibitionId())
                    .name(ar.name()).phone(ar.phone()).email(ar.email())
                    .groupLeader(leader).ticketQrToken(qr).build());
        }
        attendeeRepository.saveAll(list);
    }

    private void restoreSeat(Long timeSlotId, int amount) {
        if (timeSlotId != null && amount > 0) timeSlotRepository.decreaseReserved(timeSlotId, amount);
    }

    private Reservation getReservation(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
