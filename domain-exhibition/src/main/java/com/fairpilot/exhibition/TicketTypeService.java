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
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TenantGuard tenantGuard;

    /** 티켓타입 목록 조회 */
    @Transactional(readOnly = true)
    public List<TicketType> findAll(Long exhibitionId) {
        tenantGuard.validate(exhibitionId);
        return ticketTypeRepository.findAllByExhibitionId(exhibitionId);
    }

    /** 티켓타입 단건 조회 */
    @Transactional(readOnly = true)
    public TicketType findById(Long exhibitionId, Long ticketTypeId) {
        tenantGuard.validate(exhibitionId);
        return ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "티켓 타입을 찾을 수 없습니다."));
    }

    /** 티켓타입 생성 (EXPO_ADMIN 전용) */
    @Transactional
    public TicketType create(Long exhibitionId, TicketTypeRequest req) {
        tenantGuard.validateExpoAdmin(exhibitionId);
        TicketType ticketType = TicketType.builder()
                .exhibitionId(exhibitionId)
                .name(req.name())
                .price(req.price())
                .quota(req.quota())
                .build();
        return ticketTypeRepository.save(ticketType);
    }

    /** 티켓타입 삭제 (EXPO_ADMIN 전용) */
    @Transactional
    public void delete(Long exhibitionId, Long ticketTypeId) {
        tenantGuard.validateExpoAdmin(exhibitionId);
        TicketType ticketType = findById(exhibitionId, ticketTypeId);
        ticketTypeRepository.delete(ticketType);
    }
}