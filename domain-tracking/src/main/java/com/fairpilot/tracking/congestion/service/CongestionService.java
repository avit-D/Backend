package com.fairpilot.tracking.congestion.service;

import com.fairpilot.tracking.congestion.dto.CongestionEvent;
import com.fairpilot.tracking.domain.ScanPointType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 혼잡도 조회 파사드. 실시간 갱신은 ScanProcessingService → CongestionCounterService → SSE 경로로 처리된다. */
@Service
@RequiredArgsConstructor
public class CongestionService {

    private final CongestionCounterService counterService;

    public List<CongestionEvent> snapshot(Long exhibitionId) {
        return counterService.snapshot(exhibitionId);
    }

    public Map<Long, String> getBoothCongestionMap(Long exhibitionId) {
        return snapshot(exhibitionId).stream()
                .filter(e -> e.scanPointType() == ScanPointType.BOOTH)
                .collect(Collectors.toMap(
                        CongestionEvent::scanPointId,
                        e -> e.level().name()
                ));
    }
}
