package com.fairpilot.core.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    // PlatformAdminAccountController에서 호출 — is_deleted 무관하게 이메일 중복 체크
    boolean existsByEmail(String email);

    Optional<User> findBySocialProviderAndSocialProviderId(
            SocialProvider provider, String providerId);

    /** 초대 토큰으로 유저 조회 */
    Optional<User> findByInviteToken(String inviteToken);

    /** 만료된 초대 토큰 일괄 정리 배치용 */
    @Modifying
    @Query("UPDATE User u SET u.inviteToken = NULL, u.inviteExpiresAt = NULL " +
            "WHERE u.inviteExpiresAt IS NOT NULL " +
            "AND u.inviteExpiresAt < :now " +
            "AND u.accountStatus = 'INVITED'")
    int clearExpiredInviteTokens(@Param("now") LocalDateTime now);
}