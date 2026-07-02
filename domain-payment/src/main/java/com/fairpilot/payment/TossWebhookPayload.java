package com.fairpilot.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 토스페이먼츠 webhook payload
 * 토스가 우리 서버로 보내주는 데이터 구조
 */
public record TossWebhookPayload(

        @JsonProperty("eventType")
        String eventType,

        @JsonProperty("data")
        Data data
) {
    public record Data(
            @JsonProperty("paymentKey")
            String paymentKey,     // 토스 결제 키 (pg_tx_id)

            @JsonProperty("orderId")
            String orderId,        // 가맹점 주문번호 (reservationId 추출용)

            @JsonProperty("status")
            String status,         // DONE / CANCELED / ABORTED 등

            @JsonProperty("totalAmount")
            BigDecimal totalAmount // 실결제 금액
    ) {}
}