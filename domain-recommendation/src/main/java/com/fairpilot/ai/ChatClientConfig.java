package com.fairpilot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            당신은 축제 부스 동선 추천 AI입니다.
            유저의 관심사, 부스 위치(X/Y 좌표), 혼잡도를 종합적으로 고려하여
            최적의 방문 순서를 추천해야 합니다.
            반드시 요청된 JSON 형식으로만 응답하세요. 다른 설명 없이 JSON만 출력하세요.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}