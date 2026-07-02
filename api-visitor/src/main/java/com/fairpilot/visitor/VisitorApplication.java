package com.fairpilot.visitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
@EnableJpaRepositories(basePackages = "com.fairpilot")
public class VisitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisitorApplication.class, args);
    }
}