package com.fairpilot.exhibition;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "advertisement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Advertisement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adSlotId;

    @Column(nullable = false, length = 200)
    private String advertiserName;

    @Column
    private Long exhibitorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String linkUrl;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdStatus status;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long clicks;

    @Builder
    public Advertisement(Long adSlotId, String advertiserName, Long exhibitorId,
                         String title, String imageUrl, String linkUrl,
                         LocalDateTime startAt, LocalDateTime endAt, BigDecimal price) {
        this.adSlotId = adSlotId;
        this.advertiserName = advertiserName;
        this.exhibitorId = exhibitorId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.status = AdStatus.DRAFT;
        this.impressions = 0;
        this.clicks = 0;
    }

    /** 노출 카운트 증가 */
    public void incrementImpressions() {
        this.impressions++;
    }

    /** 클릭 카운트 증가 */
    public void incrementClicks() {
        this.clicks++;
    }
}