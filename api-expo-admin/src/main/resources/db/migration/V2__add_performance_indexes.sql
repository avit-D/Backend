-- ============================================================
-- V2: 성능 인덱스 추가
-- V1에 인덱스가 전혀 없어 조회 쿼리 전체가 풀스캔 발생.
-- 체크인·통계·예약 현황 API 기준 핵심 조회 경로 커버.
-- ============================================================

-- reservation: 전시회별 예약 목록 (ExhibitionReservationController, PlatformAdminStatsController)
CREATE INDEX idx_reservation_exhibition
    ON reservation (exhibition_id);

-- reservation: 사용자별 예약 목록 (GET /api/reservations/me)
CREATE INDEX idx_reservation_user
    ON reservation (user_id);

-- reservation_attendee: 예약 단위 참석자 조회 (가장 빈번한 JOIN)
CREATE INDEX idx_attendee_reservation
    ON reservation_attendee (reservation_id);

-- reservation_attendee: 전시회 + 체크인 검색 (CheckinService.lookupAttendees)
CREATE INDEX idx_attendee_exhibition
    ON reservation_attendee (exhibition_id);

-- name_tag: 전시회별 재고 조회 + 상태 필터 (NametagService.getStock)
CREATE INDEX idx_nametag_exhibition_status
    ON name_tag (exhibition_id, status);

-- name_tag: 참석자별 ISSUED 태그 조회 (체크인 바인딩·재발급 경로)
CREATE INDEX idx_nametag_attendee_status
    ON name_tag (attendee_id, status);

-- checkin_log: 전시회별 체크인 이력 (관리자 조회)
CREATE INDEX idx_checkin_log_exhibition
    ON checkin_log (exhibition_id);

-- checkin_log: 참석자 체크인 이력 (CheckinLogRepository.findByAttendeeId*)
CREATE INDEX idx_checkin_log_attendee
    ON checkin_log (attendee_id);

-- visit_log: 전시회 + 시각 범위 조회 (혼잡도·히트맵·집계 배치)
CREATE INDEX idx_visit_log_exhibition_scanned
    ON visit_log (exhibition_id, scanned_at);

-- visit_log: 동선 흐름 전이행렬 (StatsQueryService.flow — attendee 시간순)
CREATE INDEX idx_visit_log_exhibition_attendee_scanned
    ON visit_log (exhibition_id, attendee_id, scanned_at);

-- stat_visit_point: 전시회별 집계 통계 조회
CREATE INDEX idx_stat_visit_point_exhibition
    ON stat_visit_point (exhibition_id, stat_date);

-- stat_congestion_hourly: 전시회별 혼잡도 히트맵
CREATE INDEX idx_stat_congestion_exhibition
    ON stat_congestion_hourly (exhibition_id, stat_date);
