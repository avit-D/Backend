package com.fairpilot.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryProcessor {

    private final PaymentRepository paymentRepository;

    /**
     * RestClient — Bean으로 선언하여 재사용
     */
    private final RestClient restClient = RestClient.create();

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    @Value("${portone.api-secret:}")
    private String portOneApiSecret;

    /**
     * Payment 1건씩 독립 트랜잭션으로 처리
     * REQUIRES_NEW: 스케줄러 트랜잭션과 분리
     * → HTTP 호출 전후로 커넥션 반납/재획득
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Payment payment) {
        try {
            if ("TOSS".equals(payment.getPgProvider())) {
                retryToss(payment);
            } else if ("PORTONE".equals(payment.getPgProvider())) {
                retryPortOne(payment);
            }
        } catch (Exception e) {
            log.error("webhook 재시도 실패: paymentId={}, retryCount={}, error={}",
                    payment.getId(), payment.getWebhookRetryCount(), e.getMessage());

            if (payment.isRetryExhausted()) {
                log.error("webhook 재시도 한도 초과 — 수동 확인 필요: paymentId={}",
                        payment.getId());
            } else {
                payment.scheduleRetry(e.getMessage());
            }
            paymentRepository.save(payment);
        }
    }

    private void retryToss(Payment payment) {
        String orderId = payment.getPgTxId();

        // HTTP 호출 (트랜잭션 외부에서 실행되므로 커넥션 점유 없음)
        TossPaymentQueryResponse response = restClient.get()
                .uri("https://api.tosspayments.com/v1/payments/orders/" + orderId)
                .header("Authorization", "Basic " +
                        Base64.getEncoder().encodeToString(
                                (tossSecretKey + ":").getBytes()))
                .retrieve()
                .body(TossPaymentQueryResponse.class);

        // response null 시 save() 반드시 호출
        if (response == null) {
            payment.scheduleRetry("토스 조회 응답 없음");
            paymentRepository.save(payment);  // ← 누락 버그 수정
            return;
        }

        log.info("토스 결제 상태 조회: orderId={}, status={}", orderId, response.status());

        switch (response.status()) {
            case "DONE" -> {
                payment.updatePgTxId(response.paymentKey());
                payment.markPaid();
            }
            case "ABORTED", "EXPIRED" -> payment.markFailed();
            default -> payment.scheduleRetry("토스 상태 미확정: " + response.status());
        }
        paymentRepository.save(payment);
    }

    private void retryPortOne(Payment payment) {
        String paymentId = payment.getPgTxId();

        PortOnePaymentQueryResponse response = restClient.get()
                .uri("https://api.portone.io/payments/" + paymentId)
                .header("Authorization", "PortOne " + portOneApiSecret)
                .retrieve()
                .body(PortOnePaymentQueryResponse.class);

        // response null 시 save() 반드시 호출
        if (response == null) {
            payment.scheduleRetry("포트원 조회 응답 없음");
            paymentRepository.save(payment);  // ← 누락 버그 수정
            return;
        }

        log.info("포트원 결제 상태 조회: paymentId={}, status={}", paymentId, response.status());

        switch (response.status()) {
            case "PAID" -> payment.markPaid();
            case "FAILED", "CANCELLED" -> payment.markFailed();
            default -> payment.scheduleRetry("포트원 상태 미확정: " + response.status());
        }
        paymentRepository.save(payment);
    }
}