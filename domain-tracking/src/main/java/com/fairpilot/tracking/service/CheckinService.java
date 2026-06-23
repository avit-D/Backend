package com.fairpilot.tracking.service;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.reservation.domain.*;
import com.fairpilot.reservation.repository.ReservationAttendeeRepository;
import com.fairpilot.reservation.repository.ReservationRepository;
import com.fairpilot.tracking.domain.*;
import com.fairpilot.tracking.dto.*;
import com.fairpilot.tracking.repository.CheckinLogRepository;
import com.fairpilot.tracking.repository.NameTagRepository;
import com.fairpilot.tracking.repository.VisitLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 체크인 도메인 서비스 (기획안 §6.4).
 */
@Service
@RequiredArgsConstructor
public class CheckinService {

    private final ReservationAttendeeRepository attendeeRepo;
    private final ReservationRepository reservationRepo;
    private final NameTagRepository nameTagRepo;
    private final CheckinLogRepository checkinLogRepo;
    private final VisitLogRepository visitLogRepo;

    // -------------------------------------------------------
    // 1. 티켓 QR 검증
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public TicketVerifyResponse verifyTicket(Long exhibitionId, String ticketQrToken) {
        ReservationAttendee attendee = findAttendeeByTicketToken(ticketQrToken);
        Reservation reservation = findReservation(attendee.getReservationId());
        validateExhibition(exhibitionId, reservation.getExhibitionId());
        validatePaid(reservation);
        return TicketVerifyResponse.of(reservation, attendee);
    }

    // -------------------------------------------------------
    // 2. QR 티켓 → 네임태그 바인딩 (최초 체크인)
    // -------------------------------------------------------

    @Transactional
    public CheckinResultResponse bindNametag(BindNametagRequest req, Long staffUserId) {
        ReservationAttendee attendee = findAttendeeByTicketToken(req.ticketQrToken());
        Reservation reservation = findReservation(attendee.getReservationId());
        validateExhibition(req.exhibitionId(), reservation.getExhibitionId());
        validatePaid(reservation);

        NameTag nametag = resolveNametagForBind(req.nametagToken(), attendee.getId());

        // [Fix #2] 멱등 처리: 같은 태그가 이미 이 참석자에게 ISSUED → 200 OK 반환
        if (nametag.getStatus() == NameTagStatus.ISSUED
                && attendee.getId().equals(nametag.getAttendeeId())) {
            return toIdempotentResult(attendee, nametag);
        }

        // 이미 다른 태그가 ISSUED된 참석자 → 자동 재발급
        Optional<NameTag> existingIssued =
                nameTagRepo.findByAttendeeIdAndStatus(attendee.getId(), NameTagStatus.ISSUED);
        if (existingIssued.isPresent()) {
            return doReissue(existingIssued.get(), nametag, attendee, reservation, staffUserId, null);
        }

        // 최초 바인딩
        nametag.bind(attendee.getId(), staffUserId);
        attendee.checkIn();
        nameTagRepo.save(nametag);
        attendeeRepo.save(attendee);

        CheckinLog log = checkinLogRepo.save(CheckinLog.of(
                req.exhibitionId(), reservation.getId(), attendee.getId(),
                nametag.getId(), CheckinMethod.QR_SELF, staffUserId, null));

        // [Fix #1] GROUP 이동이면 head_count = group_size
        int headCount = headCountFor(reservation);
        createGateEntry(req.exhibitionId(), attendee.getId(), nametag.getId(), staffUserId, headCount);

        return toResult(log, attendee, nametag, true);
    }

    // -------------------------------------------------------
    // 3. 수동 검색 바인딩
    // -------------------------------------------------------

    @Transactional
    public CheckinResultResponse manualBind(ManualBindRequest req, Long staffUserId) {
        ReservationAttendee attendee = attendeeRepo.findById(req.attendeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "참석자를 찾을 수 없습니다."));
        Reservation reservation = findReservation(attendee.getReservationId());
        validateExhibition(req.exhibitionId(), reservation.getExhibitionId());
        validatePaid(reservation);

        NameTag nametag = resolveNametagForBind(req.nametagToken(), attendee.getId());

        // [Fix #2] 멱등 처리
        if (nametag.getStatus() == NameTagStatus.ISSUED
                && attendee.getId().equals(nametag.getAttendeeId())) {
            return toIdempotentResult(attendee, nametag);
        }

        Optional<NameTag> existingIssued =
                nameTagRepo.findByAttendeeIdAndStatus(attendee.getId(), NameTagStatus.ISSUED);
        if (existingIssued.isPresent()) {
            return doReissue(existingIssued.get(), nametag, attendee, reservation, staffUserId, req.memo());
        }

        nametag.bind(attendee.getId(), staffUserId);
        attendee.checkIn();
        nameTagRepo.save(nametag);
        attendeeRepo.save(attendee);

        CheckinLog log = checkinLogRepo.save(CheckinLog.of(
                req.exhibitionId(), reservation.getId(), attendee.getId(),
                nametag.getId(), CheckinMethod.MANUAL_SEARCH, staffUserId, req.memo()));

        int headCount = headCountFor(reservation);
        createGateEntry(req.exhibitionId(), attendee.getId(), nametag.getId(), staffUserId, headCount);

        return toResult(log, attendee, nametag, true);
    }

    // -------------------------------------------------------
    // 4. 재발급 (REVOKE 기존 + ISSUE 신규, GATE ENTRY 없음)
    // -------------------------------------------------------

    @Transactional
    public CheckinResultResponse reissueNametag(Long exhibitionId, Long attendeeId,
                                                String newNametagToken, Long staffUserId, String memo) {
        ReservationAttendee attendee = attendeeRepo.findById(attendeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "참석자를 찾을 수 없습니다."));

        NameTag oldTag = nameTagRepo.findByAttendeeIdAndStatus(attendeeId, NameTagStatus.ISSUED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "ISSUED 상태의 네임태그가 없습니다."));

        NameTag newTag = resolveNametagForBind(newNametagToken, attendeeId);
        Reservation reservation = findReservation(attendee.getReservationId());

        return doReissue(oldTag, newTag, attendee, reservation, staffUserId, memo);
    }

    // -------------------------------------------------------
    // 5. 워크인 (현장 즉시 예약 + 바인딩 + GATE ENTRY)
    // -------------------------------------------------------

    @Transactional
    public CheckinResultResponse walkIn(WalkInRequest req, Long staffUserId) {
        Reservation reservation = Reservation.builder()
                .userId(staffUserId)
                .exhibitionId(req.exhibitionId())
                .movementMode(MovementMode.INDIVIDUAL)
                .groupSize(req.groupSize())
                .status(ReservationStatus.PAID)
                .reservationSource(ReservationSource.ONSITE_MANUAL)
                .build();
        reservation = reservationRepo.save(reservation);

        ReservationAttendee leader = ReservationAttendee.builder()
                .reservationId(reservation.getId())
                .exhibitionId(req.exhibitionId())
                .name(req.leaderName())
                .phone(req.leaderPhone())
                .groupLeader(true)
                .ticketQrToken("WI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .build();
        leader = attendeeRepo.save(leader);

        NameTag nametag = resolveNametagForBind(req.nametagToken(), leader.getId());
        nametag.bind(leader.getId(), staffUserId);
        leader.checkIn();
        nameTagRepo.save(nametag);
        attendeeRepo.save(leader);

        CheckinLog log = checkinLogRepo.save(CheckinLog.of(
                req.exhibitionId(), reservation.getId(), leader.getId(),
                nametag.getId(), CheckinMethod.WALK_IN, staffUserId, req.memo()));

        // 워크인은 항상 INDIVIDUAL(head_count=1)
        createGateEntry(req.exhibitionId(), leader.getId(), nametag.getId(), staffUserId, 1);

        return toResult(log, leader, nametag, true);
    }

    // -------------------------------------------------------
    // 6. 네임태그 회수
    // -------------------------------------------------------

    @Transactional
    public void revokeNametag(Long nameTagId, Long staffUserId) {
        NameTag tag = nameTagRepo.findById(nameTagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "네임태그를 찾을 수 없습니다."));
        tag.revoke();
        nameTagRepo.save(tag);
    }

    // -------------------------------------------------------
    // 7. 참석자 검색
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TicketVerifyResponse> lookupAttendees(Long exhibitionId, String query) {
        return attendeeRepo.searchByExhibitionIdAndQuery(exhibitionId, query).stream()
                .map(a -> TicketVerifyResponse.of(findReservation(a.getReservationId()), a))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // 8. 팀 체크인 현황
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public TeamCheckinStatusResponse teamCheckinStatus(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        List<ReservationAttendee> attendees = attendeeRepo.findByReservationId(reservationId);

        List<AttendeeCheckinStatusResponse> attendeeResponses = attendees.stream().map(a -> {
            Optional<NameTag> tag = nameTagRepo.findByAttendeeIdAndStatus(a.getId(), NameTagStatus.ISSUED);
            return AttendeeCheckinStatusResponse.of(
                    a,
                    tag.map(NameTag::getId).orElse(null),
                    tag.map(NameTag::getToken).orElse(null)
            );
        }).collect(Collectors.toList());

        long checkedIn = attendees.stream()
                .filter(a -> a.getCheckinStatus() == CheckinStatus.CHECKED_IN).count();

        return new TeamCheckinStatusResponse(
                reservation.getId(),
                reservation.getStatus().name(),
                reservation.getGroupSize(),
                (int) checkedIn,
                attendeeResponses
        );
    }

    // -------------------------------------------------------
    // 내부 헬퍼
    // -------------------------------------------------------

    /**
     * [Fix #2] 바인딩용 태그 조회 — 멱등 케이스(같은 attendee에 이미 ISSUED)를 위해
     * AVAILABLE 체크를 여기서 하지 않고, 호출부에서 조건 분기.
     */
    private NameTag resolveNametagForBind(String token, Long attendeeId) {
        NameTag tag = nameTagRepo.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "네임태그를 찾을 수 없습니다."));
        // AVAILABLE이거나, 이 attendee에게 이미 ISSUED된 경우만 통과
        if (tag.getStatus() == NameTagStatus.AVAILABLE) return tag;
        if (tag.getStatus() == NameTagStatus.ISSUED && attendeeId.equals(tag.getAttendeeId())) return tag;
        throw new BusinessException(ErrorCode.CONFLICT,
                "사용 가능한 네임태그가 아닙니다. 현재 상태: " + tag.getStatus());
    }

    /** REVOKE 기존 태그 + ISSUE 새 태그 — GATE ENTRY 없음 */
    private CheckinResultResponse doReissue(NameTag oldTag, NameTag newTag,
                                            ReservationAttendee attendee, Reservation reservation,
                                            Long staffUserId, String memo) {
        oldTag.revoke();
        newTag.bind(attendee.getId(), staffUserId);
        nameTagRepo.save(oldTag);
        nameTagRepo.save(newTag);

        CheckinLog log = checkinLogRepo.save(CheckinLog.of(
                reservation.getExhibitionId(), reservation.getId(), attendee.getId(),
                newTag.getId(), CheckinMethod.REISSUE, staffUserId, memo));

        return toResult(log, attendee, newTag, false);
    }

    /** [Fix #1] GROUP 이동이면 group_size, INDIVIDUAL이면 1 */
    private int headCountFor(Reservation reservation) {
        return reservation.getMovementMode() == MovementMode.GROUP
                ? reservation.getGroupSize()
                : 1;
    }

    /** GATE ENTRY visit_log 기록 */
    private void createGateEntry(Long exhibitionId, Long attendeeId, Long nameTagId,
                                 Long staffUserId, int headCount) {
        VisitLog gate = VisitLog.builder()
                .exhibitionId(exhibitionId)
                .attendeeId(attendeeId)
                .nameTagId(nameTagId)
                .scanPointType(ScanPointType.GATE)
                .scanPointId(null)
                .scanType(ScanType.ENTRY)
                .headCount(headCount)
                .scannedByUserId(staffUserId)
                .manual(true)
                .autoExit(false)
                .scannedAt(null)
                .build();
        visitLogRepo.save(gate);
    }

    /** 멱등 응답 — 이미 처리된 바인딩에 대해 log 없이 현재 상태 반환 */
    private CheckinResultResponse toIdempotentResult(ReservationAttendee attendee, NameTag nametag) {
        return new CheckinResultResponse(
                null,                     // 새 log 없음
                attendee.getId(), attendee.getName(),
                nametag.getId(), nametag.getToken(),
                "IDEMPOTENT",             // 멱등 처리 표시
                false
        );
    }

    private ReservationAttendee findAttendeeByTicketToken(String token) {
        return attendeeRepo.findByTicketQrToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유효하지 않은 QR 티켓입니다."));
    }

    private Reservation findReservation(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "예약을 찾을 수 없습니다."));
    }

    private void validateExhibition(Long expected, Long actual) {
        if (!expected.equals(actual)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "전시회 불일치.");
        }
    }

    private void validatePaid(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PAID
                && reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "결제 완료된 예약만 체크인 가능합니다. 현재 상태: " + reservation.getStatus());
        }
    }

    private CheckinResultResponse toResult(CheckinLog log, ReservationAttendee attendee,
                                           NameTag nametag, boolean gateEntry) {
        return new CheckinResultResponse(
                log.getId(), attendee.getId(), attendee.getName(),
                nametag.getId(), nametag.getToken(),
                log.getCheckinMethod().name(), gateEntry
        );
    }
}
