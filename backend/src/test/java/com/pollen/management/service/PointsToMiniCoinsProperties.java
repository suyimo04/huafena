package com.pollen.management.service;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 20: 积分转迷你币换算
 * **Validates: Requirements 6.5**
 *
 * Property 20: For any points value P, the converted mini coins value equals P × 2.
 * The conversion is a pure linear function: convert(a + b) = convert(a) + convert(b), and convert(0) = 0.
 */
class PointsToMiniCoinsProperties {

    private final PointsServiceImpl service = new PointsServiceImpl(null, null);

    // ========== Property 20a: miniCoins = points * 2 ==========

    /**
     * For any points value, convertPointsToMiniCoins(points) always equals points * 2.
     */
    @Property(tries = 100)
    void conversionAlwaysDoubles(@ForAll("pointsValues") int points) {
        int result = service.convertPointsToMiniCoins(points);

        assertThat(result)
                .as("convertPointsToMiniCoins(%d) must equal %d * 2 = %d", points, points, points * 2)
                .isEqualTo(points * 2);
    }

    // ========== Property 20b: Linearity — convert(a + b) = convert(a) + convert(b) ==========

    /**
     * The conversion is linear: converting the sum equals the sum of conversions.
     */
    @Property(tries = 100)
    void conversionIsLinear(
            @ForAll("smallPointsValues") int a,
            @ForAll("smallPointsValues") int b) {
        int convertSum = service.convertPointsToMiniCoins(a + b);
        int sumConvert = service.convertPointsToMiniCoins(a) + service.convertPointsToMiniCoins(b);

        assertThat(convertSum)
                .as("convert(%d + %d) must equal convert(%d) + convert(%d)", a, b, a, b)
                .isEqualTo(sumConvert);
    }

    // ========== Property 20c: convert(0) = 0 ==========

    /**
     * Converting zero points always yields zero mini coins.
     */
    @Property(tries = 1)
    void convertZeroIsZero() {
        assertThat(service.convertPointsToMiniCoins(0))
                .as("convert(0) must be 0")
                .isEqualTo(0);
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Integer> pointsValues() {
        return Arbitraries.integers().between(-10_000, 10_000);
    }

    @Provide
    Arbitrary<Integer> smallPointsValues() {
        return Arbitraries.integers().between(-5_000, 5_000);
    }
}
