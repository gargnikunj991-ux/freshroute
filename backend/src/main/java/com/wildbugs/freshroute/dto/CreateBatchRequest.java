package com.wildbugs.freshroute.dto;

import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.ProduceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBatchRequest {

    @NotBlank(message = "Batch code is required")
    private String batchCode;

    private String variety;

    @NotNull(message = "Commodity is required")
    private Commodity commodity;

    @NotNull(message = "Produce category is required")
    private ProduceCategory category;

    @NotNull(message = "Quantity (kg) is required")
    @Positive(message = "Quantity must be greater than zero")
    private Double quantityKg;

    @NotNull(message = "Remaining shelf life (hours) is required")
    @Positive(message = "Shelf life must be positive")
    private Double remainingShelfLifeHours;

    @NotNull(message = "Procurement cost per kg is required")
    @Positive(message = "Procurement cost must be positive")
    private Double procurementCostPerKg;

    @Builder.Default
    private Double currentTemperatureCelsius = 12.0;

    @Builder.Default
    private String originHub = "Nashik Cold Logistics Hub";
}
