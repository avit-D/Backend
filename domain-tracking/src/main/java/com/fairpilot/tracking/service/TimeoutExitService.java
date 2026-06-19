package com.fairpilot.tracking.service;

import com.fairpilot.tracking.domain.ScanType;
import com.fairpilot.tracking.congestion.service.CongestionCounterService;
import com.fairpilot.tracking.congestion.service.CongestionMessageRouter;
import com.fairpilot.tracking.domain.CloseReason;
import com.fairpilot.tracking.domain.VisitLog;
import com.fairpilot.tracking.repository.VisitDwellRepository;
import com.fairpilot.tracking.repository.VisitLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 60분 경과 미종결 체류 자동 EXIT (TIMEOUT_AUTO). 합성 EXIT visit_log + dwell 마감 + 혼잡 감소.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutExitService {

    private final OpenStateService openStateService;
    private final VisitLogRepository visitLogRepository;
    private final VisitDwellRepository visitDwellRepository;
    private final CongestionCounterService counterService;
    private final CongestionMessageRouter congestionRouter;

    @Transactional
    public void closeTimeout(Long exhibitionId, Long attendeeId) {
        var openOpt = openStateService.get(exhibitionId, attendeeId);
        if (openOpt.isEmpty()) {
            openStateService.remove(exhibitionId, attendeeId);
            return;
        }
        var open = openOpt.get();
        LocalDateTime entryAt = LocalDateTime.ofEpochSecond(open.entryEpoch(), 0, ZoneOffset.UTC);
        LocalDateTime exitAt = entryAt.plusHours(1); // 60분 자동 EXIT

        visitLogRepository.save(VisitLog.builder()
                .exhibitionId(exhibitionId).attendeeId(attendeeId)
                .nameTagId(open.nameTagId())
                .scanPointType(open.scanPointType()).scanPointId(open.scanPointId())
                .scanType(ScanType.EXIT).headCount(open.headCount())
                .scannedByUserId(null).manual(false).autoExit(true).scannedAt(exitAt).build());

        visitDwellRepository.findFirstByAttendeeIdAndExitAtIsNullOrderByEntryAtDesc(attendeeId)
                .ifPresent(d -> d.close(exitAt, CloseReason.TIMEOUT_AUTO, true));

        var event = counterService.applyDelta(exhibitionId, open.scanPointType(), open.scanPointId(), -open.headCount());
        congestionRouter.publish(event);
        openStateService.remove(exhibitionId, attendeeId);
        log.info("auto-exit (timeout) exhibition={} attendee={}", exhibitionId, attendeeId);
    }
}
