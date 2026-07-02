package com.fairpilot.tracking.dto;

import com.fairpilot.tracking.domain.ScanPointType;

/** GET /api/exhibitor/scan-points 응답 단위. */
public record ScanPointResponse(ScanPointType scanPointType, Long scanPointId) {}
