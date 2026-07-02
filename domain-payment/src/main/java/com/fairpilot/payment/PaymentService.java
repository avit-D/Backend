package com.fairpilot.payment;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제 요청 발급
     * initiate() 시 webhookRetryAt = 10분 후로 설정
     * → webhook 미도달 시 스케줄러가 10분 후 자동 재시도
     */
    @Transactional
    public PaymentInitiateResponse initiate(PaymentInitiateRequest req) {
        if (!List.of("TOSS", "PORTONE").contains(req.pgProvider())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 PG사입니다: " + req.pgProvider());
        }

        paymentRepository.findByReservationIdAndStatusIn(
                req.reservationId(),
                List.of(PaymentStatus.READY, PaymentStatus.PAID)
        ).ifPresent(p -> {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "이미 진행 중이거나 완료된 결제가 있습니다. reservationId=" + req.reservationId());
        });

        String orderId = req.reservationId() + "_"
                + UUID.randomUUID().toString().replace("-", "");

        Payment payment = Payment.builder()
                .reservationId(req.reservationId())
                .exhibitionId(req.exhibitionId())
                .pgProvider(req.pgProvider())
                .pgTxId(orderId)
                .amount(req.amount())
                .feeAmount(BigDecimal.ZERO)
                .build();

        // webhook 미도달 대비: 10분 후 재시도 예약
        // webhook이 정상 수신되면 markPaid()에서 webhookRetryAt = null 처리됨
        payment.initWebhookRetry();

        paymentRepository.save(payment);
        log.info("결제 요청 발급: orderId={}, pgProvider={}, amount={}, webhookRetryAt={}",
                orderId, req.pgProvider(), req.amount(), payment.getWebhookRetryAt());

        return new PaymentInitiateResponse(orderId, req.amount());
    }

    @Transactional
    public void handleWebhook(PortOneWebhookPayload payload) {
        String pgTxId = payload.data().paymentId();
        String status = payload.data().status();

        switch (status) {
            case "PAID" -> {
                if (paymentRepository.findByPgTxId(pgTxId).isPresent()) {
                    log.info("중복 PAID webhook 무시: pgTxId={}", pgTxId);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(pgTxId))
                        .pgProvider("PORTONE")
                        .pgTxId(pgTxId)
                        .amount(BigDecimal.ZERO)
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
                paymentRepository.findByPgTxId(pgTxId).ifPresentOrElse(p -> {
                    p.markCancelled();
                    paymentRepository.save(p);
                    log.info("결제 취소 처리: pgTxId={}", pgTxId);
                }, () -> log.warn("취소 대상 결제 없음: pgTxId={}", pgTxId));
            }
            default -> log.warn("알 수 없는 결제 상태: {}", status);
        }
    }

    @Transactional
    public void handleTossWebhook(TossWebhookPayload payload) {
        String paymentKey    = payload.data().paymentKey();
        String orderId       = payload.data().orderId();
        String status        = payload.data().status();
        BigDecimal webhookAmount = payload.data().totalAmount();

        switch (status) {
            case "DONE" -> {
                if (paymentRepository.findByPgTxId(paymentKey).isPresent()) {
                    log.info("중복 DONE webhook 무시: paymentKey={}", paymentKey);
                    return;
                }

                Long reservationId = extractReservationId(orderId);

                Payment payment = paymentRepository
                        .findByReservationIdAndStatusIn(
                                reservationId, List.of(PaymentStatus.READY))
                        .map(p -> {
                            if (webhookAmount != null &&
                                    p.getAmount().compareTo(webhookAmount) != 0) {
                                log.warn("금액 불일치 감지! 저장={}, webhook={}, orderId={}",
                                        p.getAmount(), webhookAmount, orderId);
                                throw new BusinessException(ErrorCode.INVALID_INPUT,
                                        "결제 금액이 일치하지 않습니다. 위변조 의심");
                            }
                            p.updatePgTxId(paymentKey);
                            return p;
                        })
                        .orElseGet(() -> {
                            log.warn("READY 레코드 없음, 신규 생성: orderId={}", orderId);
                            return Payment.builder()
                                    .reservationId(reservationId)
                                    .pgProvider("TOSS")
                                    .pgTxId(paymentKey)
                                    .amount(webhookAmount != null ? webhookAmount : BigDecimal.ZERO)
                                    .feeAmount(BigDecimal.ZERO)
                                    .build();
                        });

                payment.markPaid();
                paymentRepository.save(payment);
                log.info("토스 결제 완료 처리: paymentKey={}", paymentKey);
            }
            case "ABORTED" -> {
                if (paymentRepository.findByPgTxId(paymentKey).isPresent()) {
                    log.info("중복 ABORTED webhook 무시: paymentKey={}", paymentKey);
                    return;
                }
                Payment payment = Payment.builder()
                        .reservationId(extractReservationId(orderId))
                        .pgProvider("TOSS")
                        .pgTxId(paymentKey)
                        .amount(BigDecimal.ZERO)
                        .feeAmount(BigDecimal.ZERO)
                        .build();
                payment.markFailed();
                paymentRepository.save(payment);
                log.info("토스 결제 실패 처리: paymentKey={}", paymentKey);
            }
            case "CANCELED" -> {
                paymentRepository.findByPgTxId(paymentKey).ifPresentOrElse(p -> {
                    p.markCancelled();
                    paymentRepository.save(p);
                    log.info("토스 결제 취소 처리: paymentKey={}", paymentKey);
                }, () -> log.warn("취소 대상 토스 결제 없음: paymentKey={}", paymentKey));
            }
            default -> log.warn("알 수 없는 토스 결제 상태: {}", status);
        }
    }

    @Transactional
    public void handleOnsite(OnsitePaymentRequest req) {
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

    private Long extractReservationId(String id) {
        try {
            return Long.parseLong(id.split("_")[0]);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "reservationId 추출 실패: " + id);
        }
    }
}