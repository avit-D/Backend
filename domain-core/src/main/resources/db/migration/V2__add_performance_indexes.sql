-- V2: 성능 인덱스 추가
CREATE INDEX idx_reservation_exhibition ON reservation (exhibition_id);
CREATE INDEX idx_reservation_user ON reservation (user_id);
CREATE INDEX idx_attendee_reservation ON reservation_attendee (reservation_id);
CREATE INDEX idx_attendee_exhibition ON reservation_attendee (exhibition_id);
CREATE INDEX idx_nametag_exhibition_status ON name_tag (exhibition_id, status);
CREATE INDEX idx_nametag_attendee_status ON name_tag (attendee_id, status);
CREATE INDEX idx_checkin_log_exhibition ON checkin_log (exhibition_id);
CREATE INDEX idx_checkin_log_attendee ON checkin_log (attendee_id);
CREATE INDEX idx_visit_log_exhibition_scanned ON visit_log (exhibition_id, scanned_at);
CREATE INDEX idx_visit_log_exhibition_attendee_scanned ON visit_log (exhibition_id, attendee_id, scanned_at);
CREATE INDEX idx_stat_visit_point_exhibition ON stat_visit_point (exhibition_id, stat_date);
CREATE INDEX idx_stat_congestion_exhibition ON stat_congestion_hourly (exhibition_id, stat_date);