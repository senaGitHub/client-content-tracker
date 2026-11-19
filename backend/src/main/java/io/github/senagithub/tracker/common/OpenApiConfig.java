package io.github.senagithub.tracker.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/** Swagger UI: /swagger-ui.html — OpenAPI JSON: /v3/api-docs */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Harbour API",
        version = "v1",
        description = "Client & content tracker for account managers."))
public class OpenApiConfig {
}
