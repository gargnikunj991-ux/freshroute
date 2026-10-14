package com.wildbugs.freshroute.model;

import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.DestinationTier;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "destination_markets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @Column(nullable = false)
    private String locationCity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestinationTier tier;

    @NotNull
    @Column(nullable = false)
    private Double minShelfLifeBufferHours;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "market_commodity_demands", joinColumns = @JoinColumn(name = "market_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "commodity")
    @Column(name = "demand_kg")
    @Builder.Default
    private Map<Commodity, Double> demandByCommodity = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "market_commodity_prices", joinColumns = @JoinColumn(name = "market_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "commodity")
    @Column(name = "price_per_kg")
    @Builder.Default
    private Map<Commodity, Double> priceByCommodity = new HashMap<>();

    public Double getDemandFor(Commodity commodity) {
        return demandByCommodity.getOrDefault(commodity, 0.0);
    }

    public Double getPriceFor(Commodity commodity) {
        return priceByCommodity.getOrDefault(commodity, 0.0);
    }
}
