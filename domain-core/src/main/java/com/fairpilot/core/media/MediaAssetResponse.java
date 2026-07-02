package com.fairpilot.core.media;

public record MediaAssetResponse(
        Long id,
        String ownerType,
        Long ownerId,
        String mediaType,
        String url,
        String contentType,
        Long fileSize,
        int displayOrder,
        boolean isThumbnail
) {
    public static MediaAssetResponse of(MediaAsset asset, String url) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getOwnerType().name(),
                asset.getOwnerId(),
                asset.getMediaType().name(),
                url,
                asset.getContentType(),
                asset.getFileSize(),
                asset.getDisplayOrder(),
                asset.isThumbnail()
        );
    }
}