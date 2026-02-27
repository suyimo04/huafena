package com.pollen.management.property;

import com.pollen.management.dto.SalaryMemberDTO;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Feature: salary-period, Property 5: 周期查询隔离性
 * **Validates: Requirements 3.1, 6.1, 6.2**
 *
 * For any 包含多个周期数据的系统状态，按周期 P 查询薪资记录时，
 * 返回的所有记录的 period 字段均等于 P，且不包含其他周期的记录。
 */
class PeriodQueryIsolationPropertyTest {

    private SalaryServiceImpl createServiceWithMocks(
            SalaryRecordRepository salaryRecordRepository,
            UserRepository userRepository) throws Exception {
        var ctor = SalaryServiceImpl.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return (SalaryServiceImpl) ctor.newInstance(
                salaryRecordRepository, userRepository, null, null, null);
    }

    /**
     * 按周期 P 查询时，返回的 DTO 中所有有薪资记录的成员，
     * 其数据均来自周期 P 的记录，不包含其他周期的数据。
     */
    @Property(tries = 100)
    void queryByPeriodReturnsOnlyRecordsOfThatPeriod(
            @ForAll("multiPeriodScenarios") MultiPeriodScenario scenario) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        // Mock findByPeriod to return only records for the queried period
        when(salaryRepo.findByPeriod(scenario.queriedPeriod()))
                .thenReturn(scenario.recordsForQueriedPeriod());

        when(userRepo.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN)))
                .thenReturn(scenario.users());

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo, userRepo);

        List<SalaryMemberDTO> result = service.getSalaryMembers(scenario.queriedPeriod());

        // All returned DTOs with a salary record id should correspond to records from the queried period
        List<SalaryMemberDTO> withRecords = result.stream()
                .filter(dto -> dto.getId() != null)
                .collect(Collectors.toList());

        Set<Long> queriedPeriodRecordIds = scenario.recordsForQueriedPeriod().stream()
                .map(SalaryRecord::getId)
                .collect(Collectors.toSet());

        assertThat(withRecords).allSatisfy(dto ->
                assertThat(queriedPeriodRecordIds)
                        .as("DTO record id %d should belong to queried period '%s'",
                                dto.getId(), scenario.queriedPeriod())
                        .contains(dto.getId()));

        // Verify findByPeriod was called only with the queried period
        verify(salaryRepo, times(1)).findByPeriod(scenario.queriedPeriod());
        // Ensure no other period was queried
        verify(salaryRepo, times(1)).findByPeriod(anyString());
    }

    /**
     * 查询周期 P 时，其他周期的记录不会出现在结果中。
     * 验证返回的 userId 集合中，有记录的用户 ID 仅来自周期 P 的记录。
     */
    @Property(tries = 100)
    void queryByPeriodExcludesOtherPeriodRecords(
            @ForAll("multiPeriodScenarios") MultiPeriodScenario scenario) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        when(salaryRepo.findByPeriod(scenario.queriedPeriod()))
                .thenReturn(scenario.recordsForQueriedPeriod());

        when(userRepo.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN)))
                .thenReturn(scenario.users());

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo, userRepo);

        List<SalaryMemberDTO> result = service.getSalaryMembers(scenario.queriedPeriod());

        // Collect user IDs that have actual salary data (non-null record id)
        Set<Long> userIdsWithData = result.stream()
                .filter(dto -> dto.getId() != null)
                .map(SalaryMemberDTO::getUserId)
                .collect(Collectors.toSet());

        // These should be a subset of user IDs from the queried period's records
        Set<Long> queriedPeriodUserIds = scenario.recordsForQueriedPeriod().stream()
                .map(SalaryRecord::getUserId)
                .collect(Collectors.toSet());

        assertThat(userIdsWithData)
                .as("Users with salary data should only come from queried period '%s'",
                        scenario.queriedPeriod())
                .isSubsetOf(queriedPeriodUserIds);

        // User IDs exclusive to other periods should NOT have data in the result
        Set<Long> otherPeriodOnlyUserIds = scenario.recordsForOtherPeriods().stream()
                .map(SalaryRecord::getUserId)
                .filter(uid -> !queriedPeriodUserIds.contains(uid))
                .collect(Collectors.toSet());

        if (!otherPeriodOnlyUserIds.isEmpty()) {
            assertThat(userIdsWithData)
                    .as("Users exclusive to other periods should not have data")
                    .doesNotContainAnyElementsOf(otherPeriodOnlyUserIds);
        }
    }

    // --- Data record ---

    record MultiPeriodScenario(
            String queriedPeriod,
            List<String> allPeriods,
            List<User> users,
            List<SalaryRecord> recordsForQueriedPeriod,
            List<SalaryRecord> recordsForOtherPeriods
    ) {}

    // --- Generators ---

    @Provide
    Arbitrary<MultiPeriodScenario> multiPeriodScenarios() {
        return Combinators.combine(
                Arbitraries.integers().between(2, 5),   // number of periods
                Arbitraries.integers().between(1, 10)   // number of users
        ).flatAs((numPeriods, numUsers) ->
                Combinators.combine(
                        uniquePeriods(numPeriods),
                        uniqueUsers(numUsers)
                ).flatAs((periods, users) -> {
                    // Pick a random period to query
                    return Arbitraries.integers().between(0, periods.size() - 1)
                            .map(queryIdx -> {
                                String queriedPeriod = periods.get(queryIdx);

                                // Build records for each period
                                List<SalaryRecord> queriedRecords = new ArrayList<>();
                                List<SalaryRecord> otherRecords = new ArrayList<>();
                                long recordId = 1L;

                                for (String period : periods) {
                                    // Each period gets a random subset of users
                                    for (User user : users) {
                                        // Deterministic assignment: use hash to decide inclusion
                                        if (Math.abs((period + user.getId()).hashCode() % 3) != 0) {
                                            SalaryRecord record = SalaryRecord.builder()
                                                    .id(recordId++)
                                                    .userId(user.getId())
                                                    .period(period)
                                                    .basePoints(Math.abs(
                                                            (period + user.getId()).hashCode() % 100))
                                                    .build();
                                            if (period.equals(queriedPeriod)) {
                                                queriedRecords.add(record);
                                            } else {
                                                otherRecords.add(record);
                                            }
                                        }
                                    }
                                }

                                return new MultiPeriodScenario(
                                        queriedPeriod, periods, users,
                                        queriedRecords, otherRecords);
                            });
                })
        );
    }

    private Arbitrary<List<String>> uniquePeriods(int size) {
        return Combinators.combine(
                Arbitraries.integers().between(2000, 2099),
                Arbitraries.integers().between(1, 12)
        ).as((year, month) -> String.format("%04d-%02d", year, month))
                .list().ofMinSize(size).ofMaxSize(size).uniqueElements();
    }

    private Arbitrary<List<User>> uniqueUsers(int size) {
        return Arbitraries.integers().between(1, size).flatMap(actualSize ->
                Arbitraries.longs().between(1L, 10000L)
                        .list().ofSize(size).uniqueElements()
                        .map(ids -> {
                            List<User> users = new ArrayList<>();
                            Role[] roles = {Role.LEADER, Role.VICE_LEADER, Role.INTERN};
                            for (int i = 0; i < ids.size(); i++) {
                                users.add(User.builder()
                                        .id(ids.get(i))
                                        .username("user" + ids.get(i))
                                        .password("hashed")
                                        .role(roles[i % roles.length])
                                        .build());
                            }
                            return users;
                        })
        );
    }
}
