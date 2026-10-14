package com.wildbugs.freshroute;

import com.wildbugs.freshroute.service.DynamicDiscountEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicDiscountEngineTest {

    private final DynamicDiscountEngine discountEngine = new DynamicDiscountEngine();

    @Test
    @DisplayName("Safe Shelf-Life Zone: rho < 0.60 yields 0% discount")
    void testSafeZoneZeroDiscount() {
        // Transit 10h + Buffer 10h = 20h. Remaining RSL = 50h -> rho = 20/50 = 0.40 (< 0.60)
        double discount = discountEngine.calculateDiscountPercentage(10.0, 10.0, 50.0, true);
        assertEquals(0.00, discount, 0.001);
    }

    @Test
    @DisplayName("Velocity Acceleration Zone: 0.60 <= rho < 0.80 yields 15% discount")
    void testVelocityZoneFifteenPercentDiscount() {
        // Transit 15h + Buffer 20h = 35h. Remaining RSL = 50h -> rho = 35/50 = 0.70
        double discount = discountEngine.calculateDiscountPercentage(15.0, 20.0, 50.0, true);
        assertEquals(0.15, discount, 0.001);
    }

    @Test
    @DisplayName("Critical Liquidation Zone: rho >= 0.80 yields 35% salvage markdown")
    void testCriticalLiquidationThirtyFivePercentDiscount() {
        // Transit 18h + Buffer 24h = 42h. Remaining RSL = 45h -> rho = 42/45 = 0.933
        double discount = discountEngine.calculateDiscountPercentage(18.0, 24.0, 45.0, true);
        assertEquals(0.35, discount, 0.001);
    }

    @Test
    @DisplayName("Disabled Dynamic Discounting always returns 0%")
    void testDisabledDiscountingReturnsZero() {
        double discount = discountEngine.calculateDiscountPercentage(18.0, 24.0, 45.0, false);
        assertEquals(0.00, discount, 0.001);
    }
}
