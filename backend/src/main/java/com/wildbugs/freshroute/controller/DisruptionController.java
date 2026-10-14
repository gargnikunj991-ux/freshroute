package com.wildbugs.freshroute.controller;

import com.wildbugs.freshroute.dto.DisruptionRequest;
import com.wildbugs.freshroute.dto.DisruptionResultDto;
import com.wildbugs.freshroute.service.DisruptionEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/disruption")
@RequiredArgsConstructor
@Tag(name = "Disruption & What-If Studio", description = "Endpoints for simulating supply-chain bottlenecks, traffic delays, and cold chain failures")
public class DisruptionController {

    private final DisruptionEngine disruptionEngine;

    @PostMapping("/simulate")
    @Operation(summary = "Simulate Supply-Chain Disruption Scenario",
               description = "Applies real-world complications (e.g., +6h transit delay or +5°C temperature spike) and returns dynamic re-routing decisions and salvage cash recovery.")
    public ResponseEntity<DisruptionResultDto> simulate(@RequestBody(required = false) DisruptionRequest request) {
        if (request == null) {
            request = DisruptionRequest.builder().build();
        }
        DisruptionResultDto result = disruptionEngine.simulateDisruption(request);
        return ResponseEntity.ok(result);
    }
}
