package com.fairpilot.core.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final String PREFIX = "refresh:";

    /** Refresh Token 발급 및 Redis 저장 */
    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue()
                .set(PREFIX + token, String.valueOf(userId), REFRESH_TTL);
        return token;
    }

    /** Refresh Token 검증 → userId 반환 */
    public Long validate(String token) {
        String userId = redisTemplate.opsForValue().get(PREFIX + token);
        if (userId == null) throw new IllegalArgumentException("유효하지 않은 Refresh Token");
        return Long.valueOf(userId);
    }

    /** Refresh Token 삭제 (로그아웃) */
    public void revoke(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}