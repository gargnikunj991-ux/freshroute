package com.wildbugs.freshroute.service;

import com.wildbugs.freshroute.model.DestinationMarket;
import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.TransportRoute;
import com.wildbugs.freshroute.model.enums.BatchStatus;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.DestinationTier;
import com.wildbugs.freshroute.model.enums.ProduceCategory;
import com.wildbugs.freshroute.repository.DestinationMarketRepository;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import com.wildbugs.freshroute.repository.TransportRouteRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeederService {

    private final ProduceBatchRepository batchRepository;
    private final DestinationMarketRepository marketRepository;
    private final TransportRouteRepository routeRepository;
    private final com.wildbugs.freshroute.repository.DispatchAllocationRepository allocationRepository;

    @PostConstruct
    @Transactional
    public void seedInitialData() {
        if (marketRepository.count() > 0) {
            log.info("Database already seeded with reference dataset.");
            return;
        }

        log.info("Seeding Indian MoFPI Benchmark Corridors & Batch Inventory...");
        resetAndSeed();
    }

    @Transactional
    public void resetAndSeed() {
        allocationRepository.deleteAll();
        routeRepository.deleteAll();
        marketRepository.deleteAll();
        batchRepository.deleteAll();

        allocationRepository.flush();
        routeRepository.flush();
        marketRepository.flush();
        batchRepository.flush();

        // 1. Destination Markets
        DestinationMarket delhi = DestinationMarket.builder()
                .name("Delhi Azadpur APMC Mandi")
                .locationCity("New Delhi")
                .tier(DestinationTier.METRO_SUPERMARKET)
                .minShelfLifeBufferHours(20.0)
                .demandByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 8000.0,
                        Commodity.MANGO, 3000.0,
                        Commodity.BANANA, 4000.0,
                        Commodity.SPINACH, 1000.0,
                        Commodity.POTATO, 5000.0
                )))
                .priceByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 45.0,
                        Commodity.MANGO, 95.0,
                        Commodity.BANANA, 32.0,
                        Commodity.SPINACH, 28.0,
                        Commodity.POTATO, 22.0
                )))
                .build();

        DestinationMarket jaipur = DestinationMarket.builder()
                .name("Jaipur Modern Retail Hub")
                .locationCity("Jaipur")
                .tier(DestinationTier.METRO_SUPERMARKET)
                .minShelfLifeBufferHours(24.0)
                .demandByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 4500.0,
                        Commodity.MANGO, 2500.0,
                        Commodity.BANANA, 3500.0,
                        Commodity.SPINACH, 1200.0
                )))
                .priceByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 48.0,
                        Commodity.MANGO, 105.0,
                        Commodity.BANANA, 34.0,
                        Commodity.SPINACH, 30.0
                )))
                .build();

        DestinationMarket mumbai = DestinationMarket.builder()
                .name("Mumbai Vashi APMC Mandi")
                .locationCity("Navi Mumbai")
                .tier(DestinationTier.LOCAL_MANDI)
                .minShelfLifeBufferHours(14.0)
                .demandByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 6000.0,
                        Commodity.MANGO, 4000.0,
                        Commodity.BANANA, 5000.0,
                        Commodity.SPINACH, 2000.0
                )))
                .priceByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 42.0,
                        Commodity.MANGO, 90.0,
                        Commodity.BANANA, 30.0,
                        Commodity.SPINACH, 25.0
                )))
                .build();

        DestinationMarket ahmedabad = DestinationMarket.builder()
                .name("Ahmedabad Agro-Processing Cluster")
                .locationCity("Ahmedabad")
                .tier(DestinationTier.FOOD_PROCESSOR)
                .minShelfLifeBufferHours(6.0)
                .demandByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 8000.0,
                        Commodity.MANGO, 3500.0,
                        Commodity.BANANA, 3000.0
                )))
                .priceByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 32.0,
                        Commodity.MANGO, 65.0,
                        Commodity.BANANA, 22.0
                )))
                .build();

        DestinationMarket indore = DestinationMarket.builder()
                .name("Indore Regional Mandi")
                .locationCity("Indore")
                .tier(DestinationTier.REGIONAL_WHOLESALE)
                .minShelfLifeBufferHours(14.0)
                .demandByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 3500.0,
                        Commodity.BANANA, 2500.0,
                        Commodity.POTATO, 4000.0
                )))
                .priceByCommodity(new HashMap<>(Map.of(
                        Commodity.TOMATO, 38.0,
                        Commodity.BANANA, 28.0,
                        Commodity.POTATO, 20.0
                )))
                .build();

        DestinationMarket pune = DestinationMarket.builder()
                .name("Pune Quick-Commerce Dark Stores")
                .locationCity("Pune")
                .tier(DestinationTier.LOCAL_MANDI)
                .minShelfLifeBufferHours(10.0)
                .demandByCommodity(new HashMap<>(Map.of(
                        Commodity.SPINACH, 2000.0,
                        Commodity.TOMATO, 3000.0
                )))
                .priceByCommodity(new HashMap<>(Map.of(
                        Commodity.SPINACH, 32.0,
                        Commodity.TOMATO, 44.0
                )))
                .build();

        marketRepository.saveAll(List.of(delhi, jaipur, mumbai, ahmedabad, indore, pune));

        // 2. Transport Routes from Nashik Agro-Hub
        TransportRoute routeDelhi = TransportRoute.builder()
                .corridorName("Nashik-Delhi NH48 North Corridor")
                .originHub("Nashik Cold Logistics Hub")
                .destinationMarket(delhi)
                .standardTransitHours(24.0)
                .freightCostPerKg(2.80)
                .maxVehicleCapacityKg(20000.0)
                .activeDelayHours(0.0)
                .build();

        TransportRoute routeJaipur = TransportRoute.builder()
                .corridorName("Nashik-Jaipur Express Corridor")
                .originHub("Nashik Cold Logistics Hub")
                .destinationMarket(jaipur)
                .standardTransitHours(18.0)
                .freightCostPerKg(2.20)
                .maxVehicleCapacityKg(15000.0)
                .activeDelayHours(0.0)
                .build();

        TransportRoute routeMumbai = TransportRoute.builder()
                .corridorName("Nashik-Mumbai Samruddhi Expressway")
                .originHub("Nashik Cold Logistics Hub")
                .destinationMarket(mumbai)
                .standardTransitHours(5.0)
                .freightCostPerKg(0.90)
                .maxVehicleCapacityKg(25000.0)
                .activeDelayHours(0.0)
                .build();

        TransportRoute routeAhmedabad = TransportRoute.builder()
                .corridorName("Nashik-Ahmedabad Industrial Highway")
                .originHub("Nashik Cold Logistics Hub")
                .destinationMarket(ahmedabad)
                .standardTransitHours(10.0)
                .freightCostPerKg(1.40)
                .maxVehicleCapacityKg(18000.0)
                .activeDelayHours(0.0)
                .build();

        TransportRoute routeIndore = TransportRoute.builder()
                .corridorName("Nashik-Indore NH52 Central Corridor")
                .originHub("Nashik Cold Logistics Hub")
                .destinationMarket(indore)
                .standardTransitHours(12.0)
                .freightCostPerKg(1.60)
                .maxVehicleCapacityKg(15000.0)
                .activeDelayHours(0.0)
                .build();

        TransportRoute routePune = TransportRoute.builder()
                .corridorName("Nashik-Pune Regional Highway")
                .originHub("Nashik Cold Logistics Hub")
                .destinationMarket(pune)
                .standardTransitHours(4.0)
                .freightCostPerKg(0.80)
                .maxVehicleCapacityKg(12000.0)
                .activeDelayHours(0.0)
                .build();

        routeRepository.saveAll(List.of(routeDelhi, routeJaipur, routeMumbai, routeAhmedabad, routeIndore, routePune));

        // 3. Realistic 32,000 kg Produce Batches (MoFPI Reference Set)
        Instant now = Instant.now();

        List<ProduceBatch> batches = List.of(
                ProduceBatch.builder()
                        .batchCode("BATCH-TOM-01")
                        .variety("Hybrid Red")
                        .commodity(Commodity.TOMATO)
                        .category(ProduceCategory.VEGETABLE)
                        .initialQuantityKg(4000.0)
                        .remainingQuantityKg(4000.0)
                        .initialShelfLifeHours(72.0)
                        .remainingShelfLifeHours(22.0) // Urgent FEFO candidate
                        .procurementCostPerKg(22.0)
                        .currentTemperatureCelsius(12.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(50, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-TOM-02")
                        .variety("Desi Country")
                        .commodity(Commodity.TOMATO)
                        .category(ProduceCategory.VEGETABLE)
                        .initialQuantityKg(3500.0)
                        .remainingQuantityKg(3500.0)
                        .initialShelfLifeHours(72.0)
                        .remainingShelfLifeHours(48.0)
                        .procurementCostPerKg(20.0)
                        .currentTemperatureCelsius(12.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(24, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-TOM-03")
                        .variety("Roma Plum")
                        .commodity(Commodity.TOMATO)
                        .category(ProduceCategory.VEGETABLE)
                        .initialQuantityKg(4500.0)
                        .remainingQuantityKg(4500.0)
                        .initialShelfLifeHours(96.0)
                        .remainingShelfLifeHours(72.0)
                        .procurementCostPerKg(24.0)
                        .currentTemperatureCelsius(11.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(24, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-MNG-01")
                        .variety("Ratnagiri Alphonso")
                        .commodity(Commodity.MANGO)
                        .category(ProduceCategory.FRUIT)
                        .initialQuantityKg(2500.0)
                        .remainingQuantityKg(2500.0)
                        .initialShelfLifeHours(60.0)
                        .remainingShelfLifeHours(36.0)
                        .procurementCostPerKg(65.0)
                        .currentTemperatureCelsius(13.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(24, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-MNG-02")
                        .variety("Gir Kesar")
                        .commodity(Commodity.MANGO)
                        .category(ProduceCategory.FRUIT)
                        .initialQuantityKg(3000.0)
                        .remainingQuantityKg(3000.0)
                        .initialShelfLifeHours(84.0)
                        .remainingShelfLifeHours(60.0)
                        .procurementCostPerKg(55.0)
                        .currentTemperatureCelsius(13.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(24, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-BAN-01")
                        .variety("Jalgaon Robusta")
                        .commodity(Commodity.BANANA)
                        .category(ProduceCategory.FRUIT)
                        .initialQuantityKg(3500.0)
                        .remainingQuantityKg(3500.0)
                        .initialShelfLifeHours(48.0)
                        .remainingShelfLifeHours(26.0)
                        .procurementCostPerKg(18.0)
                        .currentTemperatureCelsius(14.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(22, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-BAN-02")
                        .variety("Grand Naine G9")
                        .commodity(Commodity.BANANA)
                        .category(ProduceCategory.FRUIT)
                        .initialQuantityKg(4000.0)
                        .remainingQuantityKg(4000.0)
                        .initialShelfLifeHours(72.0)
                        .remainingShelfLifeHours(55.0)
                        .procurementCostPerKg(19.0)
                        .currentTemperatureCelsius(14.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(17, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-SPN-01")
                        .variety("Organic Green Palak")
                        .commodity(Commodity.SPINACH)
                        .category(ProduceCategory.LEAFY_GREEN)
                        .initialQuantityKg(1500.0)
                        .remainingQuantityKg(1500.0)
                        .initialShelfLifeHours(24.0)
                        .remainingShelfLifeHours(14.0) // Extremely fragile leafy green
                        .procurementCostPerKg(15.0)
                        .currentTemperatureCelsius(8.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(10, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-SPN-02")
                        .variety("Organic Green Palak")
                        .commodity(Commodity.SPINACH)
                        .category(ProduceCategory.LEAFY_GREEN)
                        .initialQuantityKg(1500.0)
                        .remainingQuantityKg(1500.0)
                        .initialShelfLifeHours(36.0)
                        .remainingShelfLifeHours(28.0)
                        .procurementCostPerKg(15.0)
                        .currentTemperatureCelsius(8.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(8, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build(),

                ProduceBatch.builder()
                        .batchCode("BATCH-POT-01")
                        .variety("Kufri Chipsona")
                        .commodity(Commodity.POTATO)
                        .category(ProduceCategory.TUBER)
                        .initialQuantityKg(4000.0)
                        .remainingQuantityKg(4000.0)
                        .initialShelfLifeHours(360.0)
                        .remainingShelfLifeHours(180.0)
                        .procurementCostPerKg(12.0)
                        .currentTemperatureCelsius(15.0)
                        .status(BatchStatus.AVAILABLE)
                        .harvestTimestamp(now.minus(180, ChronoUnit.HOURS))
                        .originHub("Nashik Cold Logistics Hub")
                        .build()
        );

        batchRepository.saveAll(batches);
        log.info("Seeding completed successfully: 6 markets, 6 transport corridors, 10 batches (32,000 kg).");
    }
}
