package com.fairpilot.expoadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
@MapperScan("com.fairpilot.tracking.stats.repository")
public class ExpoAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpoAdminApplication.class, args);
    }
}