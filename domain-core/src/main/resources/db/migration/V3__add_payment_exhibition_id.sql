-- V3: payment 테이블에 exhibition_id 컬럼 추가
ALTER TABLE payment
    ADD COLUMN exhibition_id BIGINT NULL
        COMMENT '박람회 ID (ONSITE 현장결제 집계용, 온라인 결제는 NULL 허용)'
        AFTER reservation_id,
    ADD KEY idx_pay_exh (exhibition_id);