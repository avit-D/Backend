package com.fairpilot.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * ONSITE 현장결제 등록 요청 DTO
 */
public record OnsitePaymentRequest(
        @NotNull Long exhibitionId,
        @NotNull Long reservationId,
        @NotBlank String pgTxId,
        @NotNull @Positive BigDecimal amount
) {}
