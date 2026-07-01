package com.fairpilot.ai;

import com.fairpilot.exhibition.Booth;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PromptBuilder
 * ────────────────────────────────────────────────────────────────
 * 변경점: 부스 ID 가 임의의 Long PK 라서 LLM 이 안전하게 다시 인용할 수 있도록
 *         [BOOTH:{id}] 토큰 형태로 노출. 회사명은 Booth 엔티티에 없으므로 제외하고
 *         name/description/tags/floor 만 컨텍스트에 포함.
 */
@Component
public class PromptBuilder {

    public Prompt build(String question, List<Booth> candidateBooths) {
        return new Prompt(List.of(
                new SystemMessage(systemText()),
                new UserMessage(userText(question, candidateBooths))
        ));
    }

    private String systemText() {
        return """
                당신은 전시회 안내 도우미 AI입니다.

                ## 핵심 규칙 (반드시 준수)
                1. 오직 아래 [부스 데이터베이스]에 있는 부스 정보만 사용하세요.
                2. 부스를 인용할 때는 반드시 [BOOTH:숫자] 형태의 토큰을 그대로 사용하세요.
                   예: [BOOTH:1024] — 직접 숫자를 만들거나 변형하지 마세요.
                3. 데이터베이스에 없는 부스, 회사명, 제품을 절대 창작하지 마세요.
                4. 모르는 정보는 "해당 정보는 제공된 부스 데이터에 없습니다"라고 답하세요.
                5. 응답은 반드시 아래 JSON 형식으로만 반환하세요. 마크다운 코드블록도 제외하세요.

                ## 응답 JSON 형식
                {
                  "answer": "참관객을 위한 친절한 한국어 답변 (부스 인용 시 [BOOTH:id] 토큰 사용)",
                  "referencedBoothIds": ["실제로 답변에 인용한 부스 ID(숫자 문자열) — 부스 데이터베이스에 있는 것만"],
                  "citationNotes": {
                    "1024": "이 부스를 인용한 구체적 이유"
                  }
                }
                """;
    }

    private String userText(String question, List<Booth> candidateBooths) {
        String boothContext = candidateBooths.stream()
                .map(b -> """
                        [BOOTH:%d] %s
                          - 태그: %s
                          - 설명: %s
                          - 위치: %s층
                        """.formatted(
                        b.getId(), b.getName(),
                        b.getTags() == null ? "없음" : b.getTags(),
                        b.getDescription() == null ? "설명 없음" : b.getDescription(),
                        b.getFloor() == null ? "?" : b.getFloor()
                ))
                .collect(Collectors.joining("\n"));

        return """
                ## 부스 데이터베이스 (총 %d개 — 이 목록 외 부스는 존재하지 않음)

                %s

                ---

                ## 참관객 질문
                %s

                위 부스 데이터베이스만 참조하여 JSON 형식으로 답변하세요.
                """.formatted(candidateBooths.size(), boothContext, question);
    }
}