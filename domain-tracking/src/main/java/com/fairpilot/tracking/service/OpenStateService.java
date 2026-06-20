package com.fairpilot.tracking.service;

import com.fairpilot.tracking.domain.ScanPointType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 부스/세션 open 상태(미종결 ENTRY) 관리.
 *  - open:{exhId}:attendee:{attendeeId}  → 현재 열린 지점 JSON
 *  - open_index (global ZSET)            → member="{exhId}:{attendeeId}", score=entry epoch (60분 스위퍼용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenStateService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final String OPEN = "open:";
    private static final String OPEN_INDEX = "open_index";
    private static final Duration OPEN_TTL = Duration.ofHours(3);

    public record OpenState(Long exhibitionId, Long attendeeId, ScanPointType scanPointType,
                            Long scanPointId, long entryEpoch, int headCount, Long dwellId,
                            Long nameTagId) {}

    public Optional<OpenState> get(Long exhibitionId, Long attendeeId) {
        String json = redis.opsForValue().get(openKey(exhibitionId, attendeeId));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, OpenState.class));
        } catch (Exception e) {
            log.error("open-state parse failed", e);
            return Optional.empty();
        }
    }

    public void put(OpenState state) {
        try {
            redis.opsForValue().set(openKey(state.exhibitionId(), state.attendeeId()),
                    objectMapper.writeValueAsString(state), OPEN_TTL);
            redis.opsForZSet().add(OPEN_INDEX, member(state.exhibitionId(), state.attendeeId()), state.entryEpoch());
        } catch (Exception e) {
            log.error("open-state put failed", e);
        }
    }

    public void remove(Long exhibitionId, Long attendeeId) {
        redis.delete(openKey(exhibitionId, attendeeId));
        redis.opsForZSet().remove(OPEN_INDEX, member(exhibitionId, attendeeId));
    }

    /** 60분 스위퍼: entry epoch <= threshold 인 "{exhId}:{attendeeId}" 목록을 원자적으로 꺼낸다. */
    public Set<String> popExpired(long thresholdEpoch, long maxCount) {
        Set<String> expired = redis.opsForZSet().rangeByScore(OPEN_INDEX, 0, thresholdEpoch, 0, maxCount);
        if (expired != null && !expired.isEmpty()) {
            redis.opsForZSet().remove(OPEN_INDEX, expired.toArray());
        }
        return expired;
    }

    private String openKey(Long exhibitionId, Long attendeeId) {
        return OPEN + exhibitionId + ":attendee:" + attendeeId;
    }
    private String member(Long exhibitionId, Long attendeeId) {
        return exhibitionId + ":" + attendeeId;
    }
}
