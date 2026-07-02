package com.fairpilot.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Redis 기반 좌석 임시 점유(Hold).
 * 예약 신청 시 reserved_count 는 즉시 차감되지만, 결제 미완료를 대비해
 * Hold 키(TTL) + 만료 인덱스(ZSET)를 관리하여 시간 내 미확정 시 자동 반납한다.
 */
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final StringRedisTemplate redis;

    @Value("${fairpilot.reservation.hold-ttl-seconds:180}")
    private long holdTtlSeconds;

    private static final String HOLD_KEY = "hold:resv:";   // hold:resv:{reservationId} = slotId
    private static final String EXPIRY_ZSET = "hold:expiry"; // member=reservationId, score=expireEpoch

    /** ZRANGEBYSCORE + ZREM 을 Lua로 원자화 — 멀티 인스턴스 중복 처리 방지 */
    @SuppressWarnings("unchecked")
    private static final DefaultRedisScript<List<String>> POP_EXPIRED_SCRIPT;
    static {
        POP_EXPIRED_SCRIPT = new DefaultRedisScript<>();
        POP_EXPIRED_SCRIPT.setScriptText(
            "local m = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2])\n" +
            "if #m > 0 then redis.call('ZREM', KEYS[1], unpack(m)) end\n" +
            "return m"
        );
        POP_EXPIRED_SCRIPT.setResultType((Class<List<String>>) (Class<?>) List.class);
    }

    /** 예약 신청 직후 호출: TTL Hold 등록 + 만료 인덱스 적재. */
    public void registerHold(Long reservationId, Long slotId) {
        long expireAt = Instant.now().getEpochSecond() + holdTtlSeconds;
        redis.opsForValue().set(HOLD_KEY + reservationId, String.valueOf(slotId), Duration.ofSeconds(holdTtlSeconds + 30));
        redis.opsForZSet().add(EXPIRY_ZSET, String.valueOf(reservationId), expireAt);
    }

    /** 결제 확정 시 호출: Hold 해제(만료 대상에서 제거). */
    public void confirmHold(Long reservationId) {
        redis.delete(HOLD_KEY + reservationId);
        redis.opsForZSet().remove(EXPIRY_ZSET, String.valueOf(reservationId));
    }

    /** 스케줄러용: 현재 시각 기준 만료된 Hold(reservationId) 목록을 원자적으로 꺼낸다. */
    public Set<String> popExpiredHolds(long maxCount) {
        long now = Instant.now().getEpochSecond();
        List<String> result = redis.execute(
            POP_EXPIRED_SCRIPT,
            List.of(EXPIRY_ZSET),
            String.valueOf(now),
            String.valueOf(maxCount)
        );
        return result == null ? Set.of() : Set.copyOf(result);
    }

    public Long slotIdOfHold(String reservationId) {
        String v = redis.opsForValue().get(HOLD_KEY + reservationId);
        if (v != null) {
            redis.delete(HOLD_KEY + reservationId);
            return Long.valueOf(v);
        }
        return null;
    }
}
