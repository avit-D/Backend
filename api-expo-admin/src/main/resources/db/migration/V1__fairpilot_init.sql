-- ============================================================
--  FairPilot · Flyway Migration
--  V1__fairpilot_init.sql
--  MySQL 8.x / InnoDB / utf8mb4_unicode_ci
--  기준: 기획안 v2.7
--  총 28개 테이블
--
--  변경 이력:
--    - users           (user → users: MySQL 예약어 충돌 방지)
--    - sessions        (session → sessions: MySQL 예약어 충돌 방지)
--    - exhibition      enforce_staff_qualification 추가 (STAFF 하드 게이트)
--    - exhibition_staff 신규 추가
--    - time_slot       version 컬럼 제거 (원자 UPDATE로 통일 - 팀 합의)
--    - reservation     movement_mode, group_size, reservation_source 추가
--                      ticket_qr_token 제거 (attendee별 QR로 이동)
--    - reservation_attendee 신규 추가 (participant → attendee로 확정)
--    - name_tag        exhibition_id, attendee_id NULL허용, AVAILABLE 상태 추가
--                      issued_at NULL 허용
--    - checkin_log     신규 추가 (체크인 감사 로그)
--    - visit_log       attendee_id, head_count, is_auto_exit 추가
--                      visitor_user_id 제거
--    - visit_dwell     attendee_id, head_count, is_estimated, close_reason 추가
--    - recommended_route preference_id, route_status 추가
--    - route_stop      congestion_snapshot 추가
--    - booth_embedding source_hash 추가
--    - settlement      online_amount, onsite_amount, paid_out_at 추가
--    - education_completion video_completed 추가 (영상 시청 완료 필수 조건)
--    - stat_visit_point, stat_congestion_hourly 신규 추가 (통계 집계용)
-- ============================================================

CREATE DATABASE IF NOT EXISTS fairpilot
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE fairpilot;

-- ============================================================
-- 1. users
--    전 역할 공통 계정 · Soft Delete
-- ============================================================
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(30)      NULL,
    role          ENUM(
                    'PLATFORM_ADMIN',
                    'EXPO_ADMIN',
                    'EXHIBITOR',
                    'VISITOR',
                    'ACCOUNTANT',
                    'STAFF'
                  )            NOT NULL DEFAULT 'VISITOR',
    deleted_at    DATETIME         NULL COMMENT '논리 삭제 시각',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_email (email),
    KEY idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자 계정 (전 역할 공통, Soft Delete)';


-- ============================================================
-- 2. exhibition
--    박람회 (멀티테넌트 경계)
--    enforce_staff_qualification: TRUE면 STAFF 체크인 하드 게이트
-- ============================================================
CREATE TABLE exhibition (
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    title                        VARCHAR(200) NOT NULL,
    slug                         VARCHAR(120) NOT NULL COMMENT 'URL 슬러그',
    venue                        VARCHAR(200)     NULL,
    address                      VARCHAR(255)     NULL,
    floor_map_meta               JSON             NULL COMMENT '배치도 축척 게이트 좌표',
    start_date                   DATE         NOT NULL,
    end_date                     DATE         NOT NULL,
    status                       ENUM('DRAFT','OPEN','CLOSED')
                                               NOT NULL DEFAULT 'DRAFT',
    enforce_staff_qualification  TINYINT(1)   NOT NULL DEFAULT 0
                                                       COMMENT 'TRUE면 LMS 미수료 STAFF 체크인/현장결제 차단 (하드 게이트)',
    created_by                   BIGINT       NOT NULL COMMENT 'users.id (PLATFORM_ADMIN)',
    deleted_at                   DATETIME         NULL COMMENT '논리 삭제 시각',
    created_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_exh_slug (slug),
    KEY idx_exh_status (status),
    CONSTRAINT fk_exh_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='박람회 (멀티테넌트 경계, STAFF 하드 게이트 제어)';


-- ============================================================
-- 3. exhibition_admin
--    박람회 ↔ EXPO_ADMIN N:M 매핑
-- ============================================================
CREATE TABLE exhibition_admin (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    exhibition_id BIGINT   NOT NULL,
    user_id       BIGINT   NOT NULL COMMENT 'users.id (EXPO_ADMIN)',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_exadm (exhibition_id, user_id),
    CONSTRAINT fk_exadm_exh  FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE CASCADE,
    CONSTRAINT fk_exadm_user FOREIGN KEY (user_id)       REFERENCES users      (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='박람회 ↔ EXPO_ADMIN N:M 매핑';


-- ============================================================
-- 4. exhibition_staff
--    박람회 ↔ STAFF N:M 매핑 (신규)
-- ============================================================
CREATE TABLE exhibition_staff (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    exhibition_id BIGINT   NOT NULL,
    user_id       BIGINT   NOT NULL COMMENT 'users.id (STAFF)',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_exstaff (exhibition_id, user_id),
    CONSTRAINT fk_exstaff_exh  FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE CASCADE,
    CONSTRAINT fk_exstaff_user FOREIGN KEY (user_id)       REFERENCES users      (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='박람회 ↔ STAFF N:M 매핑';


-- ============================================================
-- 5. exhibitor
--    참가업체
-- ============================================================
CREATE TABLE exhibitor (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    exhibition_id   BIGINT       NOT NULL,
    company_name    VARCHAR(200) NOT NULL,
    intro           TEXT             NULL COMMENT 'RAG 인덱싱 대상',
    website         VARCHAR(255)     NULL,
    account_user_id BIGINT           NULL COMMENT '스캔용 EXHIBITOR 계정 (users.id)',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exhibitor_exh (exhibition_id),
    CONSTRAINT fk_exhibitor_exh  FOREIGN KEY (exhibition_id)   REFERENCES exhibition (id) ON DELETE RESTRICT,
    CONSTRAINT fk_exhibitor_user FOREIGN KEY (account_user_id) REFERENCES users      (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='참가업체';


-- ============================================================
-- 6. booth_category
--    부스 카테고리 (테넌트별)
-- ============================================================
CREATE TABLE booth_category (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    exhibition_id BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bcat (exhibition_id, name),
    CONSTRAINT fk_bcat_exh FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='부스 카테고리 (테넌트별)';


-- ============================================================
-- 7. booth
--    전시 부스
-- ============================================================
CREATE TABLE booth (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    exhibition_id BIGINT       NOT NULL,
    exhibitor_id  BIGINT       NOT NULL,
    category_id   BIGINT           NULL,
    name          VARCHAR(200) NOT NULL,
    description   TEXT             NULL COMMENT 'RAG 인덱싱 대상',
    tags          VARCHAR(500)     NULL COMMENT '콤마 구분 태그',
    pos_x         INT              NULL COMMENT '배치 X좌표',
    pos_y         INT              NULL COMMENT '배치 Y좌표',
    floor         INT              NULL COMMENT '층',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_booth_exh       (exhibition_id),
    KEY idx_booth_exhibitor (exhibitor_id),
    CONSTRAINT fk_booth_exh       FOREIGN KEY (exhibition_id) REFERENCES exhibition    (id) ON DELETE RESTRICT,
    CONSTRAINT fk_booth_exhibitor FOREIGN KEY (exhibitor_id)  REFERENCES exhibitor     (id) ON DELETE RESTRICT,
    CONSTRAINT fk_booth_category  FOREIGN KEY (category_id)   REFERENCES booth_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='전시 부스 (RAG 인덱싱 대상: description, tags)';


-- ============================================================
-- 8. sessions
--    세미나 / 세션
--    (session → sessions: MySQL 예약어 충돌 방지)
-- ============================================================
CREATE TABLE sessions (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    exhibition_id     BIGINT       NOT NULL,
    host_exhibitor_id BIGINT           NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT             NULL,
    location          VARCHAR(120)     NULL,
    start_at          DATETIME     NOT NULL,
    end_at            DATETIME     NOT NULL,
    capacity          INT          NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_exh   (exhibition_id),
    KEY idx_session_start (start_at),
    CONSTRAINT fk_session_exh       FOREIGN KEY (exhibition_id)     REFERENCES exhibition (id) ON DELETE RESTRICT,
    CONSTRAINT fk_session_exhibitor FOREIGN KEY (host_exhibitor_id) REFERENCES exhibitor  (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='세미나 / 세션';


-- ============================================================
-- 9. time_slot
--    입장 시간대 슬롯 · 동시성 핵심
--    원자 UPDATE로 통일 (팀 합의: version 컬럼 제거)
--    UPDATE time_slot SET reserved_count = reserved_count + :groupSize
--    WHERE id = :id AND reserved_count + :groupSize <= capacity;
-- ============================================================
CREATE TABLE time_slot (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    exhibition_id  BIGINT   NOT NULL,
    start_at       DATETIME NOT NULL,
    end_at         DATETIME NOT NULL,
    capacity       INT      NOT NULL DEFAULT 0,
    reserved_count INT      NOT NULL DEFAULT 0 COMMENT '예약 확정/보류 인원수 (원자 UPDATE 대상)',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_slot_exh (exhibition_id),
    CONSTRAINT fk_slot_exh    FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE RESTRICT,
    CONSTRAINT chk_slot_count CHECK (reserved_count >= 0 AND reserved_count <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='입장 시간대 슬롯 (동시성: 원자 UPDATE, version 없음)';


-- ============================================================
-- 10. ticket_type
--     티켓 타입 (무료 / 유료 / VIP)
-- ============================================================
CREATE TABLE ticket_type (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    exhibition_id BIGINT         NOT NULL,
    name          VARCHAR(100)   NOT NULL,
    price         DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    quota         INT                NULL COMMENT '발행 한도 (NULL = 무제한)',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ticket_exh (exhibition_id),
    CONSTRAINT fk_ticket_exh FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='티켓 타입 (무료/유료/VIP)';


-- ============================================================
-- 11. reservation
--     예약 헤더 (결제·정원 차감 단위) · Soft Delete
--     ticket_qr_token 제거 → reservation_attendee.ticket_qr_token으로 분리
--     movement_mode: GROUP(도슨트/단체) / INDIVIDUAL(개별 추적)
--     group_size: 정원 차감 단위, reservation_attendee 생성 기준
--     reservation_source: ONLINE / ONSITE_MANUAL(워크인)
-- ============================================================
CREATE TABLE reservation (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    user_id            BIGINT      NOT NULL COMMENT '예약 대표자 (users.id, VISITOR)',
    exhibition_id      BIGINT      NOT NULL,
    time_slot_id       BIGINT          NULL,
    ticket_type_id     BIGINT          NULL,
    movement_mode      ENUM('INDIVIDUAL','GROUP')
                                   NOT NULL DEFAULT 'INDIVIDUAL'
                                           COMMENT 'INDIVIDUAL=개별QR추적, GROUP=대표QR 단체이동',
    group_size         INT         NOT NULL DEFAULT 1
                                           COMMENT '예약 인원수 (정원 차감 단위, 기본 1)',
    status             ENUM(
                         'PENDING',
                         'PAID',
                         'CANCELLED',
                         'REFUNDED',
                         'CHECKED_IN'
                       )           NOT NULL DEFAULT 'PENDING',
    reservation_source ENUM('ONLINE','ONSITE_MANUAL')
                                   NOT NULL DEFAULT 'ONLINE'
                                           COMMENT 'ONSITE_MANUAL=워크인 현장 등록',
    deleted_at         DATETIME        NULL COMMENT '논리 삭제 (@SQLDelete)',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_resv_user       (user_id),
    KEY idx_resv_slot       (time_slot_id),
    KEY idx_resv_exh_status (exhibition_id, status),
    CONSTRAINT fk_resv_user        FOREIGN KEY (user_id)        REFERENCES users      (id) ON DELETE RESTRICT,
    CONSTRAINT fk_resv_exh         FOREIGN KEY (exhibition_id)  REFERENCES exhibition (id) ON DELETE RESTRICT,
    CONSTRAINT fk_resv_slot        FOREIGN KEY (time_slot_id)   REFERENCES time_slot  (id) ON DELETE SET NULL,
    CONSTRAINT fk_resv_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_type(id) ON DELETE SET NULL,
    CONSTRAINT chk_group_size      CHECK (group_size >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='예약 헤더 (Soft Delete, 팀/개인 예약, 워크인 지원)';


-- ============================================================
-- 12. reservation_attendee
--     입장·동선 추적 기본 단위 (신규)
--     INDIVIDUAL: group_size만큼 N행, 각자 QR
--     GROUP: 대표 1행(is_group_leader=TRUE) 필수, 나머지 명단행 선택
--     checkin_status / attendee_status 분리 (v2.3.1 반영)
-- ============================================================
CREATE TABLE reservation_attendee (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    reservation_id   BIGINT        NOT NULL,
    exhibition_id    BIGINT        NOT NULL COMMENT '조회 편의 denormalize',
    linked_user_id   BIGINT            NULL COMMENT '참석자가 계정 보유 시 연결 (users.id)',
    name             VARCHAR(100)  NOT NULL COMMENT '참석자 표시명',
    phone            VARCHAR(30)       NULL COMMENT '수기 체크인 조회용',
    email            VARCHAR(255)      NULL,
    is_group_leader  TINYINT(1)    NOT NULL DEFAULT 0
                                           COMMENT 'GROUP 대표 QR 대상 (GROUP 예약은 정확히 1명)',
    ticket_qr_token  VARCHAR(64)       NULL COMMENT 'UNIQUE. INDIVIDUAL=각자, GROUP=대표만 발급',
    checkin_status   ENUM('NOT_CHECKED_IN','CHECKED_IN')
                                   NOT NULL DEFAULT 'NOT_CHECKED_IN',
    attendee_status  ENUM('ACTIVE','CANCELLED','NO_SHOW')
                                   NOT NULL DEFAULT 'ACTIVE',
    checked_in_at    DATETIME          NULL,
    deleted_at       DATETIME          NULL COMMENT '논리 삭제',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attendee_qr       (ticket_qr_token),
    KEY idx_attendee_resv           (reservation_id),
    KEY idx_attendee_exh            (exhibition_id),
    KEY idx_attendee_linked_user    (linked_user_id),
    CONSTRAINT fk_attendee_resv        FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendee_exh         FOREIGN KEY (exhibition_id)  REFERENCES exhibition  (id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendee_linked_user FOREIGN KEY (linked_user_id) REFERENCES users       (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='참석자 단위 추적 (INDIVIDUAL=각자QR, GROUP=대표QR, checkin/attendee 상태 분리)';


-- ============================================================
-- 13. payment
--     결제 · Soft Delete · webhook 멱등 (pg_tx_id UNIQUE)
--     pg_provider=ONSITE: 현장 결제
--     v2.7: reservation 1—N 스키마이나 MVP 정책은 1—1 권장
--     (서비스 계층에서 활성 PAID/READY payment 1건만 유지하도록 제어)
-- ============================================================
CREATE TABLE payment (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT         NOT NULL,
    pg_provider    VARCHAR(40)        NULL COMMENT 'PG사 코드 또는 ONSITE',
    pg_tx_id       VARCHAR(120)       NULL COMMENT 'PG 거래 ID (멱등키). ONSITE는 내부 영수번호',
    amount         DECIMAL(12, 2) NOT NULL,
    fee_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '수수료',
    status         ENUM(
                     'READY',
                     'PAID',
                     'FAILED',
                     'CANCELLED',
                     'REFUNDED'
                   )              NOT NULL DEFAULT 'READY',
    paid_at        DATETIME           NULL COMMENT '결제 완료 시각',
    deleted_at     DATETIME           NULL COMMENT '논리 삭제',
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pay_tx   (pg_tx_id),
    KEY idx_pay_resv       (reservation_id),
    CONSTRAINT fk_pay_resv FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='결제 (Soft Delete, webhook 멱등 pg_tx_id, ONSITE 현장결제 지원)';


-- ============================================================
-- 14. name_tag
--     QR 목걸이 (사전 인쇄 풀 + 입구 2-스캔 바인딩)
--     AVAILABLE: 인쇄됐지만 미바인딩 재고
--     ISSUED: 참석자에 바인딩 완료 (attendee당 활성 1건)
--     REVOKED: 분실/교환으로 무효화 (이력 보존)
-- ============================================================
CREATE TABLE name_tag (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    exhibition_id     BIGINT      NOT NULL COMMENT '사전 발급 재고의 소속 행사',
    attendee_id       BIGINT          NULL COMMENT '바인딩 전 NULL (reservation_attendee.id)',
    token             VARCHAR(64) NOT NULL COMMENT 'QR 인코딩 UUID (UNIQUE)',
    status            ENUM('AVAILABLE','ISSUED','REVOKED')
                                  NOT NULL DEFAULT 'AVAILABLE',
    issued_by_user_id BIGINT          NULL COMMENT '바인딩 처리자 (users.id, EXPO_ADMIN/STAFF)',
    issued_at         DATETIME        NULL COMMENT '바인딩(ISSUED) 시각. AVAILABLE이면 NULL',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_nametag_token (token),
    KEY idx_nametag_exh         (exhibition_id),
    KEY idx_nametag_attendee    (attendee_id),
    KEY idx_nametag_status      (status),
    CONSTRAINT fk_nametag_exh      FOREIGN KEY (exhibition_id)    REFERENCES exhibition          (id) ON DELETE RESTRICT,
    CONSTRAINT fk_nametag_attendee FOREIGN KEY (attendee_id)      REFERENCES reservation_attendee(id) ON DELETE SET NULL,
    CONSTRAINT fk_nametag_issuer   FOREIGN KEY (issued_by_user_id) REFERENCES users              (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='QR 네임태그 (사전인쇄풀 AVAILABLE→바인딩 ISSUED→무효 REVOKED)';


-- ============================================================
-- 15. checkin_log
--     체크인 감사 로그 (신규)
--     모든 체크인 이벤트의 단일 출처 (발급방식, 재발급 사유 등)
-- ============================================================
CREATE TABLE checkin_log (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    exhibition_id         BIGINT       NOT NULL,
    reservation_id        BIGINT       NOT NULL,
    attendee_id           BIGINT       NOT NULL,
    name_tag_id           BIGINT           NULL,
    checkin_method        ENUM(
                            'QR_SELF',
                            'MANUAL_SEARCH',
                            'ONSITE_MANUAL',
                            'WALK_IN',
                            'REISSUE'
                          )            NOT NULL COMMENT 'QR_SELF=QR스캔, MANUAL_SEARCH=수기조회, ONSITE_MANUAL=현장수기, WALK_IN=워크인, REISSUE=재발급',
    checked_in_by_user_id BIGINT       NOT NULL COMMENT 'EXPO_ADMIN 또는 STAFF (users.id)',
    checked_in_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    memo                  VARCHAR(500)     NULL COMMENT '수기 사유 / 재발급 사유',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_checkin_exh      (exhibition_id),
    KEY idx_checkin_attendee (attendee_id),
    KEY idx_checkin_resv     (reservation_id),
    CONSTRAINT fk_checkin_exh      FOREIGN KEY (exhibition_id)         REFERENCES exhibition          (id) ON DELETE RESTRICT,
    CONSTRAINT fk_checkin_resv     FOREIGN KEY (reservation_id)        REFERENCES reservation         (id) ON DELETE RESTRICT,
    CONSTRAINT fk_checkin_attendee FOREIGN KEY (attendee_id)           REFERENCES reservation_attendee(id) ON DELETE RESTRICT,
    CONSTRAINT fk_checkin_nametag  FOREIGN KEY (name_tag_id)           REFERENCES name_tag            (id) ON DELETE SET NULL,
    CONSTRAINT fk_checkin_by       FOREIGN KEY (checked_in_by_user_id) REFERENCES users               (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='체크인 감사 로그 (발급방식 단일 출처, 수기/재발급 사유)';


-- ============================================================
-- 16. visit_log
--     모든 스캔 1건 = 1행 · ENTRY/EXIT 토글
--     attendee_id: 동선 추적 기본 단위
--     head_count: INDIVIDUAL=1, GROUP=reservation.group_size
--     is_auto_exit: 60분 타임아웃 또는 타 부스 ENTRY로 자동 생성된 EXIT
--     is_manual: 수기 입력 여부 (인터넷 오류 대응)
-- ============================================================
CREATE TABLE visit_log (
    id                 BIGINT    NOT NULL AUTO_INCREMENT,
    exhibition_id      BIGINT    NOT NULL,
    attendee_id        BIGINT    NOT NULL COMMENT '동선 추적 단위 (reservation_attendee.id)',
    name_tag_id        BIGINT    NOT NULL,
    scan_point_type    ENUM('BOOTH','SESSION','GATE')
                                 NOT NULL,
    scan_point_id      BIGINT        NULL COMMENT 'booth.id / sessions.id (GATE는 NULL)',
    scan_type          ENUM('ENTRY','EXIT')
                                 NOT NULL,
    head_count         INT       NOT NULL DEFAULT 1
                                          COMMENT 'INDIVIDUAL=1, GROUP=reservation.group_size',
    scanned_by_user_id BIGINT        NULL COMMENT 'EXHIBITOR 또는 게이트 담당자. 자동EXIT는 NULL',
    is_manual          TINYINT(1) NOT NULL DEFAULT 0 COMMENT '수기 입력 여부 (1=수기)',
    is_auto_exit       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '시스템 자동 합성 EXIT (60분/타부스ENTRY)',
    scanned_at         DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at         DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vlog_exh_time   (exhibition_id, scanned_at),
    KEY idx_vlog_attendee   (attendee_id, scanned_at),
    KEY idx_vlog_point      (scan_point_type, scan_point_id),
    CONSTRAINT fk_vlog_exh      FOREIGN KEY (exhibition_id)      REFERENCES exhibition          (id) ON DELETE RESTRICT,
    CONSTRAINT fk_vlog_attendee FOREIGN KEY (attendee_id)        REFERENCES reservation_attendee(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vlog_nametag  FOREIGN KEY (name_tag_id)        REFERENCES name_tag            (id) ON DELETE CASCADE,
    CONSTRAINT fk_vlog_scanner  FOREIGN KEY (scanned_by_user_id) REFERENCES users              (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='스캔 로그 (1스캔=1행, ENTRY/EXIT 토글, 수기/자동EXIT 구분)';


-- ============================================================
-- 17. visit_dwell
--     체류시간 집계 (배치 적재, 통계·AI 추천 입력)
--     close_reason: ENTRY-EXIT 쌍이 닫힌 이유
--     is_estimated: 자동 추정 마감 여부
-- ============================================================
CREATE TABLE visit_dwell (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    exhibition_id   BIGINT    NOT NULL,
    attendee_id     BIGINT    NOT NULL,
    scan_point_type ENUM('BOOTH','SESSION') NOT NULL,
    scan_point_id   BIGINT    NOT NULL,
    entry_at        DATETIME  NOT NULL,
    exit_at         DATETIME      NULL COMMENT '미종결이면 NULL',
    dwell_seconds   INT       NOT NULL DEFAULT 0 COMMENT 'exit_at - entry_at (초)',
    head_count      INT       NOT NULL DEFAULT 1 COMMENT '인-분 가중용 (GROUP=group_size)',
    is_estimated    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '자동 추정 마감 여부',
    close_reason    ENUM(
                      'NORMAL_EXIT',
                      'NEXT_ENTRY_AUTO',
                      'TIMEOUT_AUTO',
                      'ADMIN_MANUAL'
                    )             NULL COMMENT '체류 종료 이유 (미종결이면 NULL)',
    created_at      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_dwell_exh_point (exhibition_id, scan_point_type, scan_point_id),
    KEY idx_dwell_attendee  (attendee_id),
    CONSTRAINT fk_dwell_exh      FOREIGN KEY (exhibition_id) REFERENCES exhibition          (id) ON DELETE CASCADE,
    CONSTRAINT fk_dwell_attendee FOREIGN KEY (attendee_id)   REFERENCES reservation_attendee(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='체류시간 집계 (배치, close_reason으로 자동/수동 마감 구분)';


-- ============================================================
-- 18. stat_visit_point
--     통계 집계 read-model (배치 정형화, 부스/세션별)
--     v2.7: sum_dwell_sec, dwell_count 추가
--     하드 FK 없음 (visit_log/visit_dwell 파생, 배치 재집계/재실행 안전)
--     상위 구간(일/전체) 평균은 SUM(sum_dwell_sec)/SUM(dwell_count)로 계산.
--     avg_dwell_sec는 단일 버킷 표시용 파생 편의값일 뿐 집계에 직접 쓰지 않음.
-- ============================================================
CREATE TABLE stat_visit_point (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    exhibition_id   BIGINT   NOT NULL COMMENT '테넌트 스코프 (파생, FK 없음)',
    scan_point_type ENUM('BOOTH','SESSION') NOT NULL,
    scan_point_id   BIGINT   NOT NULL COMMENT 'booth.id 또는 sessions.id',
    stat_date       DATE     NOT NULL COMMENT '집계 날짜 (KST 기준)',
    stat_hour       TINYINT  NOT NULL COMMENT '집계 시 (0-23, KST)',
    visit_count     INT      NOT NULL DEFAULT 0 COMMENT '스캔/ENTRY 건수',
    visitor_count   INT      NOT NULL DEFAULT 0 COMMENT 'head_count 합 = 인원 (GROUP 가중)',
    unique_attendee INT      NOT NULL DEFAULT 0 COMMENT 'distinct attendee = 태그 수 (GROUP 대표 1)',
    sum_dwell_sec   BIGINT   NOT NULL DEFAULT 0 COMMENT '마감된 체류시간 합 (상위 구간 평균 분자)',
    dwell_count     INT      NOT NULL DEFAULT 0 COMMENT '마감된 체류 쌍 수 (상위 구간 평균 분모)',
    avg_dwell_sec   INT      NOT NULL DEFAULT 0 COMMENT '해당 버킷 평균 (=sum/count, 파생 편의값 전용)',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_stat_point (exhibition_id, scan_point_type, scan_point_id, stat_date, stat_hour),
    KEY idx_stat_exh_date (exhibition_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='통계 집계 read-model (부스/세션별 시간대 방문/인원/체류, 하드FK 없음, 멱등 upsert)';


-- ============================================================
-- 19. stat_congestion_hourly
--     혼잡도 시간대별 스냅샷 read-model (배치 정형화)
--     하드 FK 없음. 라이브 혼잡도는 Redis, 이 테이블은 히스토리/대시보드용(준실시간)
-- ============================================================
CREATE TABLE stat_congestion_hourly (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    exhibition_id BIGINT   NOT NULL COMMENT '테넌트 스코프 (파생, FK 없음)',
    stat_date     DATE     NOT NULL COMMENT 'KST 기준',
    stat_hour     TINYINT  NOT NULL COMMENT '0-23, KST',
    head_count    INT      NOT NULL DEFAULT 0 COMMENT '해당 시간대 최대 동시 입장 인원',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_stat_cong (exhibition_id, stat_date, stat_hour),
    KEY idx_stat_cong_exh (exhibition_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='혼잡도 시간대별 스냅샷 read-model (배치 집계, 하드FK 없음, 멱등 upsert)';


-- ============================================================
-- 20. user_preference
--     AI 추천 입력 (관심사 / 가용시간 / 필수부스)
-- ============================================================
CREATE TABLE user_preference (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id              BIGINT       NOT NULL COMMENT 'VISITOR (users.id)',
    exhibition_id        BIGINT       NOT NULL,
    interest_tags        VARCHAR(500)     NULL COMMENT '관심사 (콤마 구분)',
    available_minutes    INT              NULL COMMENT '가용 시간 (분)',
    must_visit_booth_ids JSON             NULL COMMENT '필수 방문 부스 id 배열',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pref_user_exh (user_id, exhibition_id),
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id)       REFERENCES users      (id) ON DELETE CASCADE,
    CONSTRAINT fk_pref_exh  FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='AI 추천 입력 (관심사/가용시간/필수부스)';


-- ============================================================
-- 21. recommended_route
--     AI 추천 동선
-- ============================================================
CREATE TABLE recommended_route (
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    user_id           BIGINT   NOT NULL,
    exhibition_id     BIGINT   NOT NULL,
    preference_id     BIGINT       NULL COMMENT 'user_preference 연결',
    rationale         TEXT         NULL COMMENT '전체 추천 사유 (LLM 생성)',
    total_est_minutes INT          NULL COMMENT '총 예상 소요 (분)',
    route_status      ENUM('CREATED','EXPIRED','DELETED')
                                   NOT NULL DEFAULT 'CREATED',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_route_user_exh (user_id, exhibition_id),
    CONSTRAINT fk_route_user FOREIGN KEY (user_id)       REFERENCES users          (id) ON DELETE CASCADE,
    CONSTRAINT fk_route_exh  FOREIGN KEY (exhibition_id) REFERENCES exhibition     (id) ON DELETE CASCADE,
    CONSTRAINT fk_route_pref FOREIGN KEY (preference_id) REFERENCES user_preference(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='AI 추천 동선';


-- ============================================================
-- 22. route_stop
--     추천 동선 경유 부스
-- ============================================================
CREATE TABLE route_stop (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    route_id             BIGINT       NOT NULL,
    booth_id             BIGINT       NOT NULL,
    visit_order          INT          NOT NULL COMMENT '방문 순서',
    est_minutes          INT              NULL COMMENT '예상 체류 (분)',
    reason               VARCHAR(500)     NULL COMMENT '부스별 추천 사유',
    congestion_snapshot  INT              NULL COMMENT '추천 당시 혼잡도 스냅샷',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stop_route (route_id),
    CONSTRAINT fk_stop_route FOREIGN KEY (route_id) REFERENCES recommended_route(id) ON DELETE CASCADE,
    CONSTRAINT fk_stop_booth FOREIGN KEY (booth_id) REFERENCES booth            (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='추천 동선 경유 부스 (혼잡도 스냅샷 포함)';


-- ============================================================
-- 23. booth_embedding
--     RAG 임베딩 (MySQL JSON float[])
--     source_hash: description/tags 변경 감지용
-- ============================================================
CREATE TABLE booth_embedding (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    booth_id    BIGINT      NOT NULL,
    embedding   JSON        NOT NULL COMMENT '임베딩 벡터 (float[] JSON)',
    model       VARCHAR(100)    NULL COMMENT '임베딩 모델명',
    source_hash VARCHAR(64)     NULL COMMENT 'booth.name/description/tags 변경 감지용',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_emb_booth (booth_id),
    CONSTRAINT fk_emb_booth FOREIGN KEY (booth_id) REFERENCES booth (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='부스 임베딩 (RAG, MySQL JSON, source_hash 변경감지)';


-- ============================================================
-- 24. ad_slot
--     광고 슬롯 (플랫폼 공통 또는 박람회별)
-- ============================================================
CREATE TABLE ad_slot (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    exhibition_id BIGINT             NULL COMMENT 'NULL = 플랫폼 공통',
    placement     VARCHAR(80)    NOT NULL COMMENT 'HOME_TOP / EXPO_BANNER / BOOTH_RECOMMEND 등',
    base_price    DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status        ENUM('ACTIVE','INACTIVE')
                                 NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_adslot_exh (exhibition_id),
    CONSTRAINT fk_adslot_exh FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='광고 슬롯 (플랫폼/박람회)';


-- ============================================================
-- 25. advertisement
--     광고 집행
-- ============================================================
CREATE TABLE advertisement (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    ad_slot_id      BIGINT         NOT NULL,
    advertiser_name VARCHAR(200)   NOT NULL,
    exhibitor_id    BIGINT             NULL COMMENT '연계 참가업체',
    title           VARCHAR(200)   NOT NULL,
    image_url       VARCHAR(500)       NULL,
    link_url        VARCHAR(500)       NULL,
    start_at        DATETIME       NOT NULL,
    end_at          DATETIME       NOT NULL,
    price           DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status          ENUM('DRAFT','ACTIVE','PAUSED','EXPIRED')
                                   NOT NULL DEFAULT 'DRAFT',
    impressions     BIGINT         NOT NULL DEFAULT 0,
    clicks          BIGINT         NOT NULL DEFAULT 0,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ad_slot   (ad_slot_id),
    KEY idx_ad_status (status),
    CONSTRAINT fk_ad_slot      FOREIGN KEY (ad_slot_id)  REFERENCES ad_slot  (id) ON DELETE CASCADE,
    CONSTRAINT fk_ad_exhibitor FOREIGN KEY (exhibitor_id) REFERENCES exhibitor (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='광고 집행';


-- ============================================================
-- 26. settlement
--     정산 (온라인+현장 매출 - 수수료 + 광고수익 = 순지급액)
--     online_amount / onsite_amount 분리 (v2.4 반영)
-- ============================================================
CREATE TABLE settlement (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    exhibition_id  BIGINT         NOT NULL,
    period_start   DATE           NOT NULL,
    period_end     DATE           NOT NULL,
    gross_amount   DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '총 매출 (온라인+현장)',
    online_amount  DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT 'PG 결제 매출',
    onsite_amount  DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT 'ONSITE 현장 결제 매출',
    fee_amount     DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT 'PG/플랫폼 수수료',
    ad_revenue     DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '광고 수익',
    net_payout     DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '순지급액',
    status         ENUM('PENDING','CONFIRMED','PAID_OUT')
                                  NOT NULL DEFAULT 'PENDING',
    generated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_out_at    DATETIME           NULL COMMENT '지급 완료 시각',
    deleted_at     DATETIME           NULL COMMENT '논리 삭제',
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_settle_exh    (exhibition_id),
    KEY idx_settle_status (status),
    CONSTRAINT fk_settle_exh FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='정산 (온라인/현장 매출 분리, 광고수익 포함)';


-- ============================================================
-- 27. education_guide
--     LMS 교육·매뉴얼 가이드
--     텍스트만 → 확인 완료 시 passed=true
--     영상 있음 → video_completed=true 필수
--     영상+퀴즈 → video_completed AND 퀴즈통과 모두 필요
--     정답(answer)은 조회 응답에서 제외, 서버만 조회
-- ============================================================
CREATE TABLE education_guide (
    id              BIGINT           NOT NULL AUTO_INCREMENT,
    exhibition_id   BIGINT               NULL COMMENT 'NULL = 플랫폼 공통',
    target_role     ENUM('STAFF','EXHIBITOR')
                                     NOT NULL,
    category        VARCHAR(50)      NOT NULL COMMENT '행사안내/안전수칙/비상대처/부스설치/전기네트워크',
    title           VARCHAR(200)     NOT NULL,
    content         TEXT             NOT NULL COMMENT '텍스트 가이드 본문',
    video_url       VARCHAR(255)         NULL COMMENT '영상 URL (STAFF 교육에 사용)',
    is_required     TINYINT(1)       NOT NULL DEFAULT 0 COMMENT '필수 이수 여부',
    sort_order      INT              NOT NULL DEFAULT 0,
    quiz_questions  JSON                 NULL COMMENT '퀴즈 문항 배열. answer는 서버만 조회, 응답 DTO에서 제외',
    quiz_pass_score TINYINT UNSIGNED     NULL COMMENT '통과 기준 0~100 (NULL=퀴즈 없음)',
    status          ENUM('ACTIVE','INACTIVE')
                                     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_guide_role (target_role, status),
    KEY idx_guide_exh  (exhibition_id),
    CONSTRAINT fk_guide_exh FOREIGN KEY (exhibition_id) REFERENCES exhibition (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='LMS 가이드 (텍스트/영상/퀴즈, 정답 서버 전용)';


-- ============================================================
-- 28. education_completion
--     LMS 확인 완료 기록 · 자격 판정 기반
--     UNIQUE(guide_id, user_id) → 멱등 upsert 보장
--     video_completed: 영상 있는 가이드의 이수 필수 조건 (v2.3.1 반영)
--     passed: 가이드 유형별 모든 조건 충족 시 true
-- ============================================================
CREATE TABLE education_completion (
    id              BIGINT           NOT NULL AUTO_INCREMENT,
    guide_id        BIGINT           NOT NULL,
    user_id         BIGINT           NOT NULL,
    video_completed TINYINT(1)       NOT NULL DEFAULT 0
                                              COMMENT '영상 시청 완료 (영상 있는 가이드의 이수 필수 조건)',
    quiz_score      TINYINT UNSIGNED     NULL COMMENT '획득 점수 0~100 (NULL=퀴즈 없음)',
    passed          TINYINT(1)       NOT NULL DEFAULT 0
                                              COMMENT '이수 완료 여부 (가이드 유형별 모든 조건 충족 시 true)',
    confirmed_at    DATETIME             NULL COMMENT '수료 완료 시각',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_completion (guide_id, user_id),
    CONSTRAINT fk_comp_guide FOREIGN KEY (guide_id) REFERENCES education_guide (id) ON DELETE CASCADE,
    CONSTRAINT fk_comp_user  FOREIGN KEY (user_id)  REFERENCES users           (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='LMS 이수 완료 기록 (video_completed 필수조건, passed=이수완료)';

-- ============================================================
--  자격 판정 참고 쿼리 (서비스 계층 JPQL 기준)
--  is_required=TRUE 가이드 중 passed=TRUE인 것이 전부 일치하면 qualified=true
--
--  SELECT
--    (SELECT COUNT(*) FROM education_guide g
--     WHERE g.target_role = :role AND g.is_required = 1 AND g.status = 'ACTIVE'
--       AND (g.exhibition_id = :exhibitionId OR g.exhibition_id IS NULL)
--    ) AS required_total,
--    (SELECT COUNT(*) FROM education_completion c
--     JOIN education_guide g ON g.id = c.guide_id
--     WHERE c.user_id = :userId AND c.passed = 1
--       AND g.is_required = 1 AND g.status = 'ACTIVE'
--       AND (g.exhibition_id = :exhibitionId OR g.exhibition_id IS NULL)
--    ) AS completed_total
-- ============================================================
