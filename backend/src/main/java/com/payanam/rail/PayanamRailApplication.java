package com.payanam.rail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point of Payanam Rail. Run with {@code ./mvnw spring-boot:run} and open http://localhost:8080. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class PayanamRailApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayanamRailApplication.class, args);
    }
}
