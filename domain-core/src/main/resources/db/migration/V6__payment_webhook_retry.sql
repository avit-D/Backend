-- ============================================================
--  FairPilot · Flyway Migration
--  V6__payment_webhook_retry.sql
--  webhook 미도달 재시도 컬럼 추가
-- ============================================================

ALTER TABLE payment
    ADD COLUMN webhook_retry_count  TINYINT      NOT NULL DEFAULT 0
        COMMENT 'webhook 재시도 횟수 (최대 5회)',
    ADD COLUMN webhook_retry_at     DATETIME         NULL
        COMMENT '다음 재시도 예정 시각',
    ADD COLUMN webhook_last_error   VARCHAR(500)     NULL
        COMMENT '마지막 재시도 실패 사유';