package com.fairpilot.service;

import com.fairpilot.dto.BoothInfo;
import com.fairpilot.registry.VectorStoreRegistry;
import com.fairpilot.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoothVectorService {

    private final VectorStoreRegistry vectorStoreRegistry;
    private final VectorRepository vectorRepository;

    public void add(Long exhibitionId, BoothInfo booth) {
        SimpleVectorStore store = vectorStoreRegistry.getOrCreate(exhibitionId, s ->
                vectorRepository.load(exhibitionId, s)
        );

        Document document = toDocument(booth);
        store.add(List.of(document));
        vectorRepository.save(exhibitionId, store);

        log.info("부스 인덱싱 추가 - exhibitionId: {}, boothId: {}", exhibitionId, booth.id());
    }

    public void addAll(Long exhibitionId, List<BoothInfo> booths) {
        SimpleVectorStore store = vectorStoreRegistry.getOrCreate(exhibitionId, s ->
                vectorRepository.load(exhibitionId, s)
        );

        List<Document> documents = booths.stream()
                .map(this::toDocument)
                .toList();

        store.add(documents);
        vectorRepository.save(exhibitionId, store);

        log.info("부스 인덱싱 추가 - exhibitionId: {}, 총 {}개", exhibitionId, booths.size());
    }

    public void remove(Long exhibitionId, Long boothId) {
        SimpleVectorStore store = vectorStoreRegistry.getOrCreate(exhibitionId, s ->
                vectorRepository.load(exhibitionId, s)
        );

        store.delete(List.of(String.valueOf(boothId)));
        vectorRepository.save(exhibitionId, store);

        log.info("부스 인덱싱 삭제 - exhibitionId: {}, boothId: {}", exhibitionId, boothId);
    }

    public void reindexAll(Long exhibitionId, List<BoothInfo> booths) {
        vectorStoreRegistry.evict(exhibitionId);
        vectorRepository.delete(exhibitionId);  // 기존 데이터 삭제

        addAll(exhibitionId, booths);

        log.info("박람회 전체 재인덱싱 완료 - exhibitionId: {}", exhibitionId);
    }

    private Document toDocument(BoothInfo booth) {
        return new Document(
                String.valueOf(booth.id()),
                String.format("[%s] %s - %s",
                        booth.tags(), booth.name(), booth.description()),
                Map.of(
                        "boothId",  booth.id(),
                        "posX",     booth.posX(),
                        "posY",     booth.posY(),
                        "tags", booth.tags()
                )
        );
    }
}