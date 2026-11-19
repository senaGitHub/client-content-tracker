package io.github.senagithub.tracker.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness check for the hosting platform. It deliberately does not touch the database,
 * so a sleeping Neon instance can't make a deploy look unhealthy.
 */
@RestController
@Tag(name = "Health")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Returns UP when the app is running")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
