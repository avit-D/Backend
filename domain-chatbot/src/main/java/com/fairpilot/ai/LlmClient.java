package com.fairpilot.ai;

import com.fairpilot.OllamaProperties;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import com.fairpilot.dto.AssistantDto.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * LlmClient — Spring AI ChatClient + Ollama 래퍼
 * ────────────────────────────────────────────────────────────────
 * Spring AI 가 제공하는 ChatClient 인터페이스를 사용하므로
 * Ollama → OpenAI / Anthropic 등 다른 모델로 교체 시
 * application.properties 설정 변경만으로 대응 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final OllamaProperties ollamaProperties;

    public LlmRawOutput complete(Prompt prompt) {
        String rawText = chatClient.prompt(prompt)
                .call()
                .content();

        log.debug("[LlmClient] 모델={}, 원시 응답:\n{}",
                ollamaProperties.getModel(), rawText);

        return parseAndValidate(rawText);
    }

    @SuppressWarnings("unchecked")
    private LlmRawOutput parseAndValidate(String raw) {
        String cleaned = raw == null ? "" : raw
                .replaceAll("(?i)```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        try {
            Map<String, Object> parsed = objectMapper.readValue(cleaned, Map.class);

            String answer = parsed.getOrDefault("answer", "").toString();

            List<String> ids = parsed.get("referencedBoothIds") instanceof List<?>
                    ? ((List<?>) parsed.get("referencedBoothIds")).stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .toList()
                    : Collections.emptyList();

            Map<String, String> notes = parsed.get("citationNotes") instanceof Map<?, ?>
                    ? ((Map<?, ?>) parsed.get("citationNotes")).entrySet().stream()
                            .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
                            .collect(java.util.stream.Collectors.toMap(
                                    e -> (String) e.getKey(),
                                    e -> (String) e.getValue()))
                    : Collections.emptyMap();

            return LlmRawOutput.builder()
                    .answer(answer)
                    .referencedBoothIds(ids)
                    .citationNotes(notes)
                    .build();

        } catch (Exception e) {
            log.error("[LlmClient] JSON 파싱 실패. 안전 fallback 반환. raw={}", raw, e);
            return LlmRawOutput.builder()
                    .answer(cleaned.isBlank() ? "응답을 처리할 수 없습니다." : cleaned)
                    .referencedBoothIds(Collections.emptyList())
                    .citationNotes(Collections.emptyMap())
                    .build();
        }
    }
}
