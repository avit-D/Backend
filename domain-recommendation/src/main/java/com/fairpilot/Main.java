package com.fairpilot;

import com.fairpilot.service.BoothVectorService;
import com.fairpilot.dto.BoothInfo;
import com.fairpilot.dto.RouteRecommendResponse;
import com.fairpilot.service.RecommendationService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public ApplicationRunner run(
            BoothVectorService boothVectorService,
            RecommendationService recommendationService
    ) {
        return args -> {
            Long exhibitionId = 1L;
            List<BoothInfo> booths = mockBooths();
            List<String> interests = List.of("AI", "헬스케어", "음식");

            // Step 1. 인덱싱
            System.out.println("=== 부스 인덱싱 시작 ===");
            boothVectorService.addAll(exhibitionId, booths);
            System.out.println("=== 부스 인덱싱 완료 ===");

            // Step 2. 동선 추천
            System.out.println("=== 동선 추천 시작 ===");
            RouteRecommendResponse response = recommendationService.recommend(exhibitionId, interests, booths);

            System.out.println("=== 추천 결과 ===");
            System.out.println("요약: " + response.summary());
            response.route().forEach(item ->
                    System.out.printf("%d. [%s] %s (X:%d, Y:%d) - 혼잡도: %s%n  이유: %s%n",
                            item.order(), item.boothId(), item.name(),
                            item.posX(), item.posY(), item.congestionLevel(),
                            item.reason())
            );
        };
    }

    private List<BoothInfo> mockBooths() {
        return List.of(
                new BoothInfo(1L, "AI 스타트업 존",     "최신 AI 기술 및 스타트업 데모",    "AI",      2, 3, "여유"),
                new BoothInfo(2L, "헬스케어 이노베이션", "디지털 헬스케어 솔루션 전시",      "헬스케어",  3, 3, "보통"),
                new BoothInfo(3L, "푸드테크 체험관",     "식품 기술 및 스마트 팜 체험",      "푸드테크",  3, 5, "혼잡"),
                new BoothInfo(4L, "핀테크 라운지",      "블록체인 및 디지털 금융 서비스",    "핀테크",    1, 2, "여유"),
                new BoothInfo(5L, "에듀테크 부스",      "AI 기반 교육 플랫폼 소개",         "에듀테크",  4, 2, "보통"),
                new BoothInfo(6L, "메타버스 존",        "VR/AR 기반 메타버스 체험",         "AI",       5, 4, "혼잡"),
                new BoothInfo(7L, "바이오 헬스 존",     "바이오 기술 및 신약 개발 전시",     "헬스케어",  2, 5, "여유"),
                new BoothInfo(8L, "스마트시티 부스",    "IoT 및 스마트시티 인프라 솔루션",   "IT인프라",  1, 4, "보통")
        );
    }
}