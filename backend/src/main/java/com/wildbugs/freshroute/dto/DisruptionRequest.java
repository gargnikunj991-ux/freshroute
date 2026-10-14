package com.wildbugs.freshroute.dto;

import com.wildbugs.freshroute.model.enums.DisruptionType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisruptionRequest {
    @Builder.Default
    private DisruptionType disruptionType = DisruptionType.TRANSIT_DELAY;

    private Long routeId; // For transit delay
    @Builder.Default
    private Double delayHours = 6.0;

    private Long batchId; // For temperature spike
    @Builder.Default
    private Double temperatureSpikeCelsius = 5.0;
}
