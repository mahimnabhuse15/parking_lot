package com.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutoParkApplication {
    public static void main(String[] args) {
        // Auto-initialize MySQL Database
        DatabaseManager.loadConfig();
        DatabaseManager.initializeDatabase(
                DatabaseManager.getDbHost(),
                DatabaseManager.getDbPort(),
                DatabaseManager.getDbName(),
                DatabaseManager.getDbUser(),
                DatabaseManager.getDbPass(),
                20 // default capacity
        );

        // Launch Spring Boot application (Embedded Tomcat Web Server)
        SpringApplication.run(AutoParkApplication.class, args);
    }
}
