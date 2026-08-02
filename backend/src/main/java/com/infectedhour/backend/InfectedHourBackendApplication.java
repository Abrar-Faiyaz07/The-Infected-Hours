package com.infectedhour.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single Spring Boot service (Backend Schema §1). Runs on the host laptop
 * for demos; binds 0.0.0.0:8080 (see application.yml) so the client
 * laptop can reach it too — both laptops call the same instance.
 */
@SpringBootApplication
public class InfectedHourBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(InfectedHourBackendApplication.class, args);
    }
}
