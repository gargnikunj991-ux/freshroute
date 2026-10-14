package com.wildbugs.freshroute.service;

import com.wildbugs.freshroute.dto.AllocationItemDto;
import com.wildbugs.freshroute.dto.OptimizationResultDto;
import com.wildbugs.freshroute.dto.OptimizeAllocationRequest;
import com.wildbugs.freshroute.model.DestinationMarket;
import com.wildbugs.freshroute.model.DispatchAllocation;
import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.TransportRoute;
import com.wildbugs.freshroute.model.enums.BatchStatus;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.DestinationTier;
import com.wildbugs.freshroute.repository.DestinationMarketRepository;
import com.wildbugs.freshroute.repository.DispatchAllocationRepository;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import com.wildbugs.freshroute.repository.TransportRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FefoOptimizationEngine {

    private final ProduceBatchRepository batchRepository;
    private final DestinationMarketRepository marketRepository;
    private final TransportRouteRepository routeRepository;
    private final DispatchAllocationRepository allocationRepository;
    private final DynamicDiscountEngine discountEngine;

    private static final double HANDLING_COST_PER_KG = 0.50; // INR
    private static final double DISPOSAL_PENALTY_PER_KG = 3.00; // INR

    private static class CandidateAllocation {
        DestinationMarket market;
        TransportRoute route;
        double discountPercent;
        double effectivePrice;
        double freightCost;
        double netMarginPerKg;
        double remainingShelfLifeOnArrival;
        boolean isSalvageProcessingTier;
        String rationale;
    }

    @Transactional
    public OptimizationResultDto runFefoOptimization(OptimizeAllocationRequest request) {
        log.info("Starting FreshRoute FEFO + Margin Optimization run...");

        // Clear previous FreshRoute allocations
        allocationRepository.deleteByPlanType("FRESHROUTE_FEFO");

        // 1. Fetch batches in FEFO order (lowest RSL first)
        List<ProduceBatch> batches = batchRepository.findAllActiveFefoOrder();
        List<DestinationMarket> markets = marketRepository.findAll();
        List<TransportRoute> routes = routeRepository.findAll();

        Map<Long, TransportRoute> routeByMarketId = new HashMap<>();
        for (TransportRoute route : routes) {
            routeByMarketId.put(route.getDestinationMarket().getId(), route);
        }

        // Remaining demands & route capacities
        Map<Long, Map<Commodity, Double>> remainingDemand = new HashMap<>();
        double totalDemandRequested = 0.0;
        for (DestinationMarket m : markets) {
            Map<Commodity, Double> demMap = new HashMap<>(m.getDemandByCommodity());
            remainingDemand.put(m.getId(), demMap);
            for (Double d : demMap.values()) {
                totalDemandRequested += d;
            }
        }

        Map<Long, Double> remainingRouteCapacity = new HashMap<>();
        for (TransportRoute r : routes) {
            remainingRouteCapacity.put(r.getId(), r.getMaxVehicleCapacityKg());
        }

        List<DispatchAllocation> savedAllocations = new ArrayList<>();
        List<AllocationItemDto> allocationItemDtos = new ArrayList<>();

        double totalAllocatedKg = 0.0;
        double totalSpoiledKg = 0.0;
        double totalGrossRevenue = 0.0;
        double totalFreightCost = 0.0;
        double totalDiscountsGranted = 0.0;
        double totalHandlingCost = 0.0;
        double totalProcurementCost = 0.0;
        double totalSpoilageLoss = 0.0;
        Set<String> activeCorridors = new HashSet<>();

        for (ProduceBatch batch : batches) {
            // Optional concurrency verification: lock batch row
            ProduceBatch lockedBatch = batchRepository.findByIdWithPessimisticLock(batch.getId()).orElse(batch);
            double batchRemQty = lockedBatch.getRemainingQuantityKg();
            Commodity comm = lockedBatch.getCommodity();
            double rsl = lockedBatch.getRemainingShelfLifeHours();

            while (batchRemQty > 0.01) {
                // Find all feasible candidate allocations for this batch
                List<CandidateAllocation> candidates = new ArrayList<>();

                for (DestinationMarket market : markets) {
                    Map<Commodity, Double> marketDemands = remainingDemand.get(market.getId());
                    double dem = marketDemands.getOrDefault(comm, 0.0);
                    if (dem <= 0.01) continue;

                    TransportRoute route = routeByMarketId.get(market.getId());
                    if (route == null) continue;

                    double routeCap = remainingRouteCapacity.getOrDefault(route.getId(), 0.0);
                    if (routeCap <= 0.01) continue;

                    double transitHours = route.getEffectiveTransitHours();
                    double arrivalRsl = rsl - transitHours;

                    // Feasibility Constraint Check
                    if (arrivalRsl < market.getMinShelfLifeBufferHours()) {
                        continue; // Strictly invalid for this market
                    }

                    // Dynamic Discount Calculation
                    double discountPct = 0.0;
                    if (Boolean.TRUE.equals(request.getEnableDynamicDiscounting())) {
                        discountPct = discountEngine.calculateDiscountPercentage(
                                transitHours,
                                market.getMinShelfLifeBufferHours(),
                                rsl,
                                true
                        );
                    }

                    double basePrice = market.getPriceFor(comm);
                    double effectivePrice = basePrice * (1.0 - discountPct);
                    double freight = route.getFreightCostPerKg();
                    double procurement = lockedBatch.getProcurementCostPerKg();
                    double netMargin = effectivePrice - freight - HANDLING_COST_PER_KG - procurement;

                    CandidateAllocation candidate = new CandidateAllocation();
                    candidate.market = market;
                    candidate.route = route;
                    candidate.discountPercent = discountPct;
                    candidate.effectivePrice = effectivePrice;
                    candidate.freightCost = freight;
                    candidate.netMarginPerKg = netMargin;
                    candidate.remainingShelfLifeOnArrival = arrivalRsl;
                    candidate.isSalvageProcessingTier = (market.getTier() == DestinationTier.FOOD_PROCESSOR);

                    String rationale = String.format(
                            "FEFO Rank Match: RSL %.1fh allows transit (%.1fh) with %.1fh arrival buffer. Net margin: ₹%.2f/kg at %s%s",
                            rsl, transitHours, arrivalRsl, netMargin, market.getName(),
                            discountPct > 0 ? String.format(" with %.0f%% dynamic discount applied", discountPct * 100) : ""
                    );
                    candidate.rationale = rationale;

                    candidates.add(candidate);
                }

                if (candidates.isEmpty()) {
                    // No feasible retail or standard market found.
                    // Check if we can salvage to FOOD_PROCESSOR at liquidation pricing if allowed
                    break;
                }

                // Rank candidates by Net Margin descending
                candidates.sort((c1, c2) -> Double.compare(c2.netMarginPerKg, c1.netMarginPerKg));

                // Pick best feasible candidate
                CandidateAllocation best = candidates.get(0);
                Map<Commodity, Double> marketDemands = remainingDemand.get(best.market.getId());
                double dem = marketDemands.getOrDefault(comm, 0.0);
                double routeCap = remainingRouteCapacity.get(best.route.getId());

                double allocQty = Math.min(batchRemQty, Math.min(dem, routeCap));

                double grossRev = allocQty * best.market.getPriceFor(comm);
                double effectiveRev = allocQty * best.effectivePrice;
                double discountValue = grossRev - effectiveRev;
                double freight = allocQty * best.freightCost;
                double handling = allocQty * HANDLING_COST_PER_KG;
                double procurement = allocQty * lockedBatch.getProcurementCostPerKg();
                double netProfit = effectiveRev - freight - handling - procurement;

                DispatchAllocation alloc = DispatchAllocation.builder()
                        .planType("FRESHROUTE_FEFO")
                        .batch(lockedBatch)
                        .destinationMarket(best.market)
                        .transportRoute(best.route)
                        .allocatedQuantityKg(allocQty)
                        .baseMarketPricePerKg(best.market.getPriceFor(comm))
                        .discountPercentage(best.discountPercent * 100.0)
                        .effectivePricePerKg(best.effectivePrice)
                        .freightCostPerKg(best.freightCost)
                        .handlingCostPerKg(HANDLING_COST_PER_KG)
                        .procurementCostPerKg(lockedBatch.getProcurementCostPerKg())
                        .netMarginPerKg(best.netMarginPerKg)
                        .totalGrossRevenue(grossRev)
                        .totalNetProfit(netProfit)
                        .remainingShelfLifeOnArrivalHours(best.remainingShelfLifeOnArrival)
                        .decisionRationale(best.rationale)
                        .build();

                savedAllocations.add(alloc);

                allocationItemDtos.add(AllocationItemDto.builder()
                        .id((long) (allocationItemDtos.size() + 1))
                        .planType("FRESHROUTE_FEFO")
                        .batchCode(lockedBatch.getBatchCode())
                        .commodity(comm)
                        .destinationName(best.market.getName())
                        .destinationCity(best.market.getLocationCity())
                        .destinationTier(best.market.getTier().name())
                        .allocatedQuantityKg(allocQty)
                        .basePricePerKg(best.market.getPriceFor(comm))
                        .discountPercentage(best.discountPercent * 100.0)
                        .effectivePricePerKg(best.effectivePrice)
                        .freightCostPerKg(best.freightCost)
                        .netMarginPerKg(best.netMarginPerKg)
                        .totalNetProfit(netProfit)
                        .transitTimeHours(best.route.getEffectiveTransitHours())
                        .remainingShelfLifeOnArrivalHours(best.remainingShelfLifeOnArrival)
                        .decisionRationale(best.rationale)
                        .build());

                // Deduct
                batchRemQty -= allocQty;
                marketDemands.put(comm, dem - allocQty);
                remainingRouteCapacity.put(best.route.getId(), routeCap - allocQty);

                totalAllocatedKg += allocQty;
                totalGrossRevenue += effectiveRev;
                totalDiscountsGranted += discountValue;
                totalFreightCost += freight;
                totalHandlingCost += handling;
                totalProcurementCost += procurement;
                activeCorridors.add(best.route.getCorridorName());
            }

            // If remaining quantity cannot be dispatched feasibly, record as spoilage
            if (batchRemQty > 0.01) {
                totalSpoiledKg += batchRemQty;
                double loss = batchRemQty * (lockedBatch.getProcurementCostPerKg() + DISPOSAL_PENALTY_PER_KG);
                totalSpoilageLoss += loss;
                totalProcurementCost += batchRemQty * lockedBatch.getProcurementCostPerKg();
                lockedBatch.setStatus(BatchStatus.SPOILED);
            } else {
                lockedBatch.setStatus(BatchStatus.FULLY_ALLOCATED);
            }

            lockedBatch.setRemainingQuantityKg(batchRemQty);
            batchRepository.save(lockedBatch);
        }

        allocationRepository.saveAll(savedAllocations);

        double totalBatchTonnage = batches.stream().mapToDouble(ProduceBatch::getInitialQuantityKg).sum();
        double spoilagePercent = totalBatchTonnage > 0 ? (totalSpoiledKg / totalBatchTonnage) * 100.0 : 0.0;
        double netProfit = totalGrossRevenue - totalFreightCost - totalHandlingCost - totalProcurementCost - totalSpoilageLoss;
        double fulfillmentPercent = totalDemandRequested > 0 ? (totalAllocatedKg / totalDemandRequested) * 100.0 : 0.0;

        return OptimizationResultDto.builder()
                .status("OPTIMIZATION_SUCCESS")
                .planType("FRESHROUTE_FEFO")
                .totalDispatches(savedAllocations.size())
                .totalAllocatedKg(Math.round(totalAllocatedKg * 100.0) / 100.0)
                .totalSpoiledKg(Math.round(totalSpoiledKg * 100.0) / 100.0)
                .spoilagePercentage(Math.round(spoilagePercent * 10.0) / 10.0)
                .grossSalesRevenue(Math.round((totalGrossRevenue + totalDiscountsGranted) * 100.0) / 100.0)
                .totalFreightCost(Math.round(totalFreightCost * 100.0) / 100.0)
                .totalDiscountsGranted(Math.round(totalDiscountsGranted * 100.0) / 100.0)
                .totalSpoilageLoss(Math.round(totalSpoilageLoss * 100.0) / 100.0)
                .netEconomicProfit(Math.round(netProfit * 100.0) / 100.0)
                .overallDemandFulfillmentPercent(Math.round(fulfillmentPercent * 10.0) / 10.0)
                .zeroSpoilageCorridors(activeCorridors.size())
                .allocations(allocationItemDtos)
                .build();
    }
}
