package com.fairpilot.tracking.dto;

import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.domain.ScanType;
import com.fairpilot.tracking.domain.VisitDwell;
import com.fairpilot.tracking.domain.VisitLog;

import java.time.LocalDateTime;
import java.util.List;

/** 참관객 본인 동선 사후 리포트 응답. */
public record VisitReportResponse(
        Long exhibitionId,
        Long attendeeId,
        List<LogEntry> visitLogs,
        List<DwellEntry> dwells,
        long totalDwellSeconds,
        int visitedPointCount
) {
    public record LogEntry(ScanPointType scanPointType, Long scanPointId,
                           ScanType scanType, LocalDateTime scannedAt, int headCount) {
        public static LogEntry from(VisitLog l) {
            return new LogEntry(l.getScanPointType(), l.getScanPointId(),
                    l.getScanType(), l.getScannedAt(), l.getHeadCount());
        }
    }

    public record DwellEntry(ScanPointType scanPointType, Long scanPointId,
                             LocalDateTime entryAt, LocalDateTime exitAt, int dwellSeconds) {
        public static DwellEntry from(VisitDwell d) {
            return new DwellEntry(d.getScanPointType(), d.getScanPointId(),
                    d.getEntryAt(), d.getExitAt(), d.getDwellSeconds());
        }
    }
}
