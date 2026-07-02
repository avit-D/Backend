package com.fairpilot.tracking.scheduler;

import com.fairpilot.tracking.service.TimeoutExitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * 60분 자동 EXIT 스위퍼. open_index(ZSET)에서 entry_at <= now-3600초 대상을 찾아 자동 종료한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoExitSweeper {

    private final com.fairpilot.tracking.service.OpenStateService openStateService;
    private final TimeoutExitService timeoutExitService;

    @Scheduled(fixedDelayString = "${fairpilot.congestion.auto-exit-sweep-ms:60000}")
    public void sweep() {
        long threshold = Instant.now().getEpochSecond() - 3600;
        Set<String> expired = openStateService.popExpired(threshold, 500);
        if (expired == null || expired.isEmpty()) return;
        for (String member : expired) {
            try {
                String[] parts = member.split(":");
                Long exhId = Long.valueOf(parts[0]);
                Long attendeeId = Long.valueOf(parts[1]);
                timeoutExitService.closeTimeout(exhId, attendeeId);
            } catch (Exception e) {
                log.error("auto-exit sweep failed member={}", member, e);
            }
        }
        log.info("auto-exit sweeper closed {} stale visits", expired.size());
    }
}
