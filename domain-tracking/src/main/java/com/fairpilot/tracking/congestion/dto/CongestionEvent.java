package com.fairpilot.tracking.congestion.dto;

import com.fairpilot.tracking.congestion.domain.CongestionLevel;
import com.fairpilot.tracking.domain.ScanPointType;

/** SSE 로 프론트(1번)에 푸시되는 혼잡도 변경 이벤트. */
public record CongestionEvent(Long exhibitionId, ScanPointType scanPointType,
                              Long scanPointId, long count, CongestionLevel level) {}
