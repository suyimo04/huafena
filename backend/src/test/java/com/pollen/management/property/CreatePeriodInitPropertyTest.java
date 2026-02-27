package com.pollen.management.property;

import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Feature: salary-period, Property 2: 周期创建初始化完整性
 * **Validates: Requirements 1.2, 2.2**
 *
 * For any 正式成员列表和合法的周期标识，创建新周期后，该周期内的薪资记录数量应等于正式成员数量，
 * 且每条记录的 period 字段等于指定周期，各维度积分字段均为 0。
 */
class CreatePeriodInitPropertyTest {

    @SuppressWarnings("unchecked")
    private SalaryServiceImpl createServiceWithMocks(
            SalaryRecordRepository salaryRecordRepository,
            UserRepository userRepository) throws Exception {
        var ctor = SalaryServiceImpl.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        // Constructor params: salaryRecordRepository, userRepository, pointsService, auditLogRepository, salaryConfigService
        return (SalaryServiceImpl) ctor.newInstance(
                salaryRecordRepository, userRepository, null, null, null);
    }

    @Property(tries = 100)
    void createdRecordsCountEqualsFormalMemberCount(
            @ForAll("validPeriods") String period,
            @ForAll("formalMemberLists") List<User> members) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        when(salaryRepo.findByPeriod(period)).thenReturn(Collections.emptyList());
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN))).thenReturn(members);

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo, userRepo);

        List<SalaryRecord> result = service.createPeriod(period);

        assertThat(result).hasSize(members.size());
    }

    @Property(tries = 100)
    void allCreatedRecordsHaveCorrectPeriod(
            @ForAll("validPeriods") String period,
            @ForAll("formalMemberLists") List<User> members) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        when(salaryRepo.findByPeriod(period)).thenReturn(Collections.emptyList());
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN))).thenReturn(members);

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo, userRepo);

        List<SalaryRecord> result = service.createPeriod(period);

        assertThat(result).allSatisfy(record ->
                assertThat(record.getPeriod()).isEqualTo(period));
    }

    @Property(tries = 100)
    void allCreatedRecordsHaveZeroPointFields(
            @ForAll("validPeriods") String period,
            @ForAll("formalMemberLists") List<User> members) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        when(salaryRepo.findByPeriod(period)).thenReturn(Collections.emptyList());
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN))).thenReturn(members);

        SalaryServiceImpl service = createServiceWithMocks(salaryRepo, userRepo);

        List<SalaryRecord> result = service.createPeriod(period);

        assertThat(result).allSatisfy(record -> {
            assertThat(record.getBasePoints()).isZero();
            assertThat(record.getBonusPoints()).isZero();
            assertThat(record.getDeductions()).isZero();
            assertThat(record.getTotalPoints()).isZero();
            assertThat(record.getMiniCoins()).isZero();
            assertThat(record.getSalaryAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(record.getCommunityActivityPoints()).isZero();
            assertThat(record.getCheckinCount()).isZero();
            assertThat(record.getCheckinPoints()).isZero();
            assertThat(record.getViolationHandlingCount()).isZero();
            assertThat(record.getViolationHandlingPoints()).isZero();
            assertThat(record.getTaskCompletionPoints()).isZero();
            assertThat(record.getAnnouncementCount()).isZero();
            assertThat(record.getAnnouncementPoints()).isZero();
            assertThat(record.getEventHostingPoints()).isZero();
            assertThat(record.getBirthdayBonusPoints()).isZero();
            assertThat(record.getMonthlyExcellentPoints()).isZero();
        });
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
}
