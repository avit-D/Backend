package com.fairpilot.payment;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

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

    @Column
    private LocalDateTime deletedAt;

    @Builder
    public Payment(Long reservationId, String pgProvider, String pgTxId,
                   BigDecimal amount, BigDecimal feeAmount) {
        this.reservationId = reservationId;
        this.pgProvider = pgProvider;
        this.pgTxId = pgTxId;
        this.amount = amount;
        this.feeAmount = feeAmount != null ? feeAmount : BigDecimal.ZERO;
        this.status = PaymentStatus.READY;
    }

    /** 결제 완료 */
    public void markPaid() {
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /** 결제 실패 */
    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    /** 결제 취소/환불 */
    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }
}