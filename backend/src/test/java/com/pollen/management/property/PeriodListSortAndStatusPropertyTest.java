package com.pollen.management.property;

import com.pollen.management.dto.SalaryPeriodDTO;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Feature: salary-period, Property 4: 周期列表排序与状态正确性
 * **Validates: Requirements 2.4**
 *
 * For any 一组包含不同归档状态的薪酬周期，getPeriodList 返回的列表应按周期标识降序排列，
 * 且每个周期的 archived 状态应与该周期内记录的实际归档状态一致。
 */
class PeriodListSortAndStatusPropertyTest {

    private SalaryServiceImpl createServiceWithMocks(
            SalaryRecordRepository salaryRecordRepository) throws Exception {
        var ctor = SalaryServiceImpl.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return (SalaryServiceImpl) ctor.newInstance(
                salaryRecordRepository, null, null, null, null);
    }

    /**
     * getPeriodList 返回的列表应按周期标识降序排列。
     */
    @Property(tries = 100)
    void periodListIsSortedInDescendingOrder(
            @ForAll("periodDataSets") List<PeriodData> dataSet) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);

        // findDistinctPeriods returns periods sorted DESC (as per repository @Query)
        List<String> sortedPeriods = dataSet.stream()
                .map(PeriodData::period)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        when(salaryRepo.findDistinctPeriods()).thenReturn(sortedPeriods);
        for (PeriodData pd : dataSet) {
            when(salaryRepo.existsByPeriodAndArchivedTrue(pd.period())).thenReturn(pd.archived());
            when(salaryRepo.countByPeriod(pd.period())).thenReturn(pd.recordCount());
        }

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo);
        List<SalaryPeriodDTO> result = service.getPeriodList();

        // Verify descending order
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getPeriod())
                    .as("Period at index %d should be >= period at index %d", i, i + 1)
                    .isGreaterThanOrEqualTo(result.get(i + 1).getPeriod());
        }
    }

    /**
     * 每个周期的 archived 状态应与 existsByPeriodAndArchivedTrue 的返回值一致。
     */
    @Property(tries = 100)
    void eachPeriodArchivedStatusMatchesMockedValue(
            @ForAll("periodDataSets") List<PeriodData> dataSet) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);

        List<String> sortedPeriods = dataSet.stream()
                .map(PeriodData::period)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        Map<String, Boolean> expectedArchived = new HashMap<>();
        Map<String, Long> expectedCount = new HashMap<>();

        when(salaryRepo.findDistinctPeriods()).thenReturn(sortedPeriods);
        for (PeriodData pd : dataSet) {
            expectedArchived.put(pd.period(), pd.archived());
            expectedCount.put(pd.period(), pd.recordCount());
            when(salaryRepo.existsByPeriodAndArchivedTrue(pd.period())).thenReturn(pd.archived());
            when(salaryRepo.countByPeriod(pd.period())).thenReturn(pd.recordCount());
        }

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo);
        List<SalaryPeriodDTO> result = service.getPeriodList();

        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.isArchived())
                    .as("Archived status for period %s", dto.getPeriod())
                    .isEqualTo(expectedArchived.get(dto.getPeriod()));
        });
    }

    /**
     * 每个周期的 recordCount 应与 countByPeriod 的返回值一致。
     */
    @Property(tries = 100)
    void eachPeriodRecordCountMatchesMockedValue(
            @ForAll("periodDataSets") List<PeriodData> dataSet) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);

        List<String> sortedPeriods = dataSet.stream()
                .map(PeriodData::period)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        Map<String, Long> expectedCount = new HashMap<>();

        when(salaryRepo.findDistinctPeriods()).thenReturn(sortedPeriods);
        for (PeriodData pd : dataSet) {
            expectedCount.put(pd.period(), pd.recordCount());
            when(salaryRepo.existsByPeriodAndArchivedTrue(pd.period())).thenReturn(pd.archived());
            when(salaryRepo.countByPeriod(pd.period())).thenReturn(pd.recordCount());
        }

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo);
        List<SalaryPeriodDTO> result = service.getPeriodList();

        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getRecordCount())
                    .as("Record count for period %s", dto.getPeriod())
                    .isEqualTo(expectedCount.get(dto.getPeriod()));
        });
    }

    // --- Data record for period test data ---

    record PeriodData(String period, boolean archived, long recordCount) {}

    // --- Generators ---

    /**
     * Generates a list of unique periods with random archived status and record counts.
     * Periods are in valid YYYY-MM format with unique values.
     */
    @Provide
    Arbitrary<List<PeriodData>> periodDataSets() {
        return Arbitraries.integers().between(1, 15).flatMap(size ->
                Combinators.combine(
                        validUniquePeriods(size),
                        Arbitraries.of(true, false).list().ofSize(size),
                        Arbitraries.longs().between(1L, 100L).list().ofSize(size)
                ).as((periods, archivedFlags, counts) -> {
                    List<PeriodData> result = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        result.add(new PeriodData(periods.get(i), archivedFlags.get(i), counts.get(i)));
                    }
                    return result;
                })
        );
    }

    /**
     * Generates a list of unique valid YYYY-MM period strings.
     */
    private Arbitrary<List<String>> validUniquePeriods(int size) {
        return Combinators.combine(
                Arbitraries.integers().between(2000, 2099),
                Arbitraries.integers().between(1, 12)
        ).as((year, month) -> String.format("%04d-%02d", year, month))
                .list().ofMinSize(size).ofMaxSize(size).uniqueElements();
    }
}
