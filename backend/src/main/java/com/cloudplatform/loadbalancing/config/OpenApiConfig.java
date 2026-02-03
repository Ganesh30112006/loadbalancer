package com.cloudplatform.loadbalancing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Load Balancing Control Plane API")
                        .version("1.0.0")
                        .description("""
                                Infrastructure Control Plane API for managing AWS resources.
                                
                                This API provides:
                                - AWS Account onboarding with STS AssumeRole
                                - Application Blueprint management
                                - SLO Policy configuration
                                - Service lifecycle management
                                - Real-time metrics and observability
                                - Control loop monitoring
                                
                                **Authentication**: All endpoints require a valid X-User-Id header.
                                """)
                        .contact(new Contact()
                                .name("Platform Team")
                                .email("platform@example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://example.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Development server"),
                        new Server()
                                .url("https://api.loadbalancing.example.com")
                                .description("Production server")
                ));
    }
}
