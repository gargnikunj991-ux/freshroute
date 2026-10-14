package com.wildbugs.freshroute;

import com.wildbugs.freshroute.dto.BaselineComparisonDto;
import com.wildbugs.freshroute.dto.DisruptionRequest;
import com.wildbugs.freshroute.dto.DisruptionResultDto;
import com.wildbugs.freshroute.dto.OptimizationResultDto;
import com.wildbugs.freshroute.dto.OptimizeAllocationRequest;
import com.wildbugs.freshroute.model.enums.DisruptionType;
import com.wildbugs.freshroute.service.BaselineFifoEngine;
import com.wildbugs.freshroute.service.DataSeederService;
import com.wildbugs.freshroute.service.DisruptionEngine;
import com.wildbugs.freshroute.service.FefoOptimizationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FefoOptimizationEngineTest {

    @Autowired
    private FefoOptimizationEngine fefoEngine;

    @Autowired
    private BaselineFifoEngine baselineEngine;

    @Autowired
    private DisruptionEngine disruptionEngine;

    @Autowired
    private DataSeederService dataSeederService;

    @BeforeEach
    void setUp() {
        dataSeederService.resetAndSeed();
    }

    @Test
    @DisplayName("Verify FreshRoute achieves >30% spoilage reduction compared to Naive FIFO")
    void testSpoilageReductionOverBaseline() {
        OptimizationResultDto baseline = baselineEngine.runBaselineFifoSimulation();
        OptimizationResultDto freshRoute = fefoEngine.runFefoOptimization(OptimizeAllocationRequest.builder().build());

        assertNotNull(baseline);
        assertNotNull(freshRoute);

        // Baseline typically experiences ~18% spoilage (approx 5,800 kg)
        // FreshRoute should have significantly less spoilage (<= 2,000 kg or <= 7%)
        assertTrue(freshRoute.getTotalSpoiledKg() < baseline.getTotalSpoiledKg(),
                "FreshRoute must have strictly less spoilage than Baseline FIFO");

        double spoilageReductionPercent = ((baseline.getTotalSpoiledKg() - freshRoute.getTotalSpoiledKg()) / baseline.getTotalSpoiledKg()) * 100.0;
        assertTrue(spoilageReductionPercent >= 30.0,
                "FreshRoute must achieve at least 30% spoilage reduction. Achieved: " + spoilageReductionPercent + "%");

        // FreshRoute Net Economic Profit should be strictly greater than baseline
        assertTrue(freshRoute.getNetEconomicProfit() > baseline.getNetEconomicProfit(),
                "FreshRoute net economic profit must exceed baseline profit");
    }

    @Test
    @DisplayName("Verify Allocation Feasibility: Zero shelf-life buffer violations in FreshRoute")
    void testAllocationFeasibilityConstraints() {
        OptimizationResultDto freshRoute = fefoEngine.runFefoOptimization(OptimizeAllocationRequest.builder().build());

        assertFalse(freshRoute.getAllocations().isEmpty(), "Allocations should not be empty");

        freshRoute.getAllocations().forEach(alloc -> {
            assertTrue(alloc.getRemainingShelfLifeOnArrivalHours() >= 0,
                    "Batch " + alloc.getBatchCode() + " arrived with negative shelf life: " + alloc.getRemainingShelfLifeOnArrivalHours());
            assertTrue(alloc.getAllocatedQuantityKg() > 0, "Allocated quantity must be positive");
            assertNotNull(alloc.getDecisionRationale(), "Allocation must have explainable decision rationale");
        });
    }

    @Test
    @DisplayName("Verify Disruption Engine: Transit delay triggers salvage rerouting")
    void testDisruptionTransitDelay() {
        DisruptionResultDto result = disruptionEngine.simulateDisruption(
                DisruptionRequest.builder()
                        .disruptionType(DisruptionType.TRANSIT_DELAY)
                        .delayHours(6.0)
                        .build()
        );

        assertNotNull(result);
        assertEquals("TRANSIT_DELAY (+6.0h Highway Roadblock / Congestion)", result.getScenario());
        assertTrue(result.getActualSpoilagePreventedKg() > 0, "Disruption response must prevent spoilage");
        assertTrue(result.getSalvageCashRecoveredInr() > 0, "Disruption response must recover salvage cash");
        assertFalse(result.getReallocations().isEmpty(), "Must contain reallocation records");
    }
}
