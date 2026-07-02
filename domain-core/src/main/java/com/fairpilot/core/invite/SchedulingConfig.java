package com.fairpilot.core.invite;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 활성화
 * domain-core 에 두어 모든 api 모듈에서 스케줄러가 동작하도록 함
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}