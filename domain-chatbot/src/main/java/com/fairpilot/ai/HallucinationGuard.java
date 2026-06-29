package com.fairpilot.ai;

import com.fairpilot.repository.BoothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.fairpilot.dto.AssistantDto.*;

/**
 * HallucinationGuard — 무상태(Stateless) 검증 엔진
 * ────────────────────────────────────────────────────────────────
 * LLM(Ollama) 응답에서 허위 부스 ID 를 탐지·격리·마스킹
 *
 * 3중 방어
 *  ① 선언 ID × (DB 실존 ∩ 후보 풀) 교차 검증
 *  ② 응답 텍스트 잔류 ID 패턴 스캔
 *  ③ 유령 ID → [검증되지 않은 부스] 마스킹
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HallucinationGuard {

    private static final Pattern BOOTH_ID_PATTERN =
            Pattern.compile("\\bBOOTH-\\d{3}\\b");

    private final BoothRepository boothRepository;

    public record GuardResult(
            List<String> verifiedIds,
            List<String> hallucinatedIds,
            List<String> textLeakedIds,
            String sanitizedAnswer
    ) {}

    public GuardResult verify(LlmRawOutput llmOutput, Set<String> candidatePool) {

        List<String> declared = Optional.ofNullable(llmOutput.getReferencedBoothIds())
                .orElse(Collections.emptyList());

        List<String> verifiedIds = new ArrayList<>();
        List<String> hallucinatedIds = new ArrayList<>();

        // ① 선언 ID 교차 검증
        for (String rawId : declared) {
            String id = rawId.trim().toUpperCase();
            boolean isExistBooth = boothRepository.existsById(id);
            boolean isCandiateBooth = candidatePool.contains(id);
            if (isExistBooth && isCandiateBooth) {
                verifiedIds.add(id);
            } else {
                hallucinatedIds.add(rawId);
                log.warn("[HallucinationGuard] 유령 ID: '{}' (DB존재={}, 후보풀포함={})",
                        rawId,
                        isExistBooth,
                        isCandiateBooth);
            }
        }

        // ② 텍스트 잔류 ID 스캔
        Set<String> verifiedSet = new HashSet<>(verifiedIds);
        List<String> textLeakedIds = scanTextForLeakedIds(llmOutput.getAnswer(), verifiedSet);

        if (!textLeakedIds.isEmpty()) {
            log.warn("[HallucinationGuard] 텍스트 잔류 유령 ID: {}", textLeakedIds);
        }

        // ③ 마스킹
        Set<String> allGhosts = new HashSet<>(hallucinatedIds);
        allGhosts.addAll(textLeakedIds);
        String sanitized = sanitizeAnswer(llmOutput.getAnswer(), allGhosts);

        return new GuardResult(verifiedIds, hallucinatedIds, textLeakedIds, sanitized);
    }

    private List<String> scanTextForLeakedIds(String text, Set<String> verifiedSet) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        Set<String> leaked = new LinkedHashSet<>();
        Matcher m = BOOTH_ID_PATTERN.matcher(text);
        while (m.find()) {
            String id = m.group().toUpperCase();
            if (!verifiedSet.contains(id)) leaked.add(id);
        }
        return new ArrayList<>(leaked);
    }

    private String sanitizeAnswer(String text, Set<String> ghostIds) {
        if (text == null || ghostIds.isEmpty()) return text;

        String result = text;
        for (String ghost : ghostIds) {
            Pattern p = Pattern.compile(Pattern.quote(ghost), Pattern.CASE_INSENSITIVE);
            result = p.matcher(result).replaceAll("[검증되지 않은 부스]");
        }
        return result;
    }

    public Set<String> buildCandidatePool(List<String> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return new HashSet<>(boothRepository.findAllIds());
        }
        return requestedIds.stream()
                .map(id -> id.trim().toUpperCase())
                .filter(boothRepository::existsById)
                .collect(Collectors.toSet());
    }
}
