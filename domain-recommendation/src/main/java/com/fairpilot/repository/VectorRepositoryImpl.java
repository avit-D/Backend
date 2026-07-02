package com.fairpilot.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class VectorRepositoryImpl implements VectorRepository {

    private static final String FILE_PATH = "./vector-store-%d.json";

    @Override
    public void save(Long exhibitionId, SimpleVectorStore store) {
        if (store == null) {
            log.warn("박람회 {} 벡터 스토어 없음 - 저장 스킵", exhibitionId);
            return;
        }
        File file = storeFile(exhibitionId);
        store.save(file);
        log.info("박람회 {} 벡터 스토어 저장: {}", exhibitionId, file.getAbsolutePath());
    }

    @Override
    public void load(Long exhibitionId, SimpleVectorStore store) {
        File file = new File(FILE_PATH.formatted(exhibitionId));
        if (file.exists()) store.load(file);
    }

    @Override
    public boolean exists(Long exhibitionId) {
        return storeFile(exhibitionId).exists();
    }

    @Override
    public void delete(Long exhibitionId) {
        File file = storeFile(exhibitionId);
        if (file.exists() && file.delete()) {
            log.info("박람회 {} 벡터 스토어 파일 삭제: {}", exhibitionId, file.getAbsolutePath());
        }
    }

    private File storeFile(Long exhibitionId) {
        return new File(FILE_PATH.formatted(exhibitionId));
    }
}
