package com.fairpilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

public class AssistantDto {

    // ── 요청 ──────────────────────────────────────────────────────
    @Getter
    public static class AskRequest {
        @NotNull(message = "exhibitionId 는 필수입니다.")
        private Long exhibitionId;          // ← 추가: 박람회 단위 후보 풀 조회 기준

        @NotBlank(message = "question 은 필수입니다.")
        @Size(max = 500, message = "question 은 500자 이하여야 합니다.")
        private String question;

        /** 선택: 사전에 좁혀둔 후보 부스 ID 목록 (Long PK). 없으면 exhibitionId 전체 풀 사용. */
        private List<Long> candidateBoothIds;   // ← String → Long
    }

    // ── 응답 ──────────────────────────────────────────────────────
    @Getter
    @Builder
    public static class AskResponse {
        private String answer;
        private List<CitedBooth> citedBooths;
        private List<Long> removedHallucinatedIds;   // ← String → Long
        private Meta meta;
    }

    @Getter
    @Builder
    public static class CitedBooth {
        private Long id;
        private String name;
        private String relevanceNote;
        // location(floor/posX/posY) 은 필요 시 별도 필드로 추가
    }

    @Getter
    @Builder
    public static class Meta {
        private int candidatePoolSize;
        private String modelUsed;
        private List<String> llmRawBoothIds;   // LLM 원시 출력은 문자열 그대로 보존
        private List<Long> verifiedBoothIds;   // ← String → Long
    }

    // ── LLM 원시 출력 (내부용) ─────────────────────────────────────
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LlmRawOutput {
        private String answer;
        private List<String> referencedBoothIds;   // LLM 출력은 문자열로 받고 가드에서 파싱
        private Map<String, String> citationNotes; // key 도 문자열 ID 그대로
    }
}