package com.fairpilot.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 포트원 V2 webhook payload
 * 포트원이 우리 서버로 보내주는 데이터 구조
 */
public record PortOneWebhookPayload(

        @JsonProperty("type")
        String type,

        @JsonProperty("data")
        Data data
) {
    public record Data(
            @JsonProperty("paymentId")
            String paymentId,      // 포트원 결제 ID (pg_tx_id)

            @JsonProperty("transactionId")
            String transactionId,  // 거래 ID

            @JsonProperty("status")
            String status          // PAID / FAILED / CANCELLED
    ) {}
}