package com.wildbugs.freshroute.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizeAllocationRequest {
    @Builder.Default
    private Double bufferHoursSafetyMargin = 12.0;

    @Builder.Default
    private Boolean enableDynamicDiscounting = true;

    @Builder.Default
    private Boolean allowFoodProcessorSalvage = true;
}
