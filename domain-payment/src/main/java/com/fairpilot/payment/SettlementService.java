package com.fairpilot.payment;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.032");

    /** 정산 생성 */
    @Transactional
    public Settlement create(Long exhibitionId, LocalDate periodStart, LocalDate periodEnd) {
        var from = periodStart.atStartOfDay();
        var to   = periodEnd.atTime(LocalTime.MAX);

        // 온라인 매출 합산 (기간 필터) — SUM이 null이면 ZERO
        BigDecimal onlineAmount = Optional.ofNullable(
                paymentRepository.sumOnlineAmount(
                        exhibitionId, PaymentStatus.PAID, "ONSITE", from, to))
                .orElse(BigDecimal.ZERO);

        // ONSITE 현장 매출 합산 (기간 필터) — SUM이 null이면 ZERO
        BigDecimal onsiteAmount = Optional.ofNullable(
                paymentRepository.sumOnsiteAmount(
                        exhibitionId, PaymentStatus.PAID, "ONSITE", from, to))
                .orElse(BigDecimal.ZERO);

        Settlement settlement = Settlement.builder()
                .exhibitionId(exhibitionId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .onlineAmount(onlineAmount)
                .onsiteAmount(onsiteAmount)
                .feeAmount(onlineAmount.multiply(FEE_RATE))
                .adRevenue(BigDecimal.ZERO)
                .build();

        return settlementRepository.save(settlement);
    }

    /** 정산 목록 조회 */
    @Transactional(readOnly = true)
    public List<Settlement> findAll(Long exhibitionId) {
        return settlementRepository.findAllByExhibitionId(exhibitionId);
    }

    /** 엑셀 다운로드 */
    public byte[] exportExcel(Long exhibitionId) {
        List<Settlement> list = settlementRepository.findAllByExhibitionId(exhibitionId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("정산내역");

            // 헤더
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "기간시작", "기간종료", "온라인매출",
                    "현장매출", "총매출", "수수료", "광고수익", "순지급액", "상태"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // 데이터
            int rowNum = 1;
            for (Settlement s : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getId());
                row.createCell(1).setCellValue(s.getPeriodStart().toString());
                row.createCell(2).setCellValue(s.getPeriodEnd().toString());
                row.createCell(3).setCellValue(s.getOnlineAmount().doubleValue());
                row.createCell(4).setCellValue(s.getOnsiteAmount().doubleValue());
                row.createCell(5).setCellValue(s.getGrossAmount().doubleValue());
                row.createCell(6).setCellValue(s.getFeeAmount().doubleValue());
                row.createCell(7).setCellValue(s.getAdRevenue().doubleValue());
                row.createCell(8).setCellValue(s.getNetPayout().doubleValue());
                row.createCell(9).setCellValue(s.getStatus().name());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UPSTREAM_ERROR, "엑셀 생성 실패");
        }
    }
}