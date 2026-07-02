package com.fairpilot.exhibitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
@EnableJpaRepositories(basePackages = "com.fairpilot")
public class ExhibitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExhibitorApplication.class, args);
    }
}