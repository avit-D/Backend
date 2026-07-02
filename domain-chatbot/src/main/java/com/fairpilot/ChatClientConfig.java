package com.fairpilot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClientConfig
 * ────────────────────────────────────────────────────────────────
 * Spring AI 의 ChatClient 를 Bean 으로 등록
 * OllamaChatModel 은 spring-ai-starter-model-ollama 가 자동 구성
 *
 * 모델 교체 시 이 Bean 의 builder 인자만 바꾸면 됨:
 *   OpenAI    → OpenAiChatModel
 *   Anthropic → AnthropicChatModel
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }
}
