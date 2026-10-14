package com.wildbugs.freshroute.service;

import com.wildbugs.freshroute.dto.AllocationItemDto;
import com.wildbugs.freshroute.dto.OptimizationResultDto;
import com.wildbugs.freshroute.model.DestinationMarket;
import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.TransportRoute;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.repository.DestinationMarketRepository;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import com.wildbugs.freshroute.repository.TransportRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaselineFifoEngine {

    private final ProduceBatchRepository batchRepository;
    private final DestinationMarketRepository marketRepository;
    private final TransportRouteRepository routeRepository;

    @org.springframework.beans.factory.annotation.Value("${freshroute.economic.handling-cost-per-kg:0.50}")
    private double handlingCostPerKg;

    @org.springframework.beans.factory.annotation.Value("${freshroute.economic.disposal-penalty-per-kg:3.00}")
    private double disposalPenaltyPerKg;

    public OptimizationResultDto runBaselineFifoSimulation() {
        List<ProduceBatch> batches = batchRepository.findAllActiveFifoOrder();
        List<DestinationMarket> markets = marketRepository.findAll();
        List<TransportRoute> routes = routeRepository.findAll();

        Map<Long, TransportRoute> routeByMarketId = new HashMap<>();
        for (TransportRoute route : routes) {
            routeByMarketId.put(route.getDestinationMarket().getId(), route);
        }

        // Deep copy demands
        Map<Long, Map<Commodity, Double>> remainingDemand = new HashMap<>();
        double totalDemandRequested = 0.0;
        for (DestinationMarket m : markets) {
            Map<Commodity, Double> demMap = new HashMap<>(m.getDemandByCommodity());
            remainingDemand.put(m.getId(), demMap);
            for (Double d : demMap.values()) {
                totalDemandRequested += d;
            }
        }

        double totalAllocatedKg = 0.0;
        double totalSpoiledKg = 0.0;
        double totalGrossRevenue = 0.0;
        double totalFreightCost = 0.0;
        double totalSpoilageLoss = 0.0;
        double totalHandlingCost = 0.0;
        double totalProcurementCost = 0.0;
        int feasibilityViolations = 0;

        List<AllocationItemDto> allocationList = new ArrayList<>();

        for (ProduceBatch batch : batches) {
            double batchRemQty = batch.getRemainingQuantityKg();
            Commodity comm = batch.getCommodity();

            // Naive FIFO: iterates markets in fixed database order
            for (DestinationMarket market : markets) {
                if (batchRemQty <= 0) break;

                Map<Commodity, Double> marketDemands = remainingDemand.get(market.getId());
                double dem = marketDemands.getOrDefault(comm, 0.0);
                if (dem <= 0) continue;

                TransportRoute route = routeByMarketId.get(market.getId());
                if (route == null) continue;

                double allocQty = Math.min(batchRemQty, dem);
                double transitHours = route.getEffectiveTransitHours();
                double arrivalRsl = batch.getRemainingShelfLifeHours() - transitHours;

                // Check naive arrival: if arrivalRsl < minShelfLifeBufferHours, it spoils or rejected!
                boolean isRejected = (arrivalRsl < market.getMinShelfLifeBufferHours());

                double basePrice = market.getPriceFor(comm);
                double freight = route.getFreightCostPerKg();
                double procurement = batch.getProcurementCostPerKg();
                double handling = handlingCostPerKg;

                if (isRejected) {
                    feasibilityViolations++;
                    totalSpoiledKg += allocQty;
                    // Spoilage economic loss = procurement basis + freight + disposal penalty
                    double loss = allocQty * (procurement + freight + disposalPenaltyPerKg);
                    totalSpoilageLoss += loss;
                    totalFreightCost += allocQty * freight;
                    totalProcurementCost += allocQty * procurement;

                    allocationList.add(AllocationItemDto.builder()
                            .id((long) (allocationList.size() + 1))
                            .planType("BASELINE_FIFO")
                            .batchCode(batch.getBatchCode())
                            .commodity(comm)
                            .destinationName(market.getName())
                            .destinationCity(market.getLocationCity())
                            .destinationTier(market.getTier().name())
                            .allocatedQuantityKg(allocQty)
                            .basePricePerKg(basePrice)
                            .discountPercentage(0.0)
                            .effectivePricePerKg(0.0)
                            .freightCostPerKg(freight)
                            .netMarginPerKg(-(procurement + freight + disposalPenaltyPerKg))
                            .totalNetProfit(-loss)
                            .transitTimeHours(transitHours)
                            .remainingShelfLifeOnArrivalHours(arrivalRsl)
                            .decisionRationale("REJECTED / SPOILED: Naive FIFO dispatched without checking shelf-life viability. RSL on arrival (" 
                                    + String.format("%.1f", arrivalRsl) + "h) violated market buffer (" + market.getMinShelfLifeBufferHours() + "h).")
                            .build());
                } else {
                    totalAllocatedKg += allocQty;
                    double grossRev = allocQty * basePrice;
                    double cost = allocQty * (freight + handling + procurement);
                    double profit = grossRev - cost;

                    totalGrossRevenue += grossRev;
                    totalFreightCost += (allocQty * freight);
                    totalHandlingCost += (allocQty * handling);
                    totalProcurementCost += (allocQty * procurement);

                    allocationList.add(AllocationItemDto.builder()
                            .id((long) (allocationList.size() + 1))
                            .planType("BASELINE_FIFO")
                            .batchCode(batch.getBatchCode())
                            .commodity(comm)
                            .destinationName(market.getName())
                            .destinationCity(market.getLocationCity())
                            .destinationTier(market.getTier().name())
                            .allocatedQuantityKg(allocQty)
                            .basePricePerKg(basePrice)
                            .discountPercentage(0.0)
                            .effectivePricePerKg(basePrice)
                            .freightCostPerKg(freight)
                            .netMarginPerKg(basePrice - freight - handling - procurement)
                            .totalNetProfit(profit)
                            .transitTimeHours(transitHours)
                            .remainingShelfLifeOnArrivalHours(arrivalRsl)
                            .decisionRationale("Accepted: Standard FIFO dispatch. Arrived with " + String.format("%.1f", arrivalRsl) + "h shelf life.")
                            .build());
                }

                batchRemQty -= allocQty;
                marketDemands.put(comm, dem - allocQty);
            }

            // Any unallocated batch quantity spoils in warehouse
            if (batchRemQty > 0) {
                totalSpoiledKg += batchRemQty;
                double warehouseLoss = batchRemQty * (batch.getProcurementCostPerKg() + disposalPenaltyPerKg);
                totalSpoilageLoss += warehouseLoss;
                totalProcurementCost += batchRemQty * batch.getProcurementCostPerKg();
            }
        }

        double totalBatchTonnage = batches.stream().mapToDouble(ProduceBatch::getInitialQuantityKg).sum();
        double spoilagePercent = totalBatchTonnage > 0 ? (totalSpoiledKg / totalBatchTonnage) * 100.0 : 0.0;
        double netProfit = totalGrossRevenue - totalFreightCost - totalHandlingCost - totalProcurementCost - totalSpoilageLoss;
        double fulfillmentPercent = totalDemandRequested > 0 ? (totalAllocatedKg / totalDemandRequested) * 100.0 : 0.0;

        return OptimizationResultDto.builder()
                .status("BASELINE_SIMULATION_COMPLETED")
                .planType("BASELINE_FIFO")
                .totalDispatches(allocationList.size())
                .totalAllocatedKg(Math.round(totalAllocatedKg * 100.0) / 100.0)
                .totalSpoiledKg(Math.round(totalSpoiledKg * 100.0) / 100.0)
                .spoilagePercentage(Math.round(spoilagePercent * 10.0) / 10.0)
                .grossSalesRevenue(Math.round(totalGrossRevenue * 100.0) / 100.0)
                .totalFreightCost(Math.round(totalFreightCost * 100.0) / 100.0)
                .totalDiscountsGranted(0.0)
                .totalSpoilageLoss(Math.round(totalSpoilageLoss * 100.0) / 100.0)
                .netEconomicProfit(Math.round(netProfit * 100.0) / 100.0)
                .overallDemandFulfillmentPercent(Math.round(fulfillmentPercent * 10.0) / 10.0)
                .zeroSpoilageCorridors(0)
                .allocations(allocationList)
                .build();
    }
}
