package com.wildbugs.freshroute.dto;

import com.wildbugs.freshroute.model.enums.Commodity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationItemDto {
    private Long id;
    private String planType;
    private String batchCode;
    private Commodity commodity;
    private String destinationName;
    private String destinationCity;
    private String destinationTier;
    private Double allocatedQuantityKg;
    private Double basePricePerKg;
    private Double discountPercentage;
    private Double effectivePricePerKg;
    private Double freightCostPerKg;
    private Double netMarginPerKg;
    private Double totalNetProfit;
    private Double transitTimeHours;
    private Double remainingShelfLifeOnArrivalHours;
    private String decisionRationale;
}
