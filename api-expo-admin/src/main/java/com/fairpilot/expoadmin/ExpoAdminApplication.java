package com.fairpilot.expoadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
public class ExpoAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpoAdminApplication.class, args);
    }

}
