package com.fairpilot.core.media;

import jakarta.validation.constraints.NotNull;

public record MediaUploadRequest(

        @NotNull(message = "ownerType은 필수입니다.")
        OwnerType ownerType,

        @NotNull(message = "ownerId는 필수입니다.")
        Long ownerId,

        /** 대표 썸네일 여부 */
        boolean isThumbnail,

        /** 노출 순서 */
        int displayOrder
) {}