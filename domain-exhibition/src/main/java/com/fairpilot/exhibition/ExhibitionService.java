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
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final TenantGuard tenantGuard;

    /** 박람회 목록 조회 (전체 공개) */
    @Transactional(readOnly = true)
    public List<Exhibition> findAll() {
        return exhibitionRepository.findAll();
    }

    /** 박람회 단건 조회 */
    @Transactional(readOnly = true)
    public Exhibition findById(Long id) {
        return exhibitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "박람회를 찾을 수 없습니다."));
    }

    /** 박람회 생성 (PLATFORM_ADMIN 전용) */
    @Transactional
    public Exhibition create(ExhibitionRequest req, Long createdBy) {
        if (exhibitionRepository.existsBySlug(req.slug())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 슬러그입니다.");
        }
        Exhibition exhibition = Exhibition.builder()
                .title(req.title())
                .slug(req.slug())
                .venue(req.venue())
                .address(req.address())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .createdBy(createdBy)
                .build();
        return exhibitionRepository.save(exhibition);
    }

    /** 박람회 삭제 (PLATFORM_ADMIN 전용) */
    @Transactional
    public void delete(Long id) {
        Exhibition exhibition = findById(id);
        exhibitionRepository.delete(exhibition);
    }
}