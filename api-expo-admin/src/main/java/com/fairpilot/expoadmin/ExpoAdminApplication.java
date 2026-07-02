package com.fairpilot.expoadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
@EnableJpaRepositories(basePackages = "com.fairpilot")
public class ExpoAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpoAdminApplication.class, args);
    }
}