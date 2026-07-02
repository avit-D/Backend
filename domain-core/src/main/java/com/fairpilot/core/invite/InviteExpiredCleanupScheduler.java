package com.fairpilot.core.invite;

import com.fairpilot.core.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InviteExpiredCleanupScheduler {

    private final UserRepository userRepository;

    /**
     * 만료 초대 토큰 정리 배치
     * 매일 새벽 3시 실행
     * 유저 레코드는 삭제하지 않음 (재초대 가능성 유지)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void clearExpiredInviteTokens() {
        int cleared = userRepository.clearExpiredInviteTokens(LocalDateTime.now());
        log.info("만료 초대 토큰 정리 완료: {}건", cleared);
    }
}