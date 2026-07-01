package com.fairpilot.visitor.facade;

import com.fairpilot.dto.BoothInfo;
import com.fairpilot.dto.RouteRecommendResponse;
import com.fairpilot.exhibition.BoothService;
import com.fairpilot.service.RecommendationService;
import com.fairpilot.tracking.congestion.service.CongestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RouteFacade {

    private final BoothService boothService;
    private final RecommendationService recommendationService;
    private final CongestionService congestionService;

    public RouteRecommendResponse recommend(Long exhibitionId, List<String> interests) {
        List<BoothInfo> allBooths = boothService.findAll(exhibitionId).stream().map(booth -> {
            return new BoothInfo(
                    booth.getId(),
                    booth.getName(),
                    booth.getDescription(),
                    booth.getTags(),
                    booth.getPosX(),
                    booth.getPosY(),
                    "원활" //TODO("CongestionService 연동 필요")
            );
        }).toList();
        return recommendationService.recommend(exhibitionId, interests, allBooths);
    }
}
