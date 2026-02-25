package com.pollen.management.service;

import net.jqwik.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 19: 签到积分计算
 * **Validates: Requirements 6.4**
 *
 * Property 19: For any monthly checkin count, the calculated points must satisfy:
 *   < 20  → -20
 *   20-29 → -10
 *   30-39 →   0
 *   40-49 → +30
 *   >= 50 → +50
 * The result is always one of {-20, -10, 0, 30, 50}.
 */
class CheckinPointsProperties {

    private final PointsServiceImpl service = new PointsServiceImpl(null, null);

    private static final Set<Integer> VALID_RESULTS = Set.of(-20, -10, 0, 30, 50);

    // ========== Property 19a: checkinCount < 20 returns -20 ==========

    @Property(tries = 100)
    void checkinCountBelow20ReturnsMinus20(
            @ForAll("belowTwenty") int checkinCount) {
        int result = service.calculateCheckinPoints(checkinCount);
        assertThat(result)
                .as("checkinCount=%d should yield -20", checkinCount)
                .isEqualTo(-20);
    }

    // ========== Property 19b: checkinCount in [20, 29] returns -10 ==========

    @Property(tries = 100)
    void checkinCount20to29ReturnsMinus10(
            @ForAll("twentyToTwentyNine") int checkinCount) {
        int result = service.calculateCheckinPoints(checkinCount);
        assertThat(result)
                .as("checkinCount=%d should yield -10", checkinCount)
                .isEqualTo(-10);
    }

    // ========== Property 19c: checkinCount in [30, 39] returns 0 ==========

    @Property(tries = 100)
    void checkinCount30to39ReturnsZero(
            @ForAll("thirtyToThirtyNine") int checkinCount) {
        int result = service.calculateCheckinPoints(checkinCount);
        assertThat(result)
                .as("checkinCount=%d should yield 0", checkinCount)
                .isEqualTo(0);
    }

    // ========== Property 19d: checkinCount in [40, 49] returns +30 ==========

    @Property(tries = 100)
    void checkinCount40to49ReturnsPlus30(
            @ForAll("fortyToFortyNine") int checkinCount) {
        int result = service.calculateCheckinPoints(checkinCount);
        assertThat(result)
                .as("checkinCount=%d should yield 30", checkinCount)
                .isEqualTo(30);
    }

    // ========== Property 19e: checkinCount >= 50 returns +50 ==========

    @Property(tries = 100)
    void checkinCount50OrAboveReturnsPlus50(
            @ForAll("fiftyOrAbove") int checkinCount) {
        int result = service.calculateCheckinPoints(checkinCount);
        assertThat(result)
                .as("checkinCount=%d should yield 50", checkinCount)
                .isEqualTo(50);
    }

    // ========== Property 19f: result is always one of the five valid values ==========

    @Property(tries = 100)
    void resultIsAlwaysOneOfFiveValidValues(
            @ForAll("anyCheckinCount") int checkinCount) {
        int result = service.calculateCheckinPoints(checkinCount);
        assertThat(VALID_RESULTS)
                .as("checkinCount=%d produced result=%d which is not in %s", checkinCount, result, VALID_RESULTS)
                .contains(result);
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Integer> belowTwenty() {
        return Arbitraries.integers().between(0, 19);
    }

    @Provide
    Arbitrary<Integer> twentyToTwentyNine() {
        return Arbitraries.integers().between(20, 29);
    }

    @Provide
    Arbitrary<Integer> thirtyToThirtyNine() {
        return Arbitraries.integers().between(30, 39);
    }

    @Provide
    Arbitrary<Integer> fortyToFortyNine() {
        return Arbitraries.integers().between(40, 49);
    }

    @Provide
    Arbitrary<Integer> fiftyOrAbove() {
        return Arbitraries.integers().between(50, 200);
    }

    @Provide
    Arbitrary<Integer> anyCheckinCount() {
        return Arbitraries.integers().between(0, 200);
    }
}
