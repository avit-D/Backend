package com.fairpilot.tracking.stats.repository;

import com.fairpilot.tracking.stats.dto.StatDtos.DwellAvgResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 상위 구간 체류 평균 — MyBatis Native SQL.
 * 평균의 함정 방지: SUM(sum_dwell_sec) / SUM(dwell_count) 공식.
 * avg_dwell_sec 단순 AVG()는 사용하지 않는다.
 */
@Mapper
public interface StatMapper {

    /**
     * 일별 상위 구간 체류 평균.
     * stat_date 기준으로 해당 날의 모든 시간대 버킷을 합산.
     * @param exhibitionId 박람회 테넌트
     * @param statDate     집계 날짜 (KST)
     */
    DwellAvgResult dailyDwellAvg(
            @Param("exhibitionId") Long exhibitionId,
            @Param("statDate") LocalDate statDate);

    /**
     * 박람회 전체 기간 체류 평균.
     */
    DwellAvgResult totalDwellAvg(@Param("exhibitionId") Long exhibitionId);
}
