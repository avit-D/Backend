package com.fairpilot.exhibitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
public class ExhibitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExhibitorApplication.class, args);
    }

}
