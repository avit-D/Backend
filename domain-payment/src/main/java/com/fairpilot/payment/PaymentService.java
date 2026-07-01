package com.fairpilot.payment;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 포트원 webhook 멱등 처리
     * PAID/FAILED: 중복 수신 시 무시
     * CANCELLED: 기존 레코드 상태 업데이트 (멱등성 체크 제외)
     */
    @Transactional
    public void handleWebhook(PortOneWebhookPayload payload) {
        String pgTxId = payload.data().paymentId();
        String status = payload.data().status();

        switch (status) {
            case "PAID" -> {
                // 중복 webhook 방어
                if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
                    log.info("중복 PAID webhook 무시: pgTxId={}", pgTxId);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(pgTxId))
                        .pgProvider("PORTONE")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO) // 포트원 API 검증 후 실금액 세팅
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markPaid();
                paymentRepository.save(payment);
                log.info("결제 완료 처리: pgTxId={}", pgTxId);
            }
            case "FAILED" -> {
                if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
                    log.info("중복 FAILED webhook 무시: pgTxId={}", pgTxId);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(pgTxId))
                        .pgProvider("PORTONE")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO)
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markFailed();
                paymentRepository.save(payment);
                log.info("결제 실패 처리: pgTxId={}", pgTxId);
            }
            case "CANCELLED" -> {
                // CANCELLED는 기존 레코드 업데이트 — 멱등성 체크 없이 진행
                paymentRepository.findByPgTxId(pgTxId).ifPresentOrElse(p -> {
                    p.markCancelled();
                    paymentRepository.save(p);
                    log.info("결제 취소 처리: pgTxId={}", pgTxId);
                }, () -> log.warn("취소 대상 결제 없음: pgTxId={}", pgTxId));
            }
            default -> log.warn("알 수 없는 결제 상태: {}", status);
        }
    }

    /** ONSITE 현장결제 처리 (/onsite 엔드포인트 전용) */
    @Transactional
    public void handleOnsite(OnsitePaymentRequest req) {
        // 중복 등록 방어
        if (paymentRepository.findByPgTxId(req.pgTxId()).isPresent()) {
            log.info("중복 ONSITE 결제 무시: pgTxId={}", req.pgTxId());
            return;
        }
        Payment payment = Payment.builder()
                .reservationId(req.reservationId())
                .exhibitionId(req.exhibitionId())
                .pgProvider("ONSITE")
                .pgTxId(req.pgTxId())
                .amount(req.amount())
                .feeAmount(BigDecimal.ZERO)
                .build();
        payment.markPaid();
        paymentRepository.save(payment);
        log.info("현장결제 처리: pgTxId={}, exhibitionId={}", req.pgTxId(), req.exhibitionId());
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