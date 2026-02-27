package com.pollen.management.property;

import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.SalaryServiceImpl;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Feature: salary-period, Property 3: 用户-周期唯一性
 * **Validates: Requirements 1.3, 1.4, 2.3**
 *
 * For any 用户 ID 和周期组合，系统中最多存在一条对应的薪资记录。
 * 尝试创建重复的用户-周期组合应被拒绝。
 */
class UserPeriodUniquenessPropertyTest {

    @SuppressWarnings("unchecked")
    private SalaryServiceImpl createServiceWithMocks(
            SalaryRecordRepository salaryRecordRepository,
            UserRepository userRepository) throws Exception {
        var ctor = SalaryServiceImpl.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return (SalaryServiceImpl) ctor.newInstance(
                salaryRecordRepository, userRepository, null, null, null);
    }

    /**
     * 对同一周期调用两次 createPeriod，第二次应抛出 BusinessException(409)。
     * 模拟第二次调用时 findByPeriod 返回非空列表（表示周期已存在）。
     */
    @Property(tries = 100)
    void duplicatePeriodCreationIsRejected(
            @ForAll("validPeriods") String period,
            @ForAll("formalMemberLists") List<User> members) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        // First call: period does not exist yet
        List<SalaryRecord> createdRecords = members.stream()
                .map(m -> SalaryRecord.builder().userId(m.getId()).period(period).build())
                .collect(Collectors.toList());

        when(salaryRepo.findByPeriod(period))
                .thenReturn(Collections.emptyList())   // first call: empty
                .thenReturn(createdRecords);            // second call: already exists
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN))).thenReturn(members);

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo, userRepo);

        // First creation succeeds
        List<SalaryRecord> result = service.createPeriod(period);
        assertThat(result).hasSize(members.size());

        // Second creation with same period must be rejected with 409
        assertThatThrownBy(() -> service.createPeriod(period))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(409));
    }

    /**
     * 对于任意一组创建的薪资记录，不存在两条记录共享相同的 (userId, period) 组合。
     */
    @Property(tries = 100)
    void createdRecordsHaveUniqueUserPeriodCombinations(
            @ForAll("validPeriods") String period,
            @ForAll("uniqueMemberLists") List<User> members) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        when(salaryRepo.findByPeriod(period)).thenReturn(Collections.emptyList());
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN))).thenReturn(members);

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo, userRepo);

        List<SalaryRecord> result = service.createPeriod(period);

        // Group by (userId, period) — each combination must appear at most once
        Map<String, Long> counts = result.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getUserId() + ":" + r.getPeriod(),
                        Collectors.counting()));

        assertThat(counts.values()).allSatisfy(count ->
                assertThat(count).as("Each (userId, period) combination must be unique").isEqualTo(1L));
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> validPeriods() {
        return Combinators.combine(
                Arbitraries.integers().between(2000, 2099),
                Arbitraries.integers().between(1, 12)
        ).as((year, month) -> String.format("%04d-%02d", year, month));
    }

    @Provide
    Arbitrary<List<User>> formalMemberLists() {
        Arbitrary<User> memberArb = Combinators.combine(
                Arbitraries.longs().between(1L, 10000L),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(12),
                Arbitraries.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN)
        ).as((id, username, role) -> User.builder()
                .id(id)
                .username(username)
                .password("hashed")
                .role(role)
                .build());

        return memberArb.list().ofMinSize(1).ofMaxSize(20);
    }

    /**
     * Generates member lists with guaranteed unique user IDs,
     * ensuring the uniqueness property is testable without ID collisions from the generator.
     */
    @Provide
    Arbitrary<List<User>> uniqueMemberLists() {
        return Arbitraries.integers().between(1, 20).flatMap(size ->
                Combinators.combine(
                        Arbitraries.longs().between(1L, 10000L).list().ofSize(size).uniqueElements(),
                        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(12).list().ofSize(size),
                        Arbitraries.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN).list().ofSize(size)
                ).as((ids, usernames, roles) -> {
                    List<User> members = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        members.add(User.builder()
                                .id(ids.get(i))
                                .username(usernames.get(i))
                                .password("hashed")
                                .role(roles.get(i))
                                .build());
                    }
                    return members;
                })
        );
    }
}
