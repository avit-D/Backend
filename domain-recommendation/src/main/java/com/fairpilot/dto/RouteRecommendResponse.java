package com.fairpilot.dto;

import java.util.List;

public record RouteRecommendResponse(
        List<RouteItem> route,
        String summary
) {}
