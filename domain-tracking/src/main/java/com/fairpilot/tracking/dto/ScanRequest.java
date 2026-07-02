package com.fairpilot.tracking.dto;

import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.domain.ScanType;
import jakarta.validation.constraints.NotNull;

/** 무인 셀프 스캔 요청. scanType 미지정 시 서버가 open 상태로 자동 판정. */
public record ScanRequest(
        @NotNull String nametagToken,
        @NotNull ScanPointType scanPointType,
        Long scanPointId,
        ScanType scanType   // optional
) {}
