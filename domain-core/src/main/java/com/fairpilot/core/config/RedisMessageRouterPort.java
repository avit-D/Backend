package com.fairpilot.core.config;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Redis Pub/Sub 메시지 라우터 포트.
 * domain-core가 domain-tracking을 직접 참조하지 않도록
 * 인터페이스를 여기에 정의하고, 구현체(CongestionMessageRouter)는
 * domain-tracking에서 제공한다.
 */
public interface RedisMessageRouterPort extends MessageListener {

    /** 구독할 Redis 채널명 */
    String getChannel();

    @Override
    void onMessage(Message message, byte[] pattern);
}
