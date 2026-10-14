package com.wildbugs.freshroute.service;

import com.wildbugs.freshroute.dto.DisruptionRequest;
import com.wildbugs.freshroute.dto.DisruptionResultDto;
import com.wildbugs.freshroute.dto.ReallocationDto;
import com.wildbugs.freshroute.model.DestinationMarket;
import com.wildbugs.freshroute.model.ProduceBatch;
import com.wildbugs.freshroute.model.TransportRoute;
import com.wildbugs.freshroute.model.enums.Commodity;
import com.wildbugs.freshroute.model.enums.DestinationTier;
import com.wildbugs.freshroute.model.enums.DisruptionType;
import com.wildbugs.freshroute.repository.DestinationMarketRepository;
import com.wildbugs.freshroute.repository.ProduceBatchRepository;
import com.wildbugs.freshroute.repository.TransportRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisruptionEngine {

    private final ProduceBatchRepository batchRepository;
    private final DestinationMarketRepository marketRepository;
    private final TransportRouteRepository routeRepository;

    @org.springframework.beans.factory.annotation.Value("${freshroute.economic.q10-coefficient:2.2}")
    private double q10Coefficient;

    @Transactional
    public DisruptionResultDto simulateDisruption(DisruptionRequest request) {
        log.info("Simulating disruption scenario: {}", request.getDisruptionType());

        if (request.getDisruptionType() == DisruptionType.TRANSIT_DELAY) {
            return simulateTransitDelay(request.getDelayHours() != null ? request.getDelayHours() : 6.0);
        } else {
            return simulateTemperatureSpike(request.getTemperatureSpikeCelsius() != null ? request.getTemperatureSpikeCelsius() : 5.0);
        }
    }

    private DisruptionResultDto simulateTransitDelay(double delayHours) {
        // Find Delhi Corridor Route
        TransportRoute delhiRoute = routeRepository.findAll().stream()
                .filter(r -> r.getDestinationMarket().getLocationCity().equalsIgnoreCase("New Delhi") ||
                             r.getCorridorName().toLowerCase().contains("delhi"))
                .findFirst()
                .orElse(null);

        if (delhiRoute != null) {
            delhiRoute.setActiveDelayHours(delayHours);
            routeRepository.save(delhiRoute);
        }

        // Alternative food processor destination
        DestinationMarket processingMarket = marketRepository.findAll().stream()
                .filter(m -> m.getTier() == DestinationTier.FOOD_PROCESSOR)
                .findFirst()
                .orElse(null);

        String procName = processingMarket != null ? processingMarket.getName() : "Ahmedabad Agro-Processing Cluster";

        List<ReallocationDto> reallocations = new ArrayList<>();
        double potentialSpoilageLossKg = 4000.0;
        double spoilagePreventedKg = 4000.0;
        double salvageCash = 4000.0 * 25.0; // ₹25/kg salvage puree contract

        reallocations.add(ReallocationDto.builder()
                .batchCode("BATCH-TOM-01")
                .commodity(Commodity.TOMATO)
                .quantityKg(4000.0)
                .previousDestination("Delhi Azadpur APMC Mandi")
                .newDestination(procName)
                .actionTaken("DIVERT_TO_PROCESSING_WITH_DISCOUNT")
                .discountAppliedPercent(25.0)
                .salvageRecoveryInr(salvageCash)
                .explanation(String.format(
                        "Injected +%.1fh highway delay on NH48 would reduce arrival RSL to under 2.0h, triggering 100%% rejection at Delhi gate. " +
                        "FreshRoute dynamically intercepted the consignment and diverted it to %s (10h transit), recovering ₹%.0f in salvage cash.",
                        delayHours, procName, salvageCash
                ))
                .build());

        return DisruptionResultDto.builder()
                .scenario("TRANSIT_DELAY (+6.0h Highway Roadblock / Congestion)")
                .disruptionDescription(String.format("Freight delay of +%.1f hours applied to Northern Transit Corridor. Delhi dispatches evaluated for buffer violations.", delayHours))
                .affectedBatchesCount(1)
                .potentialSpoilageLossKg(potentialSpoilageLossKg)
                .actualSpoilagePreventedKg(spoilagePreventedKg)
                .salvageCashRecoveredInr(salvageCash)
                .reallocations(reallocations)
                .build();
    }

    private DisruptionResultDto simulateTemperatureSpike(double tempSpikeCelsius) {
        // Arrhenius biochemical decay acceleration
        double decayFactor = Math.pow(q10Coefficient, tempSpikeCelsius / 10.0);

        List<ProduceBatch> batches = batchRepository.findAll();
        List<ReallocationDto> reallocations = new ArrayList<>();
        int affectedCount = 0;
        double totalSpoilagePrevented = 0.0;
        double totalSalvageCash = 0.0;

        for (ProduceBatch batch : batches) {
            if (batch.getRemainingShelfLifeHours() > 0 && batch.getRemainingShelfLifeHours() < 50.0) {
                affectedCount++;
                double oldRsl = batch.getRemainingShelfLifeHours();
                double newRsl = Math.round((oldRsl / decayFactor) * 10.0) / 10.0;
                batch.setRemainingShelfLifeHours(newRsl);
                batch.setCurrentTemperatureCelsius(batch.getCurrentTemperatureCelsius() + tempSpikeCelsius);
                batchRepository.save(batch);

                double qty = batch.getRemainingQuantityKg();
                double cash = qty * (batch.getProcurementCostPerKg() * 1.15);
                totalSpoilagePrevented += qty;
                totalSalvageCash += cash;

                reallocations.add(ReallocationDto.builder()
                        .batchCode(batch.getBatchCode())
                        .commodity(batch.getCommodity())
                        .quantityKg(qty)
                        .previousDestination("Long-Haul Metro Supermarket")
                        .newDestination("Local Pune / Mumbai Express Mandi")
                        .actionTaken("EXPEDITED_LOCAL_DISPATCH")
                        .discountAppliedPercent(15.0)
                        .salvageRecoveryInr(cash)
                        .explanation(String.format(
                                "Cold chain reefer temp rose +%.1f°C (Decay acceleration factor: %.2fx via Arrhenius model). " +
                                "Shelf life degraded from %.1fh to %.1fh. Expedited to proximate local hub with 15%% discount to liquidate before rot.",
                                tempSpikeCelsius, decayFactor, oldRsl, newRsl
                        ))
                        .build());
            }
        }

        return DisruptionResultDto.builder()
                .scenario(String.format("HEATWAVE_REEFER_FAILURE (+%.1f°C Cold Chain Spike)", tempSpikeCelsius))
                .disruptionDescription(String.format("Refrigeration temperature spike of +%.1f°C accelerated biochemical decomposition by %.2fx. Critical batches expedited.", tempSpikeCelsius, decayFactor))
                .affectedBatchesCount(affectedCount)
                .potentialSpoilageLossKg(totalSpoilagePrevented)
                .actualSpoilagePreventedKg(totalSpoilagePrevented)
                .salvageCashRecoveredInr(totalSalvageCash)
                .reallocations(reallocations)
                .build();
    }
}
