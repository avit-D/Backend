package com.fairpilot.visitor;

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
public class VisitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisitorApplication.class, args);
    }
}