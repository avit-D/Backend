package com.fairpilot.payment;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 포트원 webhook 멱등 처리
     * 같은 pgTxId가 이미 있으면 무시 (중복 webhook 방어)
     */
    @Transactional
    public void handleWebhook(PortOneWebhookPayload payload) {
        String pgTxId = payload.data().paymentId();
        String type = payload.type();

        // 멱등성 체크 — 이미 처리된 webhook이면 무시
        if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
            log.info("중복 webhook 무시: pgTxId={}", pgTxId);
            return;
        }

        // ONSITE 현장결제는 별도 처리
        if ("ONSITE".equals(type)) {
            handleOnsite(payload);
            return;
        }

        // 포트원 결제 상태에 따라 처리
        switch (payload.data().status()) {
            case "PAID" -> {
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(pgTxId))
                        .pgProvider("PORTONE_TOSS")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO) // 포트원 검증 후 실금액 세팅
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markPaid();
                paymentRepository.save(payment);
                log.info("결제 완료 처리: pgTxId={}", pgTxId);
            }
            case "FAILED" -> {
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(pgTxId))
                        .pgProvider("PORTONE_TOSS")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO)
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markFailed();
                paymentRepository.save(payment);
                log.info("결제 실패 처리: pgTxId={}", pgTxId);
            }
            case "CANCELLED" -> {
                paymentRepository.findByPgTxId(pgTxId).ifPresent(p -> {
                    p.markCancelled();
                    paymentRepository.save(p);
                });
                log.info("결제 취소 처리: pgTxId={}", pgTxId);
            }
            default -> log.warn("알 수 없는 결제 상태: {}", payload.data().status());
        }
    }

    /** ONSITE 현장결제 분리 처리 */
    @Transactional
    public void handleOnsite(PortOneWebhookPayload payload) {
        String pgTxId = payload.data().paymentId();

        Payment payment = Payment.builder()
                .reservationId(extractReservationId(pgTxId))
                .pgProvider("ONSITE")
                .pgTxId(pgTxId)
                .amount(BigDecimal.ZERO)
                .feeAmount(BigDecimal.ZERO)
                .build();
        payment.markPaid();
        paymentRepository.save(payment);
        log.info("현장결제 처리: pgTxId={}", pgTxId);
    }

    /** pgTxId에서 reservationId 추출 (포트원 custom data 기반) */
    private Long extractReservationId(String pgTxId) {
        try {
            return Long.parseLong(pgTxId.split("_")[0]);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "pgTxId에서 reservationId 추출 실패: " + pgTxId);
        }
    }
}