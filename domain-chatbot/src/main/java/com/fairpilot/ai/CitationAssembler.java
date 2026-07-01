package com.fairpilot.ai;

import com.fairpilot.dto.AssistantDto.*;
import com.fairpilot.exhibition.Booth;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CitationAssembler
 * ────────────────────────────────────────────────────────────────
 * 검증된 ID 에만 DB 원본 정보 조립 (ExhibitorRepository 조회 제거)
 */
@Component
public class CitationAssembler {

    public List<CitedBooth> assemble(
            List<Long> verifiedIds,
            Map<Long, Booth> boothMap,
            Map<String, String> citationNotes
    ) {
        return verifiedIds.stream()
                .filter(boothMap::containsKey)
                .map(id -> {
                    Booth booth = boothMap.get(id);
                    return CitedBooth.builder()
                            .id(booth.getId())
                            .name(booth.getName())   // DB 원본만 사용
                            .relevanceNote(citationNotes.getOrDefault(String.valueOf(id), "관련 부스로 선정됨"))
                            .build();
                })
                .toList();
    }
}