package com.fairpilot.exhibition;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdvertisementResponse(
        Long id,
        Long adSlotId,
        String advertiserName,
        Long exhibitorId,
        String title,
        String imageUrl,
        String linkUrl,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal price,
        String status,
        long impressions,
        long clicks
) {
    public static AdvertisementResponse from(Advertisement a) {
        return new AdvertisementResponse(
                a.getId(), a.getAdSlotId(), a.getAdvertiserName(), a.getExhibitorId(),
                a.getTitle(), a.getImageUrl(), a.getLinkUrl(),
                a.getStartAt(), a.getEndAt(), a.getPrice(),
                a.getStatus().name(), a.getImpressions(), a.getClicks()
        );
    }
}
