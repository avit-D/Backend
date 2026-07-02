package com.fairpilot.tracking.service;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.reservation.repository.ReservationAttendeeRepository;
import com.fairpilot.reservation.repository.ReservationRepository;
import com.fairpilot.tracking.domain.VisitDwell;
import com.fairpilot.tracking.domain.VisitLog;
import com.fairpilot.tracking.dto.VisitReportResponse;
import com.fairpilot.tracking.dto.VisitReportResponse.DwellEntry;
import com.fairpilot.tracking.dto.VisitReportResponse.LogEntry;
import com.fairpilot.tracking.repository.VisitDwellRepository;
import com.fairpilot.tracking.repository.VisitLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 참관객 본인 동선 사후 리포트.
 * userId → attendeeId 파싱 → visit_log·visit_dwell 고속 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitReportService {

    private final ReservationRepository reservationRepository;
    private final ReservationAttendeeRepository attendeeRepository;
    private final VisitLogRepository visitLogRepository;
    private final VisitDwellRepository visitDwellRepository;

    public VisitReportResponse report(Long exhibitionId, Long userId) {
        // 1) userId + exhibitionId → attendeeId 파싱
        Long reservationId = reservationRepository
                .findByUserIdAndExhibitionId(userId, exhibitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 박람회 예약 이력이 없습니다."))
                .getId();

        Long attendeeId = attendeeRepository
                .findByReservationId(reservationId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "참석자 정보를 찾을 수 없습니다."))
                .getId();

        // 2) 원천 동선 이력 조회
        List<VisitLog> logs = visitLogRepository.findByAttendeeIdOrderByScannedAtAsc(attendeeId);
        List<VisitDwell> dwells = visitDwellRepository.findByAttendeeIdOrderByEntryAtAsc(attendeeId);

        long totalDwellSec = dwells.stream()
                .mapToLong(VisitDwell::getDwellSeconds)
                .sum();

        long visitedPoints = logs.stream()
                .map(l -> l.getScanPointType().name() + ":" + l.getScanPointId())
                .distinct()
                .count();

        return new VisitReportResponse(
                exhibitionId,
                attendeeId,
                logs.stream().map(LogEntry::from).toList(),
                dwells.stream().map(DwellEntry::from).toList(),
                totalDwellSec,
                (int) visitedPoints
        );
    }
}
