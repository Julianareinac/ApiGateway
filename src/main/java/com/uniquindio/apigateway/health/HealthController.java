package com.uniquindio.apigateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final CustomHealthIndicator healthIndicator;

    public HealthController(CustomHealthIndicator healthIndicator) {
        this.healthIndicator = healthIndicator;
    }

    @GetMapping("/health")
    public Health healthCheck() {
        return healthIndicator.health();
    }

    @GetMapping("/health/ready")
    public Health readinessCheck() {
        return Health.up()
                .withDetail("from", healthIndicator.health().getDetails().get("uptime"))
                .withDetail("status", "READY")
                .build();
    }

    @GetMapping("/health/live")
    public Health livenessCheck() {
        return Health.up()
                .withDetail("from", healthIndicator.health().getDetails().get("uptime"))
                .withDetail("status", "ALIVE")
                .build();
    }
}
