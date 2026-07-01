package com.fairpilot.visitor.web;

import com.fairpilot.dto.RouteRecommendResponse;
import com.fairpilot.visitor.facade.RouteFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final RouteFacade routeFacade;

    @GetMapping("/recommend")
    public ResponseEntity<RouteRecommendResponse> recommend(
            @RequestParam Long exhibitionId,
            @RequestParam List<String> interests
    ) {
        return ResponseEntity.ok(routeFacade.recommend(exhibitionId, interests));
    }
}
