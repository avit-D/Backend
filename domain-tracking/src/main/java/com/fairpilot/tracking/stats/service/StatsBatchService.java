package com.fairpilot.tracking.stats.service;

import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.domain.ScanType;
import com.fairpilot.tracking.domain.VisitDwell;
import com.fairpilot.tracking.domain.VisitLog;
import com.fairpilot.tracking.repository.VisitDwellRepository;
import com.fairpilot.tracking.repository.VisitLogRepository;
import com.fairpilot.tracking.stats.domain.StatCongestionHourly;
import com.fairpilot.tracking.stats.domain.StatVisitPoint;
import com.fairpilot.tracking.stats.dto.StatDtos.RebuildResult;
import com.fairpilot.tracking.stats.repository.StatCongestionHourlyRepository;
import com.fairpilot.tracking.stats.repository.StatVisitPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 정형화 배치 (개발자 4번, v2.7 스키마 대응).
 * 멱등: exhibition 단위 DELETE + saveAll 재적재.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsBatchService {

    private final VisitLogRepository visitLogRepository;
    private final VisitDwellRepository visitDwellRepository;
    private final StatVisitPointRepository statVisitPointRepository;
    private final StatCongestionHourlyRepository statCongestionHourlyRepository;

    private record PointHourKey(ScanPointType type, Long pointId, LocalDate date, byte hour) {}
    private record HourKey(LocalDate date, byte hour) {}

    @Transactional
    public RebuildResult rebuild(Long exhibitionId) {
        List<VisitLog> logs = visitLogRepository.findByExhibitionId(exhibitionId);
        List<VisitDwell> dwells = visitDwellRepository.findByExhibitionId(exhibitionId);

        List<VisitLog> entries = logs.stream()
                .filter(l -> l.getScanType() == ScanType.ENTRY
                        && l.getScanPointType() != ScanPointType.GATE)
                .toList();

        Map<PointHourKey, List<VisitLog>> entryByKey = entries.stream()
                .collect(Collectors.groupingBy(l -> new PointHourKey(
                        l.getScanPointType(), l.getScanPointId(),
                        l.getScannedAt().toLocalDate(), (byte) l.getScannedAt().getHour())));

        Map<PointHourKey, List<VisitDwell>> dwellByKey = dwells.stream()
                .filter(d -> d.getExitAt() != null && d.getScanPointType() != ScanPointType.GATE)
                .collect(Collectors.groupingBy(d -> new PointHourKey(
                        d.getScanPointType(), d.getScanPointId(),
                        d.getEntryAt().toLocalDate(), (byte) d.getEntryAt().getHour())));

        Set<PointHourKey> allKeys = new HashSet<>();
        allKeys.addAll(entryByKey.keySet());
        allKeys.addAll(dwellByKey.keySet());

        List<StatVisitPoint> pointStats = new ArrayList<>();
        for (PointHourKey k : allKeys) {
            List<VisitLog> es = entryByKey.getOrDefault(k, List.of());
            int visitCount   = es.size();
            int visitorCount = es.stream().mapToInt(VisitLog::getHeadCount).sum();
            int unique       = (int) es.stream().map(VisitLog::getAttendeeId).distinct().count();

            List<VisitDwell> ds = dwellByKey.getOrDefault(k, List.of());
            long sumDwellSec = ds.stream().mapToLong(VisitDwell::getDwellSeconds).sum();
            int dwellCount   = ds.size();
            int avgDwellSec  = dwellCount == 0 ? 0 : (int) (sumDwellSec / dwellCount);

            pointStats.add(StatVisitPoint.builder()
                    .exhibitionId(exhibitionId)
                    .scanPointType(k.type()).scanPointId(k.pointId())
                    .statDate(k.date()).statHour(k.hour())
                    .visitCount(visitCount).visitorCount(visitorCount).uniqueAttendee(unique)
                    .sumDwellSec(sumDwellSec).dwellCount(dwellCount).avgDwellSec(avgDwellSec)
                    .build());
        }

        statVisitPointRepository.deleteByExhibitionId(exhibitionId);
        statVisitPointRepository.saveAll(pointStats);

        List<VisitLog> allEntries = logs.stream()
                .filter(l -> l.getScanType() == ScanType.ENTRY)
                .toList();

        Map<HourKey, Integer> congestionByHour = new HashMap<>();
        for (VisitLog l : allEntries) {
            HourKey hk = new HourKey(l.getScannedAt().toLocalDate(), (byte) l.getScannedAt().getHour());
            congestionByHour.merge(hk, l.getHeadCount(), Integer::sum);
        }

        List<StatCongestionHourly> hourlyStats = congestionByHour.entrySet().stream()
                .map(en -> StatCongestionHourly.builder()
                        .exhibitionId(exhibitionId)
                        .statDate(en.getKey().date())
                        .statHour(en.getKey().hour())
                        .headCount(en.getValue())
                        .build())
                .toList();

        statCongestionHourlyRepository.deleteByExhibitionId(exhibitionId);
        statCongestionHourlyRepository.saveAll(hourlyStats);

        log.info("stats rebuild exhibition={} pointHour={} hourly={}",
                exhibitionId, pointStats.size(), hourlyStats.size());
        return new RebuildResult(pointStats.size(), hourlyStats.size());
    }
}
