package com.echel.planner.backend.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight liveness endpoint on the main HTTP port (8080).
 *
 * <p>Distinct from {@code /actuator/health}, which lives on the management
 * port (9090 under the prod profile) and aggregates database, connection pool,
 * disk, and other downstream checks. That endpoint is the right one for
 * internal observability and deep diagnostics.
 *
 * <p>This endpoint is the right one for load-balancer health checks, smoke
 * tests, and any external "is the process up?" probe. It deliberately does
 * not touch downstream dependencies — a brief DB blip should not take the
 * whole service out of the load balancer.
 *
 * <p>Permitted unauthenticated by {@code SecurityConfig}.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
