package com.fairpilot.tracking.service;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.tracking.domain.NameTag;
import com.fairpilot.tracking.domain.NameTagStatus;
import com.fairpilot.tracking.dto.NametagBatchRequest;
import com.fairpilot.tracking.dto.NametagStockResponse;
import com.fairpilot.tracking.repository.NameTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 네임태그 재고 관리 (기획안 §6.4 — 사전 인쇄·배포 관리).
 */
@Service
@RequiredArgsConstructor
public class NametagService {

    private final NameTagRepository nameTagRepo;

    /** 배치 생성 — AVAILABLE N개 */
    @Transactional
    public List<NametagStockResponse> batchCreate(Long exhibitionId, NametagBatchRequest req) {
        List<NameTag> tags = NameTag.batchCreate(exhibitionId, req.count());
        return nameTagRepo.saveAll(tags).stream()
                .map(NametagStockResponse::of)
                .collect(Collectors.toList());
    }

    /** 재고 현황 조회 (status 필터 선택) */
    @Transactional(readOnly = true)
    public List<NametagStockResponse> getStock(Long exhibitionId, String statusFilter) {
        List<NameTag> tags;
        if (statusFilter != null && !statusFilter.isBlank()) {
            NameTagStatus status = parseStatus(statusFilter);
            tags = nameTagRepo.findByExhibitionIdAndStatus(exhibitionId, status);
        } else {
            tags = nameTagRepo.findByExhibitionId(exhibitionId);
        }
        return tags.stream().map(NametagStockResponse::of).collect(Collectors.toList());
    }

    /** 재고 요약 (AVAILABLE / ISSUED / REVOKED 각 건수) */
    @Transactional(readOnly = true)
    public StockSummary getStockSummary(Long exhibitionId) {
        long available = nameTagRepo.countByExhibitionIdAndStatus(exhibitionId, NameTagStatus.AVAILABLE);
        long issued    = nameTagRepo.countByExhibitionIdAndStatus(exhibitionId, NameTagStatus.ISSUED);
        long revoked   = nameTagRepo.countByExhibitionIdAndStatus(exhibitionId, NameTagStatus.REVOKED);
        return new StockSummary(available, issued, revoked, available + issued + revoked);
    }

    public record StockSummary(long available, long issued, long revoked, long total) {}

    private NameTagStatus parseStatus(String s) {
        try {
            return NameTagStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 status: " + s);
        }
    }
}
