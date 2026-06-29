-- ============================================================
-- V4: deleted_at → is_deleted(TINYINT) 전환
-- 대상: users, exhibition, reservation,
--       reservation_attendee, payment, settlement
-- ============================================================

-- 1. users
--    이메일 UNIQUE 제약도 변경 (탈퇴 유저 이메일 재가입 허용)
ALTER TABLE users
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '논리 삭제 여부 (0=정상, 1=삭제)',
    DROP COLUMN deleted_at,
    DROP INDEX uq_user_email,
    ADD UNIQUE KEY uq_user_email_active (email, is_deleted);

-- 2. exhibition
ALTER TABLE exhibition
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '논리 삭제 여부 (0=정상, 1=삭제)',
    DROP COLUMN deleted_at;

-- 3. reservation
ALTER TABLE reservation
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '논리 삭제 여부 (0=정상, 1=삭제)',
    DROP COLUMN deleted_at;

-- 4. reservation_attendee
ALTER TABLE reservation_attendee
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '논리 삭제 여부 (0=정상, 1=삭제)',
    DROP COLUMN deleted_at;

-- 5. payment
ALTER TABLE payment
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '논리 삭제 여부 (0=정상, 1=삭제)',
    DROP COLUMN deleted_at;

-- 6. settlement
ALTER TABLE settlement
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '논리 삭제 여부 (0=정상, 1=삭제)',
    DROP COLUMN deleted_at;