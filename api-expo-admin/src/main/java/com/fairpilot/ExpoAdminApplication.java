package com.fairpilot;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
@MapperScan(value = "com.fairpilot.tracking.stats.repository", annotationClass = Mapper.class)
@EnableJpaRepositories(basePackages = "com.fairpilot")
@EntityScan(basePackages = "com.fairpilot")
public class ExpoAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpoAdminApplication.class, args);
    }
}