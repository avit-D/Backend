package com.fairpilot.payment;

import java.math.BigDecimal;

/**
 * 토스 결제 상태 조회 응답 DTO
 * GET https://api.tosspayments.com/v1/payments/orders/{orderId}
 */
public record TossPaymentQueryResponse(
        String paymentKey,
        String orderId,
        String status,
        BigDecimal totalAmount
) {}