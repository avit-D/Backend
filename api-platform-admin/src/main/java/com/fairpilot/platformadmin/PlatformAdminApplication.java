package com.fairpilot.platformadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
@EnableJpaRepositories(basePackages = "com.fairpilot")
public class PlatformAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformAdminApplication.class, args);
    }
}