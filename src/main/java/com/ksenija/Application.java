package com.ksenija;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the application.
 * <p>
 * Bootstraps the Spring Boot application and starts the embedded Tomcat server.
 *
 * @author Ksenija
 * @version 1.0
 */

@SpringBootApplication
public class Application {
    /**
     * Launches the Spring Boot application.
     *
     * @param args command-line arguments passed at startup (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}