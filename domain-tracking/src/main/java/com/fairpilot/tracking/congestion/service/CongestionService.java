package com.fairpilot.tracking.congestion.service;

import com.fairpilot.tracking.congestion.dto.CongestionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 혼잡도 조회 파사드. 실시간 갱신은 ScanProcessingService → CongestionCounterService → SSE 경로로 처리된다. */
@Service
@RequiredArgsConstructor
public class CongestionService {

    private final CongestionCounterService counterService;

    public List<CongestionEvent> snapshot(Long exhibitionId) {
        return counterService.snapshot(exhibitionId);
    }
}
