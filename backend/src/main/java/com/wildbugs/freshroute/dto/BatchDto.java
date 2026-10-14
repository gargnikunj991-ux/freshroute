package com.wildbugs.freshroute.dto;

import com.wildbugs.freshroute.model.enums.BatchStatus;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.ProduceCategory;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDto {
    private Long id;
    private String batchCode;
    private String variety;
    private Commodity commodity;
    private ProduceCategory category;
    private Double initialQuantityKg;
    private Double remainingQuantityKg;
    private Double initialShelfLifeHours;
    private Double remainingShelfLifeHours;
    private Double procurementCostPerKg;
    private Double currentTemperatureCelsius;
    private BatchStatus status;
    private Instant harvestTimestamp;
    private String originHub;
    private Double urgencyIndex;
    private String riskLevel; // "LOW", "MEDIUM", "CRITICAL", "EXPIRED"
}
