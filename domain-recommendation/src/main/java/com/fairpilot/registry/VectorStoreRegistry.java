package com.fairpilot.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreRegistry {

    private final EmbeddingModel embeddingModel;

    private final Map<Long, SimpleVectorStore> registry = new ConcurrentHashMap<>();

    public SimpleVectorStore getOrCreate(Long exhibitionId, Consumer<SimpleVectorStore> onCreateCallback) {
        return registry.computeIfAbsent(exhibitionId, id -> {
            SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
            onCreateCallback.accept(simpleVectorStore);
            return simpleVectorStore;
        });
    }

    public void evict(Long exhibitionId) {
        registry.remove(exhibitionId);
    }

    public boolean exists(Long exhibitionId) {
        return registry.containsKey(exhibitionId);
    }
}
