package com.wildbugs.freshroute.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisruptionResultDto {
    private String scenario;
    private String disruptionDescription;
    private Integer affectedBatchesCount;
    private Double potentialSpoilageLossKg;
    private Double actualSpoilagePreventedKg;
    private Double salvageCashRecoveredInr;
    private List<ReallocationDto> reallocations;
}
