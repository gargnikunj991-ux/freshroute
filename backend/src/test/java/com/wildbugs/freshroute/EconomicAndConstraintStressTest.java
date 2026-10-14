package com.wildbugs.freshroute;

import com.wildbugs.freshroute.dto.OptimizationResultDto;
import com.wildbugs.freshroute.dto.OptimizeAllocationRequest;
import com.wildbugs.freshroute.model.DestinationMarket;
import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.TransportRoute;
import com.wildbugs.freshroute.model.enums.BatchStatus;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.ProduceCategory;
import com.wildbugs.freshroute.repository.DestinationMarketRepository;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import com.wildbugs.freshroute.repository.TransportRouteRepository;
import com.wildbugs.freshroute.service.DataSeederService;
import com.wildbugs.freshroute.service.FefoOptimizationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EconomicAndConstraintStressTest {

    @Autowired
    private FefoOptimizationEngine fefoEngine;

    @Autowired
    private ProduceBatchRepository batchRepository;

    @Autowired
    private DestinationMarketRepository marketRepository;

    @Autowired
    private TransportRouteRepository routeRepository;

    @Autowired
    private DataSeederService dataSeederService;

    @BeforeEach
    void setUp() {
        dataSeederService.resetAndSeed();
    }

    @Test
    @DisplayName("CONSTRAINT STRESS: Strict Transport Vehicle Capacity Enforced")
    void testTransportVehicleCapacityNeverExceeded() {
        // Find Pune route which has max vehicle capacity of 12,000 kg
        TransportRoute puneRoute = routeRepository.findAll().stream()
                .filter(r -> r.getDestinationMarket().getLocationCity().equalsIgnoreCase("Pune"))
                .findFirst()
                .orElseThrow();

        double vehicleLimit = puneRoute.getMaxVehicleCapacityKg();

        OptimizationResultDto result = fefoEngine.runFefoOptimization(OptimizeAllocationRequest.builder().build());

        // Sum all dispatches to Pune
        double totalDispatchedToPune = result.getAllocations().stream()
                .filter(a -> a.getDestinationCity().equalsIgnoreCase("Pune"))
                .mapToDouble(a -> a.getAllocatedQuantityKg())
                .sum();

        assertTrue(totalDispatchedToPune <= vehicleLimit,
                "Total quantity dispatched to Pune (" + totalDispatchedToPune + "kg) exceeded truck payload capacity (" + vehicleLimit + "kg)");
    }

    @Test
    @DisplayName("CONSTRAINT STRESS: Market Demand Capping (Never Over-supply)")
    void testMarketDemandNeverOverAllocated() {
        OptimizationResultDto result = fefoEngine.runFefoOptimization(OptimizeAllocationRequest.builder().build());

        List<DestinationMarket> markets = marketRepository.findAll();

        for (DestinationMarket market : markets) {
            for (Commodity comm : Commodity.values()) {
                double maxDemand = market.getDemandFor(comm);

                double totalAllocated = result.getAllocations().stream()
                        .filter(a -> a.getDestinationName().equals(market.getName()) && a.getCommodity() == comm)
                        .mapToDouble(a -> a.getAllocatedQuantityKg())
                        .sum();

                assertTrue(totalAllocated <= maxDemand + 0.001,
                        "Market " + market.getName() + " received " + totalAllocated + "kg of " + comm + " exceeding demand " + maxDemand);
            }
        }
    }

    @Test
    @DisplayName("ECONOMIC STRESS: Zero Demand Scenario gracefully handles inventory holding")
    void testZeroDemandDoesNotCrashAndRecordsHolding() {
        // Zero out demand across all markets
        List<DestinationMarket> markets = marketRepository.findAll();
        for (DestinationMarket m : markets) {
            m.setDemandByCommodity(new HashMap<>());
            marketRepository.save(m);
        }

        OptimizationResultDto result = fefoEngine.runFefoOptimization(OptimizeAllocationRequest.builder().build());

        assertNotNull(result);
        assertEquals(0, result.getTotalDispatches(), "No dispatches when demand is 0");
        assertEquals(0.0, result.getTotalAllocatedKg(), 0.001);
        assertEquals(0.0, result.getGrossSalesRevenue(), 0.001);
    }
}
