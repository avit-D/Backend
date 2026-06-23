package com.fairpilot.exhibition;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "ad_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdSlot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long exhibitionId;

    @Column(nullable = false, length = 80)
    private String placement;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdSlotStatus status;

    @Builder
    public AdSlot(Long exhibitionId, String placement, BigDecimal basePrice) {
        this.exhibitionId = exhibitionId;
        this.placement = placement;
        this.basePrice = basePrice != null ? basePrice : BigDecimal.ZERO;
        this.status = AdSlotStatus.ACTIVE;
    }
}