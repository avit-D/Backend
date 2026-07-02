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
public class SessionService {

    private final SessionRepository sessionRepository;
    private final TenantGuard tenantGuard;

    /** 세션 목록 조회 */
    @Transactional(readOnly = true)
    public List<Session> findAll(Long exhibitionId) {
        tenantGuard.validate(exhibitionId);
        return sessionRepository.findAllByExhibitionId(exhibitionId);
    }

    /** 세션 단건 조회 */
    @Transactional(readOnly = true)
    public Session findById(Long exhibitionId, Long sessionId) {
        tenantGuard.validate(exhibitionId);
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));
    }

    /** 세션 생성 (EXPO_ADMIN 전용) */
    @Transactional
    public Session create(Long exhibitionId, SessionRequest req) {
        tenantGuard.validateExpoAdmin(exhibitionId);
        Session session = Session.builder()
                .exhibitionId(exhibitionId)
                .hostExhibitorId(req.hostExhibitorId())
                .title(req.title())
                .description(req.description())
                .location(req.location())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .capacity(req.capacity())
                .build();
        return sessionRepository.save(session);
    }

    /** 세션 삭제 (EXPO_ADMIN 전용) */
    @Transactional
    public void delete(Long exhibitionId, Long sessionId) {
        tenantGuard.validateExpoAdmin(exhibitionId);
        Session session = findById(exhibitionId, sessionId);
        sessionRepository.delete(session);
    }
}