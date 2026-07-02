package com.fairpilot.service;

import com.fairpilot.ai.CitationAssembler;
import com.fairpilot.ai.HallucinationGuard;
import com.fairpilot.ai.HallucinationGuard.*;
import com.fairpilot.ai.LlmClient;
import com.fairpilot.ai.PromptBuilder;
import com.fairpilot.dto.AssistantDto.*;
import com.fairpilot.exhibition.Booth;
import com.fairpilot.exhibition.BoothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import com.fairpilot.OllamaProperties;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AssistantService — 파이프라인 오케스트레이터
 * ────────────────────────────────────────────────────────────────
 * 변경점:
 *  - candidatePool, verifiedIds 등 모든 ID 타입 String → Long
 *  - 후보 풀 조회 기준이 exhibitionId 추가 (요청에 필수)
 *  - boothRepository.findAllById 가 JpaRepository 표준 메서드(List<Long>) 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final BoothRepository boothRepository;
    private final HallucinationGuard guard;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final CitationAssembler assembler;
    private final OllamaProperties ollamaProperties;

    public AskResponse ask(AskRequest request) {

        // ① 후보 풀 구성: exhibitionId 기준 또는 요청 지정 ID
        Set<Long> candidatePool = guard.buildCandidatePool(
                request.getExhibitionId(), request.getCandidateBoothIds());
        List<Booth> candidateBooths = boothRepository.findAllById(candidatePool);
        log.debug("[AssistantService] exhibitionId={}, 후보풀={}, 질문='{}'",
                request.getExhibitionId(), candidatePool.size(), request.getQuestion());

        // ② Spring AI Prompt 조립
        Prompt prompt = promptBuilder.build(request.getQuestion(), candidateBooths);

        // ③ Ollama 호출
        LlmRawOutput llmOutput = llmClient.complete(prompt);

        // ④ 할루시네이션 가드
        GuardResult guardResult = guard.verify(llmOutput, candidatePool);
        log.debug("[AssistantService] 검증완료={}, 제거={}+{}",
                guardResult.verifiedIds(),
                guardResult.hallucinatedIds(),
                guardResult.textLeakedIds());

        // ⑤ 인용 조립 (boothMap 키도 Long)
        Map<Long, Booth> boothMap = candidateBooths.stream()
                .collect(Collectors.toMap(Booth::getId, b -> b));

        List<CitedBooth> citedBooths = assembler.assemble(
                guardResult.verifiedIds(), boothMap, llmOutput.getCitationNotes());

        List<Long> allHallucinatedIds = new ArrayList<>();
        allHallucinatedIds.addAll(guardResult.hallucinatedIds());
        allHallucinatedIds.addAll(guardResult.textLeakedIds());

        return AskResponse.builder()
                .answer(guardResult.sanitizedAnswer())
                .citedBooths(citedBooths)
                .removedHallucinatedIds(allHallucinatedIds)
                .meta(Meta.builder()
                        .candidatePoolSize(candidatePool.size())
                        .modelUsed(ollamaProperties.getModel())
                        .llmRawBoothIds(llmOutput.getReferencedBoothIds())
                        .verifiedBoothIds(guardResult.verifiedIds())
                        .build())
                .build();
    }
}