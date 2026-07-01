package com.fairpilot.payment;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제 요청 발급
     * orderId = {reservationId}_{UUID} 형식으로 생성
     * READY Payment 레코드 저장 후 orderId + amount 반환
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

        paymentRepository.save(payment);
        log.info("결제 요청 발급: orderId={}, pgProvider={}, amount={}",
                orderId, req.pgProvider(), req.amount());

        return new PaymentInitiateResponse(orderId, req.amount());
    }

    /**
     * 포트원 webhook 멱등 처리
     */
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

    /**
     * 토스페이먼츠 webhook 멱등 처리
     *
     * [흐름]
     * 1. paymentKey로 중복 체크 (멱등)
     * 2. orderId로 READY 레코드 조회
     * 3. READY 있으면 금액 검증 후 pgTxId를 paymentKey로 교체
     * 4. READY 없으면 신규 생성
     */
    @Transactional
    public void handleTossWebhook(TossWebhookPayload payload) {
        String paymentKey    = payload.data().paymentKey();
        String orderId       = payload.data().orderId();
        String status        = payload.data().status();
        BigDecimal webhookAmount = payload.data().totalAmount();

        switch (status) {
            case "DONE" -> {
                // 1. paymentKey 중복 체크 (멱등)
                if (paymentRepository.findByPgTxId(paymentKey).isPresent()) {
                    log.info("중복 DONE webhook 무시: paymentKey={}", paymentKey);
                    return;
                }

                Long reservationId = extractReservationId(orderId);

                // 2. READY 레코드 조회
                Payment payment = paymentRepository
                        .findByReservationIdAndStatusIn(
                                reservationId, List.of(PaymentStatus.READY))
                        .map(p -> {
                            // 3. 금액 불일치 감지 (위변조 방어)
                            if (webhookAmount != null &&
                                    p.getAmount().compareTo(webhookAmount) != 0) {
                                log.warn("금액 불일치 감지! 저장={}, webhook={}, orderId={}",
                                        p.getAmount(), webhookAmount, orderId);
                                throw new BusinessException(ErrorCode.INVALID_INPUT,
                                        "결제 금액이 일치하지 않습니다. 위변조 의심");
                            }
                            // pgTxId: orderId → paymentKey 교체
                            // paymentKey 중복은 1번에서 이미 걸러졌으므로 UNIQUE 위반 없음
                            p.updatePgTxId(paymentKey);
                            return p;
                        })
                        .orElseGet(() -> {
                            // initiate() 없이 바로 webhook 온 케이스
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

    /** ONSITE 현장결제 처리 */
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

    /** ID 문자열에서 reservationId 추출 — {reservationId}_{uuid} 형식 */
    private Long extractReservationId(String id) {
        try {
            return Long.parseLong(id.split("_")[0]);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "reservationId 추출 실패: " + id);
        }
    }
}