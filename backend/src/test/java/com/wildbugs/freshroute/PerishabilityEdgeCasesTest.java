package com.wildbugs.freshroute;

import com.wildbugs.freshroute.dto.DisruptionRequest;
import com.wildbugs.freshroute.dto.DisruptionResultDto;
import com.wildbugs.freshroute.dto.OptimizationResultDto;
import com.wildbugs.freshroute.dto.OptimizeAllocationRequest;
import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.enums.BatchStatus;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.DisruptionType;
import com.wildbugs.freshroute.model.enums.ProduceCategory;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import com.wildbugs.freshroute.service.DataSeederService;
import com.wildbugs.freshroute.service.DisruptionEngine;
import com.wildbugs.freshroute.service.FefoOptimizationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PerishabilityEdgeCasesTest {

    @Autowired
    private FefoOptimizationEngine fefoEngine;

    @Autowired
    private DisruptionEngine disruptionEngine;

    @Autowired
    private ProduceBatchRepository batchRepository;

    @Autowired
    private DataSeederService dataSeederService;

    @BeforeEach
    void setUp() {
        dataSeederService.resetAndSeed();
    }

    @Test
    @DisplayName("EDGE CASE: Batch with transit > RSL must NEVER be dispatched on long-haul routes")
    void testImpossibleLongHaulTripBlocked() {
        // Create fragile batch with only 8 hours shelf life
        ProduceBatch fragileBatch = ProduceBatch.builder()
                .batchCode("EDGE-FRAGILE-01")
                .variety("Organic Cherry")
                .commodity(Commodity.TOMATO)
                .category(ProduceCategory.VEGETABLE)
                .initialQuantityKg(1000.0)
                .remainingQuantityKg(1000.0)
                .initialShelfLifeHours(24.0)
                .remainingShelfLifeHours(8.0) // Delhi takes 24h, Jaipur takes 18h
                .procurementCostPerKg(30.0)
                .currentTemperatureCelsius(12.0)
                .status(BatchStatus.AVAILABLE)
                .harvestTimestamp(Instant.now())
                .originHub("Nashik Hub")
                .build();

        batchRepository.saveAndFlush(fragileBatch);

        OptimizationResultDto result = fefoEngine.runFefoOptimization(OptimizeAllocationRequest.builder().build());

        // Verify that this fragile batch was NEVER allocated to Delhi or Jaipur
        result.getAllocations().stream()
                .filter(a -> a.getBatchCode().equals("EDGE-FRAGILE-01"))
                .forEach(a -> {
                    assertNotEquals("Delhi Azadpur APMC Mandi", a.getDestinationName(),
                            "Cannot allocate 8h shelf-life batch to Delhi (24h transit)");
                    assertNotEquals("Jaipur Modern Retail Hub", a.getDestinationName(),
                            "Cannot allocate 8h shelf-life batch to Jaipur (18h transit)");
                    assertTrue(a.getRemainingShelfLifeOnArrivalHours() >= 0,
                            "Shelf life on arrival must never be negative");
                });
    }

    @Test
    @DisplayName("EDGE CASE: Batch that cannot reach ANY destination before buffer spoils in warehouse")
    void testUnreachableBatchSpoilsInWarehouseWithoutWastingFreight() {
        // Shelf life is only 2 hours. Minimum transit is Pune (4h).
        ProduceBatch doomedBatch = ProduceBatch.builder()
                .batchCode("EDGE-DOOMED-01")
                .variety("Tender Spinach")
                .commodity(Commodity.SPINACH)
                .category(ProduceCategory.LEAFY_GREEN)
                .initialQuantityKg(800.0)
                .remainingQuantityKg(800.0)
                .initialShelfLifeHours(12.0)
                .remainingShelfLifeHours(2.0)
                .procurementCostPerKg(15.0)
                .currentTemperatureCelsius(8.0)
                .status(BatchStatus.AVAILABLE)
                .harvestTimestamp(Instant.now())
                .originHub("Nashik Hub")
                .build();

        batchRepository.saveAndFlush(doomedBatch);

        OptimizationResultDto result = fefoEngine.runFefoOptimization(OptimizeAllocationRequest.builder().build());

        // Must NOT dispatch 0 kg on road to save freight fees
        boolean wasDispatched = result.getAllocations().stream()
                .anyMatch(a -> a.getBatchCode().equals("EDGE-DOOMED-01"));

        assertFalse(wasDispatched, "Doomed batch with 2h shelf-life must not be dispatched on road");

        ProduceBatch updated = batchRepository.findByBatchCode("EDGE-DOOMED-01").orElseThrow();
        assertEquals(BatchStatus.SPOILED, updated.getStatus());
        assertEquals(800.0, updated.getRemainingQuantityKg(), 0.001);
    }

    @Test
    @DisplayName("EDGE CASE: Extreme heatwave (+20°C) evaluates Arrhenius decay without crash or NaN")
    void testExtremeHeatwaveArrheniusStability() {
        DisruptionResultDto result = disruptionEngine.simulateDisruption(
                DisruptionRequest.builder()
                        .disruptionType(DisruptionType.HEATWAVE_TEMPERATURE_SPIKE)
                        .temperatureSpikeCelsius(20.0) // Extreme thermal shock
                        .build()
        );

        assertNotNull(result);
        assertTrue(result.getAffectedBatchesCount() >= 1, "Extreme heatwave should affect fragile batches");
        assertFalse(Double.isNaN(result.getActualSpoilagePreventedKg()), "Prevented kg must not be NaN");
        assertFalse(Double.isInfinite(result.getActualSpoilagePreventedKg()), "Prevented kg must not be Infinite");

        // Verify database batches have non-negative shelf-life
        batchRepository.findAll().forEach(b -> {
            assertTrue(b.getRemainingShelfLifeHours() >= 0.0,
                    "Batch " + b.getBatchCode() + " has negative RSL: " + b.getRemainingShelfLifeHours());
        });
    }
}
