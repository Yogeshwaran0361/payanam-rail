package com.payanam.rail.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html. The API is versioned under /api/v1 and split into groups
 * (select one in the top-right drop-down). Click "Authorize" and paste a token from /auth/signin.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI payanamOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Payanam Rail API").version("v1")
                        .description("Railway Reservation and Platform Allocation System for Payanam Junction"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return group("all", "/api/v1/**");
    }

    @Bean
    public GroupedOpenApi authApi() {
        return group("auth", "/api/v1/auth/**");
    }

    @Bean
    public GroupedOpenApi trainsApi() {
        return group("trains", "/api/v1/trains/**");
    }

    @Bean
    public GroupedOpenApi bookingsApi() {
        return group("bookings", "/api/v1/bookings/**", "/api/v1/notifications/**");
    }

    @Bean
    public GroupedOpenApi pnrApi() {
        return group("pnr", "/api/v1/pnr/**");
    }

    @Bean
    public GroupedOpenApi platformsApi() {
        return group("platforms", "/api/v1/platforms/**");
    }

    @Bean
    public GroupedOpenApi operationsApi() {
        return group("operations", "/api/v1/operations/**");
    }

    @Bean
    public GroupedOpenApi simulationApi() {
        return group("simulation", "/api/v1/simulation/**");
    }

    @Bean
    public GroupedOpenApi auditApi() {
        return group("audit", "/api/v1/audit/**");
    }

    @Bean
    public GroupedOpenApi testsApi() {
        return group("tests", "/api/v1/tests/**");
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return group("admin", "/api/v1/admin/**");
    }

    private static GroupedOpenApi group(String name, String... paths) {
        return GroupedOpenApi.builder().group(name).pathsToMatch(paths).build();
    }
}
