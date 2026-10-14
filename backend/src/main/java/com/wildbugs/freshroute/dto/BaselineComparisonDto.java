package com.wildbugs.freshroute.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaselineComparisonDto {
    private OptimizationResultDto baseline;
    private OptimizationResultDto freshRoute;

    // Derived Improvement Metrics
    private Double spoilageReductionKg;
    private Double spoilageReductionPercent;
    private Double netProfitGainInr;
    private Double netProfitGainPercent;
    private Double demandFulfillmentGainPercent;
    private Integer feasibilityViolationsPrevented;
    private String executiveSummary;
}
