package com.solnotfound.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  /** Defines the public API metadata and JWT bearer authentication used by Swagger UI. */
  @Bean
  OpenAPI applicationOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("404 Sol Not Found API")
                .description(
                    "API para organizar actividades y monitorear sus condiciones climaticas")
                .version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
