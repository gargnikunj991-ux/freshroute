package com.wildbugs.freshroute.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOverviewDto {
    private Double totalInventoryKg;
    private Integer activeBatchesCount;
    private Double averageRslHours;
    private Integer totalDestinationsCount;
    private Integer highRiskBatchesCount;
    private BaselineComparisonDto comparison;
    private List<BatchDto> activeBatches;
}
