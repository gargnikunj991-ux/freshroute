package com.wildbugs.freshroute.controller;

import com.wildbugs.freshroute.dto.BaselineComparisonDto;
import com.wildbugs.freshroute.dto.BatchDto;
import com.wildbugs.freshroute.dto.DashboardOverviewDto;
import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.repository.DestinationMarketRepository;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Inventory Feed", description = "Endpoints for executive metrics and real-time perishable inventory")
public class DashboardController {

    private final ProduceBatchRepository batchRepository;
    private final DestinationMarketRepository marketRepository;
    private final AllocationController allocationController;

    @GetMapping("/overview")
    @Operation(summary = "Get Executive Dashboard Overview",
               description = "Returns aggregate inventory statistics, risk counts, and side-by-side benchmark comparison.")
    public ResponseEntity<DashboardOverviewDto> getOverview() {
        List<ProduceBatch> batches = batchRepository.findAll();
        List<BatchDto> batchDtos = batches.stream().map(this::mapToDto).collect(Collectors.toList());

        double totalInventoryKg = batches.stream().mapToDouble(ProduceBatch::getRemainingQuantityKg).sum();
        double avgRsl = batches.stream().mapToDouble(ProduceBatch::getRemainingShelfLifeHours).average().orElse(0.0);
        int highRiskCount = (int) batches.stream().filter(b -> b.getRemainingShelfLifeHours() <= 24.0).count();

        BaselineComparisonDto comparison = allocationController.compareWithBaseline().getBody();

        DashboardOverviewDto overview = DashboardOverviewDto.builder()
                .totalInventoryKg(Math.round(totalInventoryKg * 100.0) / 100.0)
                .activeBatchesCount(batches.size())
                .averageRslHours(Math.round(avgRsl * 10.0) / 10.0)
                .totalDestinationsCount((int) marketRepository.count())
                .highRiskBatchesCount(highRiskCount)
                .comparison(comparison)
                .activeBatches(batchDtos)
                .build();

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/batches")
    @Operation(summary = "Get Active Perishable Batches in FEFO Order",
               description = "Returns all inventory batches sorted by remaining shelf life with computed urgency indices and risk ratings.")
    public ResponseEntity<List<BatchDto>> getBatches() {
        List<ProduceBatch> batches = batchRepository.findAllActiveFefoOrder();
        List<BatchDto> dtos = batches.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private BatchDto mapToDto(ProduceBatch b) {
        String risk;
        if (b.getRemainingShelfLifeHours() <= 0.0) {
            risk = "EXPIRED";
        } else if (b.getRemainingShelfLifeHours() <= 16.0) {
            risk = "CRITICAL";
        } else if (b.getRemainingShelfLifeHours() <= 30.0) {
            risk = "MEDIUM";
        } else {
            risk = "LOW";
        }

        return BatchDto.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .variety(b.getVariety())
                .commodity(b.getCommodity())
                .category(b.getCategory())
                .initialQuantityKg(b.getInitialQuantityKg())
                .remainingQuantityKg(b.getRemainingQuantityKg())
                .initialShelfLifeHours(b.getInitialShelfLifeHours())
                .remainingShelfLifeHours(b.getRemainingShelfLifeHours())
                .procurementCostPerKg(b.getProcurementCostPerKg())
                .currentTemperatureCelsius(b.getCurrentTemperatureCelsius())
                .status(b.getStatus())
                .harvestTimestamp(b.getHarvestTimestamp())
                .originHub(b.getOriginHub())
                .urgencyIndex(Math.round(b.getUrgencyIndex() * 1000.0) / 1000.0)
                .riskLevel(risk)
                .build();
    }
}
