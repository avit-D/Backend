package com.fairpilot.repository;

import org.springframework.ai.vectorstore.SimpleVectorStore;

public interface VectorRepository {
    void save(Long exhibitionId, SimpleVectorStore store);
    void load(Long exhibitionId, SimpleVectorStore store);
    void delete(Long exhibitionId);
    boolean exists(Long exhibitionId);
}
