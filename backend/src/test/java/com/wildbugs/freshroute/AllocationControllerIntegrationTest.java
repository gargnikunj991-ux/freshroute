package com.wildbugs.freshroute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildbugs.freshroute.dto.DisruptionRequest;
import com.wildbugs.freshroute.dto.OptimizeAllocationRequest;
import com.wildbugs.freshroute.model.enums.DisruptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AllocationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.wildbugs.freshroute.service.DataSeederService dataSeederService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        dataSeederService.resetAndSeed();
    }

    @Test
    @DisplayName("API TEST: POST /api/v1/allocation/optimize returns 200 with valid allocation payload")
    void testOptimizeEndpoint() throws Exception {
        OptimizeAllocationRequest req = OptimizeAllocationRequest.builder()
                .enableDynamicDiscounting(true)
                .bufferHoursSafetyMargin(12.0)
                .build();

        mockMvc.perform(post("/api/v1/allocation/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPTIMIZATION_SUCCESS"))
                .andExpect(jsonPath("$.planType").value("FRESHROUTE_FEFO"))
                .andExpect(jsonPath("$.totalAllocatedKg").isNumber())
                .andExpect(jsonPath("$.allocations").isArray());
    }

    @Test
    @DisplayName("API TEST: GET /api/v1/allocation/compare-baseline returns side-by-side benchmark metrics")
    void testCompareBaselineEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/allocation/compare-baseline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline.planType").value("BASELINE_FIFO"))
                .andExpect(jsonPath("$.freshRoute.planType").value("FRESHROUTE_FEFO"))
                .andExpect(jsonPath("$.spoilageReductionPercent").isNumber())
                .andExpect(jsonPath("$.netProfitGainInr").isNumber())
                .andExpect(jsonPath("$.executiveSummary").isString());
    }

    @Test
    @DisplayName("API TEST: GET /api/v1/dashboard/overview returns aggregated inventory KPIs")
    void testDashboardOverviewEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInventoryKg").isNumber())
                .andExpect(jsonPath("$.activeBatchesCount").isNumber())
                .andExpect(jsonPath("$.activeBatches").isArray());
    }

    @Test
    @DisplayName("API TEST: GET /api/v1/dashboard/batches returns batches sorted by FEFO urgency")
    void testDashboardBatchesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].urgencyIndex").isNumber())
                .andExpect(jsonPath("$[0].riskLevel").isString());
    }

    @Test
    @DisplayName("API TEST: POST /api/v1/disruption/simulate triggers transit delay rerouting")
    void testDisruptionSimulateEndpoint() throws Exception {
        DisruptionRequest req = DisruptionRequest.builder()
                .disruptionType(DisruptionType.TRANSIT_DELAY)
                .delayHours(6.0)
                .build();

        mockMvc.perform(post("/api/v1/disruption/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("TRANSIT_DELAY (+6.0h Highway Roadblock / Congestion)"))
                .andExpect(jsonPath("$.reallocations").isArray());
    }

    @Test
    @DisplayName("API TEST: POST /api/v1/dashboard/batches creates custom batch dynamically for live demos")
    void testCreateCustomBatchEndpoint() throws Exception {
        com.wildbugs.freshroute.dto.CreateBatchRequest req = com.wildbugs.freshroute.dto.CreateBatchRequest.builder()
                .batchCode("LIVE-DEMO-01")
                .variety("Mahabaleshwar Winter")
                .commodity(com.wildbugs.freshroute.model.enums.Commodity.TOMATO)
                .category(com.wildbugs.freshroute.model.enums.ProduceCategory.VEGETABLE)
                .quantityKg(2500.0)
                .remainingShelfLifeHours(32.0)
                .procurementCostPerKg(25.0)
                .currentTemperatureCelsius(10.0)
                .originHub("Nashik Test Hub")
                .build();

        mockMvc.perform(post("/api/v1/dashboard/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchCode").value("LIVE-DEMO-01"))
                .andExpect(jsonPath("$.initialQuantityKg").value(2500.0))
                .andExpect(jsonPath("$.urgencyIndex").isNumber());
    }

    @Test
    @DisplayName("API TEST: GET /api/v1/dashboard/markets and /routes returns network topology")
    void testGetMarketsAndRoutesEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/markets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").isString());

        mockMvc.perform(get("/api/v1/dashboard/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].corridorName").isString());
    }
}
