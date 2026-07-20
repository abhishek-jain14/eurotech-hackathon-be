package com.qagenie.testbe.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Generates OpenAPI 3 documentation, served at /swagger-ui.html and /v3/api-docs.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "QAGenie Test Automation Platform API",
                version = "1.0.0",
                description = "APIs for application onboarding, environment configuration, test scenario " +
                        "management, test data, test flows, execution and reporting.",
                contact = @Contact(name = "QAGenie Platform Team", email = "platform-team@qagenie.io")
        ),
        servers = {
                @Server(url = "/", description = "Default")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
