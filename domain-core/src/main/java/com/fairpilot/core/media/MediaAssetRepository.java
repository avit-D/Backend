package com.fairpilot.core.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    /** owner별 미디어 목록 조회 (노출 순서 정렬) */
    List<MediaAsset> findByOwnerTypeAndOwnerIdOrderByDisplayOrderAsc(
            OwnerType ownerType, Long ownerId);
}