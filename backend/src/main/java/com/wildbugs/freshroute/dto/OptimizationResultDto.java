package com.wildbugs.freshroute.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationResultDto {
    private String status;
    private String planType;
    private Integer totalDispatches;
    private Double totalAllocatedKg;
    private Double totalSpoiledKg;
    private Double spoilagePercentage;
    private Double grossSalesRevenue;
    private Double totalFreightCost;
    private Double totalDiscountsGranted;
    private Double totalSpoilageLoss;
    private Double netEconomicProfit;
    private Double overallDemandFulfillmentPercent;
    private Integer zeroSpoilageCorridors;
    private List<AllocationItemDto> allocations;
}
