package com.fairpilot.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private final PaymentRepository paymentRepository;
    private final WebhookRetryProcessor webhookRetryProcessor;

    /**
     * webhook 미도달 재시도 배치 — 1분마다 실행
     * @Transactional 제거: 외부 HTTP 호출이 포함되므로
     * DB 커넥션을 오래 잡지 않도록 Payment별로 별도 트랜잭션 처리
     */
    @Scheduled(fixedDelay = 60_000)
    public void retryPendingWebhooks() {
        List<Payment> targets = paymentRepository.findRetryTargets(LocalDateTime.now());

        if (targets.isEmpty()) return;

        log.info("webhook 재시도 대상: {}건", targets.size());

        for (Payment payment : targets) {
            // Payment별 독립 트랜잭션 — HTTP 호출 중 커넥션 반납
            webhookRetryProcessor.process(payment);
        }
    }
}