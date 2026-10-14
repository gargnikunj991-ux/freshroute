package com.wildbugs.freshroute.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "transport_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String corridorName;

    @NotNull
    @Column(nullable = false)
    private String originHub;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_market_id", nullable = false)
    private DestinationMarket destinationMarket;

    @NotNull
    @Column(nullable = false)
    private Double standardTransitHours;

    @NotNull
    @Column(nullable = false)
    private Double freightCostPerKg;

    @NotNull
    @Column(nullable = false)
    private Double maxVehicleCapacityKg;

    @Builder.Default
    @Column(nullable = false)
    private Double activeDelayHours = 0.0;

    public Double getEffectiveTransitHours() {
        return standardTransitHours + (activeDelayHours != null ? activeDelayHours : 0.0);
    }
}
