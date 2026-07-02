package com.fairpilot.expoadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.reservation.domain.Reservation;
import com.fairpilot.reservation.domain.ReservationAttendee;
import com.fairpilot.reservation.dto.ReservationResponse;
import com.fairpilot.reservation.repository.ReservationAttendeeRepository;
import com.fairpilot.reservation.repository.ReservationRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 전시회 예약 현황 API (EXPO_ADMIN 전용, 기획안 §6.3 + §6.4 admin 뷰).
 *
 * GET  /api/exhibitions/{id}/reservations        — 예약 목록 (페이징)
 * GET  /api/exhibitions/{id}/reservations/export — 엑셀 다운로드
 */
@PreAuthorize("hasAnyRole('EXPO_ADMIN', 'ACCOUNTANT')")
@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/reservations")
@RequiredArgsConstructor
public class ExhibitionReservationController {

    private final ReservationRepository reservationRepository;
    private final ReservationAttendeeRepository attendeeRepository;

    /** 전시회 예약 목록 — EXPO_ADMIN 뷰 */
    @GetMapping
    public ApiResponse<Page<ReservationResponse>> listReservations(
            @PathVariable Long exhibitionId,
            Pageable pageable) {
        Page<ReservationResponse> page = reservationRepository
                .findByExhibitionId(exhibitionId, pageable)
                .map(r -> ReservationResponse.of(r, attendeeRepository.findByReservationId(r.getId())));
        return ApiResponse.ok(page);
    }

    /**
     * 예약 명단 엑셀 export.
     * 시트1: 예약 헤더 / 시트2: 참석자 상세
     */
    @GetMapping("/export")
    public void exportExcel(@PathVariable Long exhibitionId,
                            HttpServletResponse response) throws IOException {
        List<Reservation> reservations = reservationRepository.findByExhibitionId(exhibitionId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"reservations_" + exhibitionId + ".xlsx\"");

        try (Workbook wb = new XSSFWorkbook()) {
            writeReservationSheet(wb, reservations);
            writeAttendeeSheet(wb, reservations);
            wb.write(response.getOutputStream());
        }
    }

    // -------------------------------------------------------
    // 내부 헬퍼
    // -------------------------------------------------------

    private void writeReservationSheet(Workbook wb, List<Reservation> reservations) {
        Sheet sheet = wb.createSheet("예약 목록");
        CellStyle headerStyle = createHeaderStyle(wb);

        String[] headers = {"예약ID", "상태", "이동방식", "인원수", "예약소스"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Reservation r : reservations) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getId());
            row.createCell(1).setCellValue(r.getStatus().name());
            row.createCell(2).setCellValue(r.getMovementMode().name());
            row.createCell(3).setCellValue(r.getGroupSize());
            row.createCell(4).setCellValue(r.getReservationSource().name());
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void writeAttendeeSheet(Workbook wb, List<Reservation> reservations) {
        Sheet sheet = wb.createSheet("참석자 상세");
        CellStyle headerStyle = createHeaderStyle(wb);

        String[] headers = {"예약ID", "참석자ID", "이름", "전화번호", "이메일", "팀장여부", "체크인상태", "참석자상태"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Reservation r : reservations) {
            List<ReservationAttendee> attendees = attendeeRepository.findByReservationId(r.getId());
            for (ReservationAttendee a : attendees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(a.getId());
                row.createCell(2).setCellValue(a.getName());
                row.createCell(3).setCellValue(a.getPhone() != null ? a.getPhone() : "");
                row.createCell(4).setCellValue(a.getEmail() != null ? a.getEmail() : "");
                row.createCell(5).setCellValue(a.isGroupLeader() ? "Y" : "N");
                row.createCell(6).setCellValue(a.getCheckinStatus().name());
                row.createCell(7).setCellValue(a.getAttendeeStatus().name());
            }
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
