package com.fairpilot.tracking.congestion.service;

import com.fairpilot.core.config.RedisMessageRouterPort;
import com.fairpilot.tracking.congestion.dto.CongestionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 다중 인스턴스 SSE fan-out.
 * - publish(): 혼잡 이벤트를 Redis 채널로 발행 → 모든 인스턴스가 수신.
 * - onMessage(): 수신한 이벤트를 자기 인스턴스의 로컬 SSE 구독자에게 브로드캐스트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CongestionMessageRouter implements RedisMessageRouterPort {

    public static final String CHANNEL = "congestion:events";

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    private final StringRedisTemplate redis;
    private final CongestionSseService sseService;
    private final ObjectMapper objectMapper;

    public void publish(CongestionEvent event) {
        try {
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("congestion publish failed", e);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            CongestionEvent event = objectMapper.readValue(body, CongestionEvent.class);
            sseService.broadcast(event);
        } catch (Exception e) {
            log.error("congestion onMessage failed", e);
        }
    }
}
