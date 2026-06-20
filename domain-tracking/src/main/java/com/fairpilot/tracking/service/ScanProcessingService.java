package com.fairpilot.tracking.service;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.tracking.domain.NameTag;
import com.fairpilot.tracking.domain.NameTagStatus;
import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.domain.ScanType;
import com.fairpilot.tracking.congestion.dto.CongestionEvent;
import com.fairpilot.tracking.congestion.service.CongestionCounterService;
import com.fairpilot.tracking.congestion.service.CongestionMessageRouter;
import com.fairpilot.tracking.dto.ScanRequest;
import com.fairpilot.tracking.dto.ScanResult;
import com.fairpilot.tracking.repository.NameTagRepository;
import com.fairpilot.reservation.domain.MovementMode;
import com.fairpilot.reservation.domain.Reservation;
import com.fairpilot.reservation.domain.ReservationAttendee;
import com.fairpilot.reservation.repository.ReservationAttendeeRepository;
import com.fairpilot.reservation.repository.ReservationRepository;
import com.fairpilot.tracking.domain.CloseReason;
import com.fairpilot.tracking.domain.VisitDwell;
import com.fairpilot.tracking.domain.VisitLog;
import com.fairpilot.tracking.repository.VisitDwellRepository;
import com.fairpilot.tracking.repository.VisitLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * 부스/세션 셀프 스캔 처리 엔진 (개발자 4번, v2.4).
 * 서버가 open 상태로 ENTRY/EXIT를 자동 판정하고, visit_log·visit_dwell·Redis 혼잡 카운터를 일관되게 갱신한다.
 * attendee 단위 분산 락으로 동시 중복 스캔을 직렬화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanProcessingService {

    private final NameTagRepository nameTagRepository;
    private final ReservationAttendeeRepository attendeeRepository;
    private final ReservationRepository reservationRepository;
    private final VisitLogRepository visitLogRepository;
    private final VisitDwellRepository visitDwellRepository;
    private final OpenStateService openStateService;
    private final CongestionCounterService counterService;
    private final CongestionMessageRouter congestionRouter;
    private final RedissonClient redissonClient;
    private final TransactionTemplate txTemplate;

    @Value("${fairpilot.stats.debounce-seconds:30}") private int debounceSeconds;
    @Value("${fairpilot.stats.dwell-cap-seconds:3600}") private int capSeconds;

    public ScanResult scan(ScanRequest req, Long scannedByUserId) {
        // 1) 토큰 → 네임태그 가드 (AVAILABLE/REVOKED 거부)
        NameTag tag = nameTagRepository.findByToken(req.nametagToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유효하지 않은 네임태그입니다."));
        if (tag.getStatus() != NameTagStatus.ISSUED || tag.getAttendeeId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "바인딩되지 않았거나 회수된 네임태그입니다.");
        }
        Long attendeeId = tag.getAttendeeId();

        RLock lock = redissonClient.getLock("lock:scan:attendee:" + attendeeId);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!locked) throw new BusinessException(ErrorCode.CONFLICT, "스캔 처리가 지연되고 있습니다.");
            return txTemplate.execute(status -> process(req, tag, attendeeId, scannedByUserId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.CONFLICT, "스캔 처리가 중단되었습니다.");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private ScanResult process(ScanRequest req, NameTag tag, Long attendeeId, Long scannedByUserId) {
        ReservationAttendee attendee = attendeeRepository.findById(attendeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Reservation reservation = reservationRepository.findById(attendee.getReservationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Long exhId = attendee.getExhibitionId();
        int headCount = reservation.getMovementMode() == MovementMode.GROUP ? reservation.getGroupSize() : 1;
        ScanPointType type = req.scanPointType();
        Long pointId = req.scanPointId();
        LocalDateTime now = LocalDateTime.now();

        var openOpt = openStateService.get(exhId, attendeeId);

        // ENTRY/EXIT 자동 판정: 이 지점에 열린 ENTRY가 있으면 EXIT, 없으면 ENTRY
        boolean sameOpen = openOpt.isPresent()
                && openOpt.get().scanPointType() == type
                && java.util.Objects.equals(openOpt.get().scanPointId(), pointId);

        if (sameOpen) {
            long elapsed = now.toEpochSecond(ZoneOffset.UTC) - openOpt.get().entryEpoch();
            if (elapsed < debounceSeconds) {
                return new ScanResult(attendeeId, reservation.getMovementMode().name(), headCount,
                        "DEBOUNCED", false, "직전 스캔과 너무 가까워 무시했습니다.");
            }
            return doExit(exhId, attendeeId, tag, type, pointId, headCount, scannedByUserId, now, reservation);
        }
        return doEntry(exhId, attendeeId, tag, type, pointId, headCount, scannedByUserId, now, openOpt.orElse(null), reservation);
    }

    private ScanResult doEntry(Long exhId, Long attendeeId, NameTag tag, ScanPointType type, Long pointId,
                               int headCount, Long scannedByUserId, LocalDateTime now,
                               OpenStateService.OpenState other, Reservation reservation) {
        boolean autoClosedPrevious = false;
        // 다른 지점이 열려 있으면 자동 종료(NEXT_ENTRY_AUTO)
        if (other != null) {
            autoClosePrevious(exhId, attendeeId, other, now);
            autoClosedPrevious = true;
        }
        // ENTRY visit_log
        visitLogRepository.save(VisitLog.builder()
                .exhibitionId(exhId).attendeeId(attendeeId).nameTagId(tag.getId())
                .scanPointType(type).scanPointId(pointId)
                .scanType(ScanType.ENTRY).headCount(headCount)
                .scannedByUserId(scannedByUserId).manual(false).autoExit(false).scannedAt(now).build());
        // open dwell
        VisitDwell dwell = visitDwellRepository.save(VisitDwell.open(exhId, attendeeId, type, pointId, now, headCount));
        // open 상태 (nameTagId 보존 → 자동 EXIT 합성 시 FK 충족)
        openStateService.put(new OpenStateService.OpenState(exhId, attendeeId, type, pointId,
                now.toEpochSecond(ZoneOffset.UTC), headCount, dwell.getId(), tag.getId()));
        // 혼잡 +head_count → SSE
        broadcast(counterService.applyDelta(exhId, type, pointId, headCount));

        return new ScanResult(attendeeId, reservation.getMovementMode().name(), headCount, "ENTRY",
                autoClosedPrevious,
                autoClosedPrevious ? "이전 지점 미종결 체류를 자동 종료하고 현재 지점 ENTRY를 기록했습니다." : "ENTRY 기록 완료");
    }

    private ScanResult doExit(Long exhId, Long attendeeId, NameTag tag, ScanPointType type, Long pointId,
                              int headCount, Long scannedByUserId, LocalDateTime now, Reservation reservation) {
        visitLogRepository.save(VisitLog.builder()
                .exhibitionId(exhId).attendeeId(attendeeId).nameTagId(tag.getId())
                .scanPointType(type).scanPointId(pointId)
                .scanType(ScanType.EXIT).headCount(headCount)
                .scannedByUserId(scannedByUserId).manual(false).autoExit(false).scannedAt(now).build());
        visitDwellRepository.findFirstByAttendeeIdAndExitAtIsNullOrderByEntryAtDesc(attendeeId)
                .ifPresent(d -> d.close(now, CloseReason.NORMAL_EXIT, false));
        openStateService.remove(exhId, attendeeId);
        broadcast(counterService.applyDelta(exhId, type, pointId, -headCount));
        return new ScanResult(attendeeId, reservation.getMovementMode().name(), headCount, "EXIT", false, "EXIT 기록 완료");
    }

    /** 다른 지점 open 자동 종료(다음 부스 ENTRY). */
    private void autoClosePrevious(Long exhId, Long attendeeId, OpenStateService.OpenState prev, LocalDateTime now) {
        LocalDateTime entryAt = LocalDateTime.ofEpochSecond(prev.entryEpoch(), 0, ZoneOffset.UTC);
        LocalDateTime cap = entryAt.plusSeconds(capSeconds);
        LocalDateTime exitAt = now.isBefore(cap) ? now : cap;
        // 합성 EXIT visit_log (prev.nameTagId()로 FK 충족)
        visitLogRepository.save(VisitLog.builder()
                .exhibitionId(exhId).attendeeId(attendeeId)
                .nameTagId(prev.nameTagId())
                .scanPointType(prev.scanPointType()).scanPointId(prev.scanPointId())
                .scanType(ScanType.EXIT).headCount(prev.headCount())
                .scannedByUserId(null).manual(false).autoExit(true).scannedAt(exitAt).build());
        visitDwellRepository.findFirstByAttendeeIdAndExitAtIsNullOrderByEntryAtDesc(attendeeId)
                .ifPresent(d -> d.close(exitAt, CloseReason.NEXT_ENTRY_AUTO, true));
        broadcast(counterService.applyDelta(exhId, prev.scanPointType(), prev.scanPointId(), -prev.headCount()));
        openStateService.remove(exhId, attendeeId);
    }


    /** 관리자 수동 종료(/visits/open/{dwellId}/close). */
    @Transactional
    public void manualClose(Long dwellId) {
        VisitDwell d = visitDwellRepository.findById(dwellId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!d.isOpen()) throw new BusinessException(ErrorCode.CONFLICT, "이미 종료된 체류입니다.");
        LocalDateTime now = LocalDateTime.now();
        d.close(now, CloseReason.ADMIN_MANUAL, true);
        broadcast(counterService.applyDelta(d.getExhibitionId(), d.getScanPointType(), d.getScanPointId(), -d.getHeadCount()));
        openStateService.remove(d.getExhibitionId(), d.getAttendeeId());
    }

    private void broadcast(CongestionEvent event) {
        congestionRouter.publish(event);
    }
}
