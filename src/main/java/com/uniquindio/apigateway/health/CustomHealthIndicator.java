package com.uniquindio.apigateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    private final Instant startTime;

    public CustomHealthIndicator() {
        this.startTime = Instant.now();  // Guardar el tiempo de inicio
    }

    @Override
    public Health health() {
        Instant now = Instant.now();
        Duration uptime = Duration.between(startTime, now);

        return Health.up()
                .withDetail("version", "1.0.0")
                .withDetail("uptime", uptime.toString())
                .withDetail("checks", getHealthChecks())
                .build();
    }

    private Object getHealthChecks() {
        return new Object[] {
                new HealthCheck("Readiness check", "READY", startTime),
                new HealthCheck("Liveness check", "ALIVE", startTime)
        };
    }

    static class HealthCheck {
        public String name;
        public String status;
        public Instant from;

        public HealthCheck(String name, String status, Instant from) {
            this.name = name;
            this.status = status;
            this.from = from;
        }
    }
}

