package com.fairpilot.service;

import com.fairpilot.dto.BoothInfo;
import com.fairpilot.dto.RouteItem;
import com.fairpilot.dto.RouteRecommendResponse;
import com.fairpilot.registry.VectorStoreRegistry;
import com.fairpilot.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.VectorStore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    @Qualifier("recommendClient")
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final VectorStoreRegistry registry;
    private final VectorRepository vectorRepository;

    private static final String promptFormat = """
                [유저 관심사]
                %s
                
                [방문 가능한 부스 목록]
                %s
                
                위 정보를 바탕으로 다음 조건을 고려하여 최적 방문 순서를 추천해주세요:
                1. 유저 관심사와 부스 카테고리 일치도 (가장 중요)
                2. 이동 거리 최소화 (같은 층 우선, X/Y 좌표 기반 인접 순서)
                3. 혼잡도가 낮은 부스 우선 배치
                
                반드시 아래 JSON 형식으로만 응답하세요:
                {
                  "route": [
                    {"order": 1, "boothId": 번호, "name": "부스명", "reason": "추천이유"},
                    {"order": 2, "boothId": 번호, "name": "부스명", "reason": "추천이유"}
                  ],
                  "summary": "전체 동선 요약 한 줄"
                }
                """;

    public RouteRecommendResponse recommend(Long exhibitionId, List<String> interests, List<BoothInfo> allBooths) {

        VectorStore vectorStore = registry.getOrCreate(exhibitionId, s ->
                vectorRepository.load(exhibitionId, s)
        );
        String interestKeywords = String.join(", ", interests);

        // 1. RAG: 관심사 기반 유사 부스 검색
        List<Document> relatedDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(interestKeywords)
                        .topK(10)
                        .similarityThreshold(0.5)
                        .build()
        );

        log.info("RAG 검색 결과: {}개 문서", relatedDocs.size());

        // 2. 검색된 boothId로 allBooths에서 필터링
        List<Long> candidateIds = relatedDocs.stream()
                .map(doc -> Long.valueOf(doc.getMetadata().get("boothId").toString()))
                .toList();

        List<BoothInfo> candidates = allBooths.stream()
                .filter(b -> candidateIds.contains(b.id()))
                .toList();

        // RAG 결과가 없으면 전체 부스 대상으로 추천
        if (candidates.isEmpty()) {
            log.warn("RAG 검색 결과 없음 - 전체 부스 대상으로 추천");
            candidates = allBooths;
        }

        // 3. LLM 컨텍스트 구성
        String boothContext = candidates.stream()
                .map(b -> String.format(
                        "부스ID=%d, 이름=%s, 태그=%s, 위치=(X:%d,Y:%d), 층=%d, 혼잡도=%s",
                        b.id(), b.name(), b.tags(), b.posX(), b.posY(), b.floor(), b.congestionLevel()
                ))
                .collect(Collectors.joining("\n"));

        // 4. LLM 호출
        String prompt = String.format(promptFormat, interestKeywords, boothContext);

        String rawJson;
        try {
            rawJson = chatClient.prompt().user(prompt).call().content();
            log.info("LLM 응답: {}", rawJson);
            // 5. 파싱 후 반환
            return parse(rawJson, candidates);
        } catch (Exception e) {
            log.error("LLM 호출 실패", e);
            return new RouteRecommendResponse(List.of(), "추천 서비스 일시 불가");
        }
    }

    private RouteRecommendResponse parse(String rawJson, List<BoothInfo> booths) {
        String json = rawJson.replaceAll("(?s)```json|```", "").trim();

        try {
            JsonNode root = objectMapper.readTree(json);
            List<RouteItem> route = new ArrayList<>();

            for (JsonNode item : root.get("route")) {
                long boothId = item.get("boothId").asLong();

                BoothInfo booth = booths.stream()
                        .filter(b -> b.id().equals(boothId))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("부스 없음: " + boothId));

                route.add(new RouteItem(
                        item.get("order").asInt(),
                        boothId,
                        booth.name(),
                        booth.posX(),
                        booth.posY(),
                        booth.floor(),
                        booth.congestionLevel(),
                        item.get("reason").asText()
                ));
            }

            return new RouteRecommendResponse(route, root.get("summary").asText());

        } catch (Exception e) {
            throw new RuntimeException("LLM 응답 파싱 실패. 원본: " + rawJson, e);
        }
    }
}