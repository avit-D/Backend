package com.fairpilot.payment;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE settlement SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long exhibitionId;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal onlineAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal onsiteAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal adRevenue;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column
    private LocalDateTime paidOutAt;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Builder
    public Settlement(Long exhibitionId, LocalDate periodStart, LocalDate periodEnd,
                      BigDecimal onlineAmount, BigDecimal onsiteAmount,
                      BigDecimal feeAmount, BigDecimal adRevenue) {
        this.exhibitionId = exhibitionId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.onlineAmount = onlineAmount != null ? onlineAmount : BigDecimal.ZERO;
        this.onsiteAmount = onsiteAmount != null ? onsiteAmount : BigDecimal.ZERO;
        this.grossAmount = this.onlineAmount.add(this.onsiteAmount);
        this.feeAmount = feeAmount != null ? feeAmount : BigDecimal.ZERO;
        this.adRevenue = adRevenue != null ? adRevenue : BigDecimal.ZERO;
        this.netPayout = this.grossAmount.subtract(this.feeAmount).add(this.adRevenue);
        this.status = SettlementStatus.PENDING;
        this.isDeleted = false;
    }
}