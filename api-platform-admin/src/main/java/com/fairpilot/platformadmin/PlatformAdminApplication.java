package com.fairpilot.platformadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.fairpilot")
public class PlatformAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformAdminApplication.class, args);
    }

}
