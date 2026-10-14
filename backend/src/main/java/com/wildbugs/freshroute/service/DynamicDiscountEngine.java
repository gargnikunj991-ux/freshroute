package com.wildbugs.freshroute.service;

import org.springframework.stereotype.Service;

@Service
public class DynamicDiscountEngine {

    /**
     * Calculates the dynamic discount percentage based on the Shelf-Life Consumption Ratio (rho).
     * rho = (TransitTime + MinRetailShelfLifeBuffer) / RemainingShelfLife
     *
     * @param transitHours expected transit time in hours
     * @param minBufferHours mandatory destination shelf-life buffer
     * @param remainingShelfLifeHours remaining shelf life of batch
     * @param isEnabled flag to enable/disable dynamic discounting
     * @return discount percentage (0.00, 0.15, or 0.35)
     */
    public double calculateDiscountPercentage(double transitHours,
                                             double minBufferHours,
                                             double remainingShelfLifeHours,
                                             boolean isEnabled) {
        if (!isEnabled || remainingShelfLifeHours <= 0.0) {
            return 0.0;
        }

        double totalCommittedHours = transitHours + minBufferHours;
        double rho = totalCommittedHours / Math.max(1.0, remainingShelfLifeHours);

        if (rho < 0.60) {
            return 0.00; // Normal prime price
        } else if (rho < 0.80) {
            return 0.15; // 15% velocity acceleration discount
        } else {
            return 0.35; // 35% urgent salvage markdown
        }
    }
}
