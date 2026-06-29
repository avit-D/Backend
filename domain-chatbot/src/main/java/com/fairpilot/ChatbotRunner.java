package com.fairpilot;

import com.fairpilot.dto.AssistantDto.*;
import com.fairpilot.service.AssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * ChatbotRunner
 * ────────────────────────────────────────────────────────────────
 * Spring 컨텍스트는 띄우되 Controller 레이어는 로드하지 않음
 *
 * 실행 전 Ollama 가 로컬에서 떠 있어야 합니다.
 *   ollama serve
 *   ollama pull llama3.2
 *
 * 실행:
 *   ./gradlew :domain-chatbot:bootRun
 */
@Slf4j
@SpringBootApplication
public class ChatbotRunner {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotRunner.class, args);
    }

    // ── 시나리오 정의 ─────────────────────────────────────────────
    record Scenario(String label, String question, List<String> candidateBoothIds) {}

    static final List<Scenario> SCENARIOS = List.of(
            new Scenario(
                    "일반 질문 — 후보 풀 전체",
                    "AI 관련 부스 추천해줘",
                    null
            ),
            new Scenario(
                    "카테고리 질문 — 후보 풀 전체",
                    "보안 솔루션 전시하는 곳 어디야?",
                    null
            ),
            new Scenario(
                    "후보 풀 제한 — 정상 케이스",
                    "물류 자동화 부스 알려줘",
                    List.of("BOOTH-001", "BOOTH-002", "BOOTH-003")
            ),
            new Scenario(
                    "후보 풀 제한 — 관련 부스 없는 케이스",
                    "핀테크 결제 관련 부스 추천해줘",
                    List.of("BOOTH-001", "BOOTH-002")  // 핀테크는 BOOTH-006 이라 풀에 없음
            ),
            new Scenario(
                    "할루시네이션 유도 — 존재하지 않는 ID 포함",
                    "AI 비전이랑 스마트팜 부스 비교해줘",
                    List.of("BOOTH-001", "BOOTH-999")  // BOOTH-999 는 DB에 없음
            )
    );

    // ── ApplicationRunner: Spring Bean 주입 후 실행 ───────────────
    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class ScenarioRunner implements ApplicationRunner {

        private final AssistantService assistantService;
        private final ObjectMapper objectMapper;

        @Override
        public void run(ApplicationArguments args) throws Exception {
            System.out.println("═══════════════════════════════════════════════════");
            System.out.println("  Exhibition Chatbot Runner");
            System.out.println("═══════════════════════════════════════════════════\n");

            for (int i = 0; i < SCENARIOS.size(); i++) {
                Scenario scenario = SCENARIOS.get(i);

                System.out.printf("─── 시나리오 %d: %s%n", i + 1, scenario.label());
                System.out.println("질문   : " + scenario.question());
                System.out.println("후보풀 : " + (scenario.candidateBoothIds() == null
                        ? "전체" : scenario.candidateBoothIds()));
                System.out.println();

                try {
                    AskRequest request = buildRequest(scenario);
                    AskResponse response = assistantService.ask(request);

                    System.out.println("▶ 답변:\n" + response.getAnswer());
                    System.out.println();

                    if (!response.getCitedBooths().isEmpty()) {
                        System.out.println("▶ 인용 부스:");
                        response.getCitedBooths().forEach(b ->
                                System.out.printf("  [%s] %s (%s)%n  위치: %s%n  이유: %s%n",
                                        b.getId(), b.getName(), b.getCompany(),
                                        b.getLocation(), b.getRelevanceNote()));
                        System.out.println();
                    }

                    if (!response.getRemovedHallucinatedIds().isEmpty()) {
                        System.out.println("⚠ 제거된 유령 ID: " + response.getRemovedHallucinatedIds());
                        System.out.println();
                    }

                    System.out.println("▶ 메타:");
                    System.out.println("  후보풀 크기  : " + response.getMeta().getCandidatePoolSize());
                    System.out.println("  LLM 선언 ID : " + response.getMeta().getLlmRawBoothIds());
                    System.out.println("  검증된 ID   : " + response.getMeta().getVerifiedBoothIds());
                    System.out.println("  사용 모델   : " + response.getMeta().getModelUsed());

                } catch (Exception e) {
                    log.error("시나리오 실행 오류", e);
                    System.out.println("✗ 오류 발생: " + e.getMessage());
                }

                System.out.println("\n═══════════════════════════════════════════════════\n");
            }
        }

        private AskRequest buildRequest(Scenario scenario) throws Exception {
            var node = objectMapper.createObjectNode();
            node.put("question", scenario.question());
            if (scenario.candidateBoothIds() != null) {
                var arr = node.putArray("candidateBoothIds");
                scenario.candidateBoothIds().forEach(arr::add);
            }
            return objectMapper.treeToValue(node, AskRequest.class);
        }
    }
}