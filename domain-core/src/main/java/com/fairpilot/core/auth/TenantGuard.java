package com.fairpilot.core.auth;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantGuard {

    @PersistenceContext
    private EntityManager em;

    /**
     * 현재 로그인 유저가 해당 exhibition에 접근 가능한지 검증.
     * PLATFORM_ADMIN → 무조건 통과
     * EXPO_ADMIN     → exhibition_admin 테이블 확인
     * STAFF          → exhibition_staff 테이블 확인
     * EXHIBITOR      → exhibitor 테이블 확인
     * VISITOR        → 그냥 통과 (읽기 전용)
     */
    public void validate(Long exhibitionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");

        switch (role) {
            case "PLATFORM_ADMIN", "VISITOR" -> { /* 통과 */ }
            case "EXPO_ADMIN" -> checkTable(
                    "SELECT 1 FROM exhibition_admin WHERE exhibition_id=:eid AND user_id=:uid",
                    exhibitionId, userId);
            case "STAFF" -> checkTable(
                    "SELECT 1 FROM exhibition_staff WHERE exhibition_id=:eid AND user_id=:uid",
                    exhibitionId, userId);
            case "EXHIBITOR" -> checkTable(
                    "SELECT 1 FROM exhibitor WHERE exhibition_id=:eid AND account_user_id=:uid",
                    exhibitionId, userId);
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * EXPO_ADMIN 전용 — 부스/세션 수정 권한 (EXHIBITOR 완전 배제)
     */
    public void validateExpoAdmin(Long exhibitionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");

        if (role.equals("PLATFORM_ADMIN")) return;

        if (!role.equals("EXPO_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        checkTable(
                "SELECT 1 FROM exhibition_admin WHERE exhibition_id=:eid AND user_id=:uid",
                exhibitionId, userId);
    }

    private void checkTable(String sql, Long exhibitionId, Long userId) {
        try {
            em.createNativeQuery(sql)
                    .setParameter("eid", exhibitionId)
                    .setParameter("uid", userId)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}