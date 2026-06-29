-- ============================================================
-- V4: deleted_at → is_deleted(TINYINT) 전환
-- ============================================================

-- 1. users
ALTER TABLE users
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '논리 삭제 여부 (0=정상, 1=삭제)',
    DROP COLUMN deleted_at,
    DROP INDEX uq_user_email;
-- ※ 이메일 중복 체크는 서비스 레이어에서
--   is_deleted = 0 조건으로 직접 검사

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