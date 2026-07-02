package com.fairpilot.payment;

/**
 * 포트원 결제 상태 조회 응답 DTO
 * GET https://api.portone.io/payments/{paymentId}
 */
public record PortOnePaymentQueryResponse(
        String id,
        String status
) {}