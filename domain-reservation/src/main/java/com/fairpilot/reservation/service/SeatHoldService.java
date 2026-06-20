package com.fairpilot.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
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
        Set<String> expired = redis.opsForZSet().rangeByScore(EXPIRY_ZSET, 0, now, 0, maxCount);
        if (expired != null && !expired.isEmpty()) {
            redis.opsForZSet().remove(EXPIRY_ZSET, expired.toArray());
        }
        return expired;
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
