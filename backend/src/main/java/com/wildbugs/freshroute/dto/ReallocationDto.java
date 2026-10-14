package com.wildbugs.freshroute.dto;

import com.wildbugs.freshroute.model.enums.Commodity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReallocationDto {
    private String batchCode;
    private Commodity commodity;
    private Double quantityKg;
    private String previousDestination;
    private String newDestination;
    private String actionTaken; // "DIVERT_TO_PROCESSING", "APPLY_DISCOUNT_REROUTE", "CANCEL_DISPATCH"
    private Double discountAppliedPercent;
    private Double salvageRecoveryInr;
    private String explanation;
}
