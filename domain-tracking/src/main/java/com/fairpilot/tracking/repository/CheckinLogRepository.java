package com.fairpilot.tracking.repository;

import com.fairpilot.tracking.domain.CheckinLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckinLogRepository extends JpaRepository<CheckinLog, Long> {

    List<CheckinLog> findByAttendeeIdOrderByCheckedInAtDesc(Long attendeeId);

    List<CheckinLog> findByReservationIdOrderByCheckedInAtDesc(Long reservationId);
}
