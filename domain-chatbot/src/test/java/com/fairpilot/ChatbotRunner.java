package com.fairpilot;

import com.fairpilot.exhibition.BoothRepository;
import com.fairpilot.OllamaProperties;
import com.fairpilot.dto.AssistantDto.*;
import com.fairpilot.ai.CitationAssembler;
import com.fairpilot.ai.HallucinationGuard;
import com.fairpilot.ai.LlmClient;
import com.fairpilot.ai.PromptBuilder;
import com.fairpilot.repository.MockBoothRepository;
import com.fairpilot.service.AssistantService;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * ChatbotRunner
 * ────────────────────────────────────────────────────────────────
 * @SpringBootApplication / 컴포넌트 스캔을 전혀 쓰지 않음.
 * domain-chatbot 이 다른 도메인 모듈(보안, Redis 등)에 묶여 있어
 * Auto-Configuration 이 무관한 Bean(JWT, Redis 등)까지 같이 띄우려다
 * BeanCreationException 이 발생하는 문제를 원천 차단.
 *
 * 대신 AnnotationConfigApplicationContext 로 챗봇 파이프라인에
 * 필요한 Bean만 직접(수동) 등록하여 실행.
 *
 * 사전 준비:
 *   ollama serve
 *   ollama pull llama3.2
 *
 * 실행:
 *   ./gradlew :domain-chatbot:test --tests ChatbotRunner
 *   또는 IDE 에서 main() 직접 실행
 */
public class ChatbotRunner {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String MODEL = "llama3.2";
    private static final Long EXHIBITION_ID = 100L;

    record Scenario(String label, Long exhibitionId, String question, List<Long> candidateBoothIds) {}

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario(
                    "일반 질문 — 후보 풀 전체",
                    EXHIBITION_ID,
                    "AI 관련 부스 추천해줘",
                    null
            ),
            new Scenario(
                    "카테고리 질문 — 후보 풀 전체",
                    EXHIBITION_ID,
                    "보안 솔루션 전시하는 곳 어디야?",
                    null
            ),
            new Scenario(
                    "후보 풀 제한 — 정상 케이스",
                    EXHIBITION_ID,
                    "물류 자동화 부스 알려줘",
                    List.of(1001L, 1002L, 1003L)
            ),
            new Scenario(
                    "후보 풀 제한 — 관련 부스 없는 케이스",
                    EXHIBITION_ID,
                    "핀테크 결제 관련 부스 추천해줘",
                    List.of(1001L, 1002L)
            ),
            new Scenario(
                    "할루시네이션 유도 — 존재하지 않는 ID 포함",
                    EXHIBITION_ID,
                    "AI 비전이랑 스마트팜 부스 비교해줘",
                    List.of(1001L, 9999L)
            )
    );

    public static void main(String[] args) throws Exception {
        // ── Spring 컨텍스트: 필요한 Bean만 수동 등록 ──────────────────
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {

            ctx.registerBean(BoothRepository.class, MockBoothRepository::new);
            ctx.registerBean(HallucinationGuard.class,
                    () -> new HallucinationGuard(ctx.getBean(BoothRepository.class)));
            ctx.registerBean(PromptBuilder.class, PromptBuilder::new);
            ctx.registerBean(CitationAssembler.class, CitationAssembler::new);
            ctx.registerBean(ObjectMapper.class, JsonMapper::shared);

            ctx.registerBean(OllamaProperties.class, () -> {
                OllamaProperties props = new OllamaProperties();
                props.setModel(MODEL);
                return props;
            });

            ctx.registerBean(ChatClient.class, () -> {
                OllamaApi ollamaApi = OllamaApi.builder().build();
                OllamaChatModel chatModel = OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .options(OllamaChatOptions.builder()
                                .model(MODEL)
                                .temperature(0.3)
                                .numPredict(1500)
                                .build())
                        .build();
                return ChatClient.builder(chatModel).build();
            });

            ctx.registerBean(LlmClient.class, () -> new LlmClient(
                    ctx.getBean(ChatClient.class),
                    ctx.getBean(ObjectMapper.class),
                    ctx.getBean(OllamaProperties.class)
            ));

            ctx.registerBean(AssistantService.class, () -> new AssistantService(
                    ctx.getBean(BoothRepository.class),
                    ctx.getBean(HallucinationGuard.class),
                    ctx.getBean(PromptBuilder.class),
                    ctx.getBean(LlmClient.class),
                    ctx.getBean(CitationAssembler.class),
                    ctx.getBean(OllamaProperties.class)
            ));

            ctx.refresh();

            runScenarios(ctx.getBean(AssistantService.class), ctx.getBean(ObjectMapper.class));
        }
    }

    private static void runScenarios(AssistantService assistantService, ObjectMapper objectMapper) throws Exception {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  Exhibition Chatbot Runner (수동 Bean 등록, Mock Repository)");
        System.out.println("═══════════════════════════════════════════════════\n");

        for (int i = 0; i < SCENARIOS.size(); i++) {
            Scenario scenario = SCENARIOS.get(i);

            System.out.printf("─── 시나리오 %d: %s%n", i + 1, scenario.label());
            System.out.println("박람회ID : " + scenario.exhibitionId());
            System.out.println("질문     : " + scenario.question());
            System.out.println("후보풀   : " + (scenario.candidateBoothIds() == null
                    ? "전체" : scenario.candidateBoothIds()));
            System.out.println();

            try {
                AskRequest request = buildRequest(scenario, objectMapper);
                AskResponse response = assistantService.ask(request);

                System.out.println("▶ 답변:\n" + response.getAnswer());
                System.out.println();

                if (!response.getCitedBooths().isEmpty()) {
                    System.out.println("▶ 인용 부스:");
                    response.getCitedBooths().forEach(b ->
                            System.out.printf("  [%d] %s — %s%n",
                                    b.getId(), b.getName(), b.getRelevanceNote()));
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
                System.out.println("✗ 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("\n═══════════════════════════════════════════════════\n");
        }
    }

    private static AskRequest buildRequest(Scenario scenario, ObjectMapper objectMapper) throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("exhibitionId", scenario.exhibitionId());
        node.put("question", scenario.question());
        if (scenario.candidateBoothIds() != null) {
            var arr = node.putArray("candidateBoothIds");
            scenario.candidateBoothIds().forEach(arr::add);
        }
        return objectMapper.treeToValue(node, AskRequest.class);
    }
}