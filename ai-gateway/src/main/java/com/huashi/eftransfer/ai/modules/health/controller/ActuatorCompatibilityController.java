package com.huashi.eftransfer.ai.modules.health.controller;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActuatorCompatibilityController {

    private final HealthEndpoint healthEndpoint;

    public ActuatorCompatibilityController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/actuator/health")
    public HealthComponent health() {
        return healthEndpoint.health();
    }
}
