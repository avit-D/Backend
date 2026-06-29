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
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE payment SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    @Column
    private Long exhibitionId;

    @Column(length = 40)
    private String pgProvider;

    @Column(length = 120, unique = true)
    private String pgTxId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column
    private LocalDateTime paidAt;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Builder
    public Payment(Long reservationId, Long exhibitionId, String pgProvider, String pgTxId,
                   BigDecimal amount, BigDecimal feeAmount) {
        this.reservationId = reservationId;
        this.exhibitionId = exhibitionId;
        this.pgProvider = pgProvider;
        this.pgTxId = pgTxId;
        this.amount = amount;
        this.feeAmount = feeAmount != null ? feeAmount : BigDecimal.ZERO;
        this.status = PaymentStatus.READY;
        this.isDeleted = false;
    }

    public void markPaid() {
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }
}