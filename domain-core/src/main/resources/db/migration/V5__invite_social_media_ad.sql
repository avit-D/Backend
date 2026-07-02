-- ============================================================
--  FairPilot · Flyway Migration
--  V5__invite_social_media_ad.sql
--  MySQL 8.x / InnoDB / utf8mb4_unicode_ci
--
--  반영 배경:
--    - (프론트) 피드백 + 소셜 로그인(구글) 동시 반영
--    - users 테이블 변경이 초대 플로우와 소셜 로그인에서 겹치므로
--      이번 한 번의 마이그레이션으로 통합 처리
--    - SaaS 입점 신청은 B2B 특성상 앱 외부(이메일/영업)에서 받기로
--      팀 결정 → tenant_application 테이블 제외 (팀장 확인)
--
--  변경 내역:
--    1. users        - 관리자 초대 플로우 컬럼 추가, password_hash NULL 허용
--    2. users        - 소셜 로그인(구글) 컬럼 추가
--    3. media_asset   신규 - booth/exhibition/education_guide 공용 이미지·영상
--    4. advertisement - ad_type 컬럼 추가 (GOOGLE / INTERNAL 구분)
-- ============================================================

USE fairpilot;

-- ============================================================
-- 1~2. users 테이블 변경
--   - password_hash: NOT NULL → NULL 허용
--     (관리자 초대 시 비밀번호 미발급 상태 / 소셜 로그인 유저는 비밀번호 없음)
--   - invite_token / invite_expires_at / account_status: 관리자 초대 플로우
--   - social_provider / social_provider_id: 구글 소셜 로그인
-- ============================================================
ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(255) NULL COMMENT '소셜 로그인/초대 미완료 유저는 NULL 허용';

ALTER TABLE users
    ADD COLUMN invite_token       VARCHAR(64)  NULL COMMENT '관리자 초대 토큰 (1회성, 로그인 후 NULL 처리)' AFTER password_hash,
    ADD COLUMN invite_expires_at  DATETIME     NULL COMMENT '초대 토큰 만료 시각' AFTER invite_token,
    ADD COLUMN account_status     ENUM('INVITED','ACTIVE')
                                  NOT NULL DEFAULT 'ACTIVE'
                                  COMMENT 'INVITED=초대후 비밀번호 미설정, ACTIVE=정상 활성 계정' AFTER role,
    ADD COLUMN social_provider    ENUM('NONE','GOOGLE')
                                  NOT NULL DEFAULT 'NONE'
                                  COMMENT '소셜 로그인 제공자' AFTER account_status,
    ADD COLUMN social_provider_id VARCHAR(255) NULL COMMENT '소셜 제공자측 고유 ID (구글 sub 값)' AFTER social_provider;

-- 초대 토큰 UNIQUE (재사용 공격 방지)
ALTER TABLE users
    ADD CONSTRAINT uq_user_invite_token UNIQUE KEY (invite_token);

-- 소셜 로그인 식별자 UNIQUE (같은 구글 계정으로 중복 가입 방지)
ALTER TABLE users
    ADD CONSTRAINT uq_user_social UNIQUE KEY (social_provider, social_provider_id);

-- 초대 토큰 조회 성능용 인덱스 (만료 체크 배치에서 사용)
CREATE INDEX idx_user_invite_expires ON users (invite_expires_at);


-- ============================================================
-- 3. media_asset (신규)
--    booth / exhibition / education_guide 등 여러 도메인이 공유하는
--    이미지·영상 업로드 테이블 (AWS S3 저장)
--    하드 FK 없음 — owner_type별로 대상 테이블이 다르므로 다형성 연관관계로 처리
--    (정합성은 애플리케이션 레이어에서 owner_type+owner_id 조합으로 검증)
-- ============================================================
CREATE TABLE media_asset (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    owner_type    ENUM('BOOTH','EXHIBITION','EDUCATION_GUIDE','ADVERTISEMENT')
                               NOT NULL COMMENT '소속 도메인 구분',
    owner_id      BIGINT       NOT NULL COMMENT '소속 도메인의 PK (다형성, 하드 FK 없음)',
    media_type    ENUM('IMAGE','VIDEO')
                               NOT NULL COMMENT '미디어 유형',
    s3_key        VARCHAR(500) NOT NULL COMMENT 'S3 객체 키 (버킷 내부 경로)',
    s3_bucket     VARCHAR(120) NOT NULL COMMENT 'S3 버킷명 (환경별 분리 대비)',
    content_type  VARCHAR(100)     NULL COMMENT 'MIME 타입 (image/png, video/mp4 등)',
    file_size     BIGINT           NULL COMMENT '파일 크기 (byte)',
    display_order INT          NOT NULL DEFAULT 0 COMMENT '같은 owner 내 노출 순서 (썸네일/갤러리 정렬용)',
    is_thumbnail  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '대표 썸네일 여부 (owner당 1개만 TRUE 권장)',
    uploaded_by   BIGINT           NULL COMMENT '업로드한 users.id',
    is_deleted    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Soft Delete',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_media_owner (owner_type, owner_id),
    KEY idx_media_uploader (uploaded_by),
    CONSTRAINT fk_media_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='공용 미디어 자산 (S3) - booth/exhibition/LMS/광고 이미지·영상, 다형성 연관 (하드FK 없음)';


-- ============================================================
-- 4. advertisement 테이블 변경
--    ad_type 컬럼 추가: GOOGLE(구글애즈) / INTERNAL(자체 배너) 구분
--    GOOGLE인 경우 google_ad_unit_id만 사용하고, 기존 image_url/link_url 등은 NULL 허용 유지
-- ============================================================
ALTER TABLE advertisement
    ADD COLUMN ad_type           ENUM('GOOGLE','INTERNAL')
                                  NOT NULL DEFAULT 'INTERNAL'
                                  COMMENT 'GOOGLE=구글애즈 슬롯, INTERNAL=자체 배너 광고' AFTER ad_slot_id,
    ADD COLUMN google_ad_unit_id VARCHAR(120) NULL COMMENT '구글애즈 광고 단위 ID (ad_type=GOOGLE일 때만 사용)' AFTER ad_type;

CREATE INDEX idx_ad_type ON advertisement (ad_type);


-- ============================================================
--  참고: 관리자 초대 플로우 (서비스 레이어)
--  SaaS 입점 신청은 앱 외부(이메일/영업)에서 받으므로,
--  PLATFORM_ADMIN이 직접 EXPO_ADMIN 계정을 생성하는 절차만 정리.
--
--  1. PLATFORM_ADMIN이 EXPO_ADMIN 계정 생성 화면에서 이메일만 입력
--  2. users 테이블에 계정 생성
--     - password_hash = NULL
--     - account_status = 'INVITED'
--     - invite_token = 발급, invite_expires_at = 발급시각 + 72시간
--  3. 초대 이메일 발송 (invite_token 포함 링크)
--  4. 담당자가 링크 클릭 → 비밀번호 설정
--     → account_status = 'ACTIVE', invite_token = NULL, invite_expires_at = NULL
--  5. invite_expires_at 경과 시 토큰 만료 처리 (배치 또는 로그인 시점 검증)
-- ============================================================


-- ============================================================
-- CHECK 제약 추가 (팀장 코드리뷰 반영)
-- social_provider/social_provider_id, ad_type/google_ad_unit_id
-- 정합성 보장
--
-- (기존 ALTER TABLE users / ALTER TABLE advertisement 블록 뒤)
-- ============================================================

ALTER TABLE users
    ADD CONSTRAINT chk_user_social_consistency
    CHECK (
        (social_provider = 'NONE' AND social_provider_id IS NULL)
        OR
        (social_provider = 'GOOGLE' AND social_provider_id IS NOT NULL)
    );

ALTER TABLE advertisement
    ADD CONSTRAINT chk_ad_type_consistency
    CHECK (
        (ad_type = 'INTERNAL' AND google_ad_unit_id IS NULL)
        OR
        (ad_type = 'GOOGLE' AND google_ad_unit_id IS NOT NULL)
    );
