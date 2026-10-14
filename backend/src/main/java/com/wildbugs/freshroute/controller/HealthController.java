package com.wildbugs.freshroute.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "Health & Keep-Alive", description = "Endpoints for uptime monitoring and Render cold-start prevention")
public class HealthController {

    @GetMapping({"/api/v1/health", "/api/health"})
    @Operation(summary = "Health Check & Cold-Start Keep-Alive",
               description = "Returns system status and timestamp. Targeted by internal 12-minute keep-alive scheduler to prevent Render free-tier sleep.")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "freshroute-backend",
                "timestamp", Instant.now().toString(),
                "jvmVersion", System.getProperty("java.version")
        ));
    }
}
