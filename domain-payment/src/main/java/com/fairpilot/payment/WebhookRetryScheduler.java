package com.fairpilot.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * webhook 미도달 재시도 배치
     * 1분마다 실행 — READY 상태이고 webhookRetryAt 이 현재 이전인 건 재시도
     *
     * [재시도 대상]
     * - status = READY (아직 PAID/FAILED 처리 안 됨)
     * - webhookRetryAt <= 현재 시각
     * - webhookRetryCount < 5 (최대 5회)
     *
     * [처리 방식]
     * PG사에 결제 상태를 직접 조회(polling)해서 처리
     * 토스: GET https://api.tosspayments.com/v1/payments/orders/{orderId}
     * 포트원: GET https://api.portone.io/payments/{paymentId}
     */
    @Scheduled(fixedDelay = 60_000) // 1분마다
    @Transactional
    public void retryPendingWebhooks() {
        List<Payment> targets = paymentRepository.findRetryTargets(LocalDateTime.now());

        if (targets.isEmpty()) return;

        log.info("webhook 재시도 대상: {}건", targets.size());

        for (Payment payment : targets) {
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
    }

    /**
     * 토스 결제 상태 직접 조회
     * orderId 기반으로 토스에 조회 → DONE이면 markPaid()
     */
    private void retryToss(Payment payment) {
        String orderId = payment.getPgTxId(); // initiate 시 저장된 orderId

        RestClient restClient = RestClient.create();
        TossPaymentQueryResponse response = restClient.get()
                .uri("https://api.tosspayments.com/v1/payments/orders/" + orderId)
                .header("Authorization", "Basic " +
                        java.util.Base64.getEncoder().encodeToString(
                                (getTossSecretKey() + ":").getBytes()))
                .retrieve()
                .body(TossPaymentQueryResponse.class);

        if (response == null) {
            payment.scheduleRetry("토스 조회 응답 없음");
            return;
        }

        log.info("토스 결제 상태 조회: orderId={}, status={}", orderId, response.status());

        switch (response.status()) {
            case "DONE" -> {
                payment.updatePgTxId(response.paymentKey());
                payment.markPaid();
                log.info("토스 재시도 PAID 처리: orderId={}", orderId);
            }
            case "ABORTED", "EXPIRED" -> {
                payment.markFailed();
                log.info("토스 재시도 FAILED 처리: orderId={}", orderId);
            }
            default -> {
                // 아직 처리 중 → 다음 재시도 예약
                payment.scheduleRetry("토스 상태 미확정: " + response.status());
            }
        }
        paymentRepository.save(payment);
    }

    /**
     * 포트원 결제 상태 직접 조회
     */
    private void retryPortOne(Payment payment) {
        String paymentId = payment.getPgTxId();

        RestClient restClient = RestClient.create();
        PortOnePaymentQueryResponse response = restClient.get()
                .uri("https://api.portone.io/payments/" + paymentId)
                .header("Authorization", "PortOne " + getPortOneApiSecret())
                .retrieve()
                .body(PortOnePaymentQueryResponse.class);

        if (response == null) {
            payment.scheduleRetry("포트원 조회 응답 없음");
            return;
        }

        log.info("포트원 결제 상태 조회: paymentId={}, status={}", paymentId, response.status());

        switch (response.status()) {
            case "PAID" -> {
                payment.markPaid();
                log.info("포트원 재시도 PAID 처리: paymentId={}", paymentId);
            }
            case "FAILED", "CANCELLED" -> {
                payment.markFailed();
                log.info("포트원 재시도 FAILED 처리: paymentId={}", paymentId);
            }
            default -> {
                payment.scheduleRetry("포트원 상태 미확정: " + response.status());
            }
        }
        paymentRepository.save(payment);
    }

    @org.springframework.beans.factory.annotation.Value("${toss.secret-key}")
    private String tossSecretKey;

    @org.springframework.beans.factory.annotation.Value("${portone.api-secret:}")
    private String portOneApiSecret;

    private String getTossSecretKey() { return tossSecretKey; }
    private String getPortOneApiSecret() { return portOneApiSecret; }
}