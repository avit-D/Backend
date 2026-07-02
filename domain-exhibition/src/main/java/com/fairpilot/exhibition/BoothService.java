package com.fairpilot.exhibition;

import com.fairpilot.core.auth.TenantGuard;
import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoothService {

    private final BoothRepository boothRepository;
    private final TenantGuard tenantGuard;

    /** 부스 목록 조회 */
    @Transactional(readOnly = true)
    public List<Booth> findAll(Long exhibitionId) {
        tenantGuard.validate(exhibitionId);
        return boothRepository.findAllByExhibitionId(exhibitionId);
    }

    /** 부스 단건 조회 */
    @Transactional(readOnly = true)
    public Booth findById(Long exhibitionId, Long boothId) {
        tenantGuard.validate(exhibitionId);
        return boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "부스를 찾을 수 없습니다."));
    }

    /** 부스 생성 (EXPO_ADMIN 전용) */
    @Transactional
    public Booth create(Long exhibitionId, BoothRequest req) {
        tenantGuard.validateExpoAdmin(exhibitionId);
        Booth booth = Booth.builder()
                .exhibitionId(exhibitionId)
                .exhibitorId(req.exhibitorId())
                .categoryId(req.categoryId())
                .name(req.name())
                .description(req.description())
                .tags(req.tags())
                .posX(req.posX())
                .posY(req.posY())
                .floor(req.floor())
                .build();
        return boothRepository.save(booth);
    }

    /** 부스 삭제 (EXPO_ADMIN 전용) */
    @Transactional
    public void delete(Long exhibitionId, Long boothId) {
        tenantGuard.validateExpoAdmin(exhibitionId);
        Booth booth = findById(exhibitionId, boothId);
        boothRepository.delete(booth);
    }
}