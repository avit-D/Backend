package com.fairpilot.payment;

import java.math.BigDecimal;

/**
 * 결제 요청 발급 응답 DTO
 * 프론트엔드는 orderId + amount 를 받아 PG사 결제창을 띄움
 */
public record PaymentInitiateResponse(

        /** 주문 ID — {reservationId}_{UUID} 형식, PG사 결제창에 그대로 전달 */
        String orderId,

        /** 결제 금액 — 프론트가 보낸 값을 그대로 반환 (확인용) */
        BigDecimal amount
) {}