package com.fairpilot.reservation.service;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis Sorted Set 가상 대기열(트래픽 폭증 완화).
 * 진입 순번이 활성 슬롯 이내이면 ALLOWED 토큰을 발급하고, ALLOWED 보유자만 예약을 진행한다.
 */
@Service
@RequiredArgsConstructor
public class QueueService {

    private final StringRedisTemplate redis;

    @Value("${fairpilot.reservation.queue.allowed-ttl-seconds:120}")
    private long allowedTtlSeconds;

    private static final String QUEUE = "queue:slot:";       // zset member=userId score=enterTime
    private static final String ALLOWED = "queue:allowed:";  // allowed:slot:{slotId}:user:{userId}

    public record QueuePosition(long position, boolean allowed) {}

    /** 대기열 진입(중복 진입 시 기존 순번 유지). */
    public QueuePosition enter(Long slotId, Long userId, int activeSlots) {
        String key = QUEUE + slotId;
        String member = String.valueOf(userId);
        redis.opsForZSet().addIfAbsent(key, member, System.currentTimeMillis());
        return status(slotId, userId, activeSlots);
    }

    public QueuePosition status(Long slotId, Long userId, int activeSlots) {
        String key = QUEUE + slotId;
        String member = String.valueOf(userId);
        Long rank = redis.opsForZSet().rank(key, member);
        if (rank == null) {
            return new QueuePosition(-1, false);
        }
        long position = rank + 1; // 1-based
        boolean allowed = position <= activeSlots;
        if (allowed) {
            redis.opsForValue().set(allowedKey(slotId, userId), "1", Duration.ofSeconds(allowedTtlSeconds));
        }
        return new QueuePosition(position, allowed);
    }

    /** 예약 신청 전 ALLOWED 토큰 검증. 큐 운영이 비활성(활성 슬롯=무제한)이면 통과. */
    public void requireAllowed(Long slotId, Long userId) {
        Boolean ok = redis.hasKey(allowedKey(slotId, userId));
        if (Boolean.FALSE.equals(ok)) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_ALLOWED);
        }
    }

    /** 예약 확정/취소 시 대기열에서 제거(후순위 자동 승급). */
    public void leave(Long slotId, Long userId) {
        redis.opsForZSet().remove(QUEUE + slotId, String.valueOf(userId));
        redis.delete(allowedKey(slotId, userId));
    }

    private String allowedKey(Long slotId, Long userId) {
        return ALLOWED + slotId + ":user:" + userId;
    }
}
