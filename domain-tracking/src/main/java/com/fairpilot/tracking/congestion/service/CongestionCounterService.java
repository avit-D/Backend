package com.fairpilot.tracking.congestion.service;

import com.fairpilot.tracking.congestion.domain.CongestionLevel;
import com.fairpilot.tracking.domain.ScanPointType;
import com.fairpilot.tracking.congestion.dto.CongestionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis 실시간 점유 카운터. v2.4: 증감 단위는 head_count(GROUP=group_size).
 * 키: congestion:{exhId}:{type}:{pointId}
 */
@Service
@RequiredArgsConstructor
public class CongestionCounterService {

    private final StringRedisTemplate redis;

    @Value("${fairpilot.congestion.level-medium:30}") private long medium;
    @Value("${fairpilot.congestion.level-high:60}")   private long high;
    @Value("${fairpilot.congestion.level-full:100}")  private long full;

    private static final String COUNT = "congestion:";
    private static final String POINTS = "congestion:points:";

    /** head_count 만큼 증감(EXIT는 음수). 0 미만 방지. 변경된 혼잡 이벤트 반환. */
    public CongestionEvent applyDelta(Long exhibitionId, ScanPointType type, Long pointId, int delta) {
        String key = countKey(exhibitionId, type, pointId);
        Long v = redis.opsForValue().increment(key, delta);
        long count = v == null ? 0 : v;
        if (count < 0) { redis.opsForValue().set(key, "0"); count = 0; }
        redis.opsForSet().add(POINTS + exhibitionId, type.name() + ":" + pointId);
        return new CongestionEvent(exhibitionId, type, pointId, count, level(count));
    }

    public List<CongestionEvent> snapshot(Long exhibitionId) {
        List<CongestionEvent> result = new ArrayList<>();
        Set<String> points = redis.opsForSet().members(POINTS + exhibitionId);
        if (points == null) return result;
        for (String p : points) {
            String[] parts = p.split(":");
            ScanPointType type = ScanPointType.valueOf(parts[0]);
            Long pointId = Long.valueOf(parts[1]);
            String v = redis.opsForValue().get(countKey(exhibitionId, type, pointId));
            long count = v == null ? 0 : Long.parseLong(v);
            result.add(new CongestionEvent(exhibitionId, type, pointId, count, level(count)));
        }
        return result;
    }

    private CongestionLevel level(long count) {
        if (count >= full) return CongestionLevel.FULL;
        if (count >= high) return CongestionLevel.HIGH;
        if (count >= medium) return CongestionLevel.MEDIUM;
        return CongestionLevel.LOW;
    }

    private String countKey(Long exhId, ScanPointType type, Long pointId) {
        return COUNT + exhId + ":" + type.name() + ":" + pointId;
    }
}
