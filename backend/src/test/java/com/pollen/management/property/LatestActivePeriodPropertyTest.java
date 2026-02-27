package com.pollen.management.property;

import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Feature: salary-period, Property 6: 最新活跃周期选择正确性
 * **Validates: Requirements 3.2**
 *
 * For any 一组包含活跃和已归档周期的系统状态，getLatestActivePeriod 返回的周期
 * 应是所有未归档周期中字典序最大的那个。
 */
class LatestActivePeriodPropertyTest {

    private SalaryServiceImpl createServiceWithMocks(
            SalaryRecordRepository salaryRecordRepository) throws Exception {
        var ctor = SalaryServiceImpl.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return (SalaryServiceImpl) ctor.newInstance(
                salaryRecordRepository, null, null, null, null);
    }

    /**
     * When active periods exist, getLatestActivePeriod returns the
     * lexicographically largest one among all non-archived periods.
     */
    @Property(tries = 100)
    void returnsLexicographicallyLargestActivePeriod(
            @ForAll("scenariosWithActivePeriods") PeriodScenario scenario) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);

        // findDistinctActivePeriods returns active periods sorted DESC
        List<String> activeSortedDesc = scenario.activePeriods().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        when(salaryRepo.findDistinctActivePeriods()).thenReturn(activeSortedDesc);

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo);
        String result = service.getLatestActivePeriod();

        // The expected result is the lexicographically largest active period
        String expected = scenario.activePeriods().stream()
                .max(Comparator.naturalOrder())
                .orElse(null);

        assertThat(result)
                .as("Should return the lexicographically largest active period")
                .isEqualTo(expected);
    }

    /**
     * When no active periods exist (all archived or empty), getLatestActivePeriod returns null.
     */
    @Property(tries = 100)
    void returnsNullWhenNoActivePeriods(
            @ForAll("scenariosWithNoActivePeriods") PeriodScenario scenario) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);

        when(salaryRepo.findDistinctActivePeriods()).thenReturn(Collections.emptyList());

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo);
        String result = service.getLatestActivePeriod();

        assertThat(result)
                .as("Should return null when no active periods exist")
                .isNull();
    }

    /**
     * The returned period must not be any of the archived periods.
     */
    @Property(tries = 100)
    void returnedPeriodIsNeverArchived(
            @ForAll("scenariosWithActivePeriods") PeriodScenario scenario) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);

        List<String> activeSortedDesc = scenario.activePeriods().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        when(salaryRepo.findDistinctActivePeriods()).thenReturn(activeSortedDesc);

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo);
        String result = service.getLatestActivePeriod();

        assertThat(result).isNotNull();
        assertThat(scenario.archivedPeriods())
                .as("Returned period '%s' must not be among archived periods", result)
                .doesNotContain(result);
        assertThat(scenario.activePeriods())
                .as("Returned period '%s' must be among active periods", result)
                .contains(result);
    }

    // --- Data record ---

    record PeriodScenario(
            List<String> activePeriods,
            List<String> archivedPeriods
    ) {}

    // --- Generators ---

    /**
     * Generates scenarios where at least 1 active period exists,
     * with a mix of active and archived periods.
     */
    @Provide
    Arbitrary<PeriodScenario> scenariosWithActivePeriods() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 10),  // active count (>= 1)
                Arbitraries.integers().between(0, 10)   // archived count
        ).flatAs((activeCount, archivedCount) ->
                uniquePeriods(activeCount + archivedCount).map(allPeriods -> {
                    List<String> active = allPeriods.subList(0, activeCount);
                    List<String> archived = allPeriods.subList(activeCount, allPeriods.size());
                    return new PeriodScenario(
                            new ArrayList<>(active),
                            new ArrayList<>(archived));
                })
        );
    }

    /**
     * Generates scenarios where no active periods exist —
     * either all periods are archived or there are no periods at all.
     */
    @Provide
    Arbitrary<PeriodScenario> scenariosWithNoActivePeriods() {
        return Arbitraries.integers().between(0, 10).flatMap(archivedCount ->
                uniquePeriods(archivedCount).map(periods ->
                        new PeriodScenario(
                                Collections.emptyList(),
                                new ArrayList<>(periods)))
        );
    }

    private Arbitrary<List<String>> uniquePeriods(int size) {
        if (size == 0) {
            return Arbitraries.just(Collections.emptyList());
        }
        return Combinators.combine(
                Arbitraries.integers().between(2000, 2099),
                Arbitraries.integers().between(1, 12)
        ).as((year, month) -> String.format("%04d-%02d", year, month))
                .list().ofMinSize(size).ofMaxSize(size).uniqueElements();
    }
}
