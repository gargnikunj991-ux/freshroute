package com.wildbugs.freshroute.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "dispatch_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String planType; // "FRESHROUTE_FEFO" or "BASELINE_FIFO"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProduceBatch batch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_id", nullable = false)
    private DestinationMarket destinationMarket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    private TransportRoute transportRoute;

    @NotNull
    @Column(nullable = false)
    private Double allocatedQuantityKg;

    @NotNull
    @Column(nullable = false)
    private Double baseMarketPricePerKg;

    @NotNull
    @Column(nullable = false)
    private Double discountPercentage;

    @NotNull
    @Column(nullable = false)
    private Double effectivePricePerKg;

    @NotNull
    @Column(nullable = false)
    private Double freightCostPerKg;

    @NotNull
    @Column(nullable = false)
    private Double handlingCostPerKg;

    @NotNull
    @Column(nullable = false)
    private Double procurementCostPerKg;

    @NotNull
    @Column(nullable = false)
    private Double netMarginPerKg;

    @NotNull
    @Column(nullable = false)
    private Double totalGrossRevenue;

    @NotNull
    @Column(nullable = false)
    private Double totalNetProfit;

    @NotNull
    @Column(nullable = false)
    private Double remainingShelfLifeOnArrivalHours;

    @Column(length = 1000)
    private String decisionRationale;

    @Builder.Default
    private Instant allocatedAt = Instant.now();
}
