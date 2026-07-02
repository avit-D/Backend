package com.fairpilot.ai;

import com.fairpilot.dto.AssistantDto.*;
import com.fairpilot.exhibition.Booth;
import com.fairpilot.exhibition.BoothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * HallucinationGuard — 무상태(Stateless) 검증 엔진
 * ────────────────────────────────────────────────────────────────
 * Booth.id 가 Long(자동생성 PK)으로 바뀜에 따라 검증 방식 변경:
 *
 *  - 기존: "BOOTH-001" 같은 고정 패턴 → 정규식으로 텍스트 스캔 가능
 *  - 변경: Long PK 는 임의 숫자라 정규식 스캔이 오탐 위험 큼
 *          → 프롬프트에서 부스를 [BOOTH:{id}] 토큰으로 노출하고
 *             LLM 이 반드시 이 토큰 형태로만 인용하도록 강제
 *          → 텍스트 스캔도 동일 토큰 패턴(\[BOOTH:\d+\])으로 수행
 *
 * 3중 방어
 *  ① 선언 ID × (DB 실존 ∩ 후보 풀) 교차 검증
 *  ② 응답 텍스트 잔류 [BOOTH:id] 토큰 스캔
 *  ③ 유령 토큰 → [검증되지 않은 부스] 마스킹
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HallucinationGuard {

    /** 프롬프트/응답에서 부스를 식별하는 토큰 패턴: [BOOTH:123] */
    private static final Pattern BOOTH_TOKEN_PATTERN =
            Pattern.compile("\\[BOOTH:(\\d+)\\]");

    private final BoothRepository boothRepository;

    public record GuardResult(
            List<Long> verifiedIds,
            List<Long> hallucinatedIds,
            List<Long> textLeakedIds,
            String sanitizedAnswer
    ) {}

    /**
     * @param llmOutput     LLM 원시 출력 (referencedBoothIds 는 Long 문자열 목록)
     * @param candidatePool 이번 요청에서 허용된 부스 ID 집합 (보통 exhibitionId 로 조회한 결과)
     */
    public GuardResult verify(LlmRawOutput llmOutput, Set<Long> candidatePool) {

        List<String> declaredRaw = Optional.ofNullable(llmOutput.getReferencedBoothIds())
                .orElse(Collections.emptyList());

        List<Long> verifiedIds = new ArrayList<>();
        List<Long> hallucinatedIds = new ArrayList<>();

        // ① 선언 ID 교차 검증 (문자열 → Long 파싱 실패도 할루시네이션으로 처리)
        for (String rawId : declaredRaw) {
            Long id = parseIdSafely(rawId);
            boolean isExistBooth = boothRepository.existsById(id);
            boolean containCandidate = candidatePool.contains(id);
            if (id != null && isExistBooth && containCandidate) {
                verifiedIds.add(id);
            } else {
                log.warn("[HallucinationGuard] 유령 ID: '{}' (파싱={}, DB존재={}, 후보풀포함={})",
                        rawId, id,
                        id != null && isExistBooth,
                        id != null && containCandidate);
                if (id != null) hallucinatedIds.add(id);
            }
        }

        // ② 응답 텍스트 잔류 [BOOTH:id] 토큰 스캔
        Set<Long> verifiedSet = new HashSet<>(verifiedIds);
        List<Long> textLeakedIds = scanTextForLeakedTokens(llmOutput.getAnswer(), verifiedSet);

        if (!textLeakedIds.isEmpty()) {
            log.warn("[HallucinationGuard] 텍스트 잔류 유령 토큰: {}", textLeakedIds);
        }

        // ③ 유령 토큰 마스킹
        Set<Long> allGhosts = new HashSet<>(hallucinatedIds);
        allGhosts.addAll(textLeakedIds);
        String sanitized = sanitizeAnswer(llmOutput.getAnswer(), allGhosts);

        return new GuardResult(verifiedIds, hallucinatedIds, textLeakedIds, sanitized);
    }

    /** "123", "BOOTH-123" 등 다양한 LLM 출력 형태를 Long 으로 안전 파싱 */
    private Long parseIdSafely(String raw) {
        if (raw == null) return null;
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        if (digitsOnly.isBlank()) return null;
        try {
            return Long.parseLong(digitsOnly);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Long> scanTextForLeakedTokens(String text, Set<Long> verifiedSet) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        Set<Long> leaked = new LinkedHashSet<>();
        Matcher m = BOOTH_TOKEN_PATTERN.matcher(text);
        while (m.find()) {
            Long id = Long.parseLong(m.group(1));
            if (!verifiedSet.contains(id)) leaked.add(id);
        }
        return new ArrayList<>(leaked);
    }

    private String sanitizeAnswer(String text, Set<Long> ghostIds) {
        if (text == null || ghostIds.isEmpty()) return text;

        String result = text;
        for (Long ghost : ghostIds) {
            String token = "[BOOTH:" + ghost + "]";
            result = result.replace(token, "[검증되지 않은 부스]");
        }
        return result;
    }

    /**
     * 후보 풀 구성
     *  - requestedIds 없음 → exhibitionId 전체 부스를 후보 풀로 사용
     *  - requestedIds 있음 → DB에 실존하는 것만 필터링
     */
    public Set<Long> buildCandidatePool(Long exhibitionId, List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return boothRepository.findAllByExhibitionId(exhibitionId).stream()
                    .map(Booth::getId)
                    .collect(Collectors.toSet());
        }
        return requestedIds.stream()
                .filter(boothRepository::existsById)
                .collect(Collectors.toSet());
    }
}