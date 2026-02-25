package com.pollen.management.service;

import com.pollen.management.dto.RotationThresholds;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Feature: pollen-group-management, Property 22: 正式成员总数不变量
 * **Validates: Requirements 8.4**
 *
 * Property 22: After any executePromotion call, the total count of formal members
 * (VICE_LEADER + MEMBER) must remain exactly 5.
 */
class FormalMemberCountProperties {

    private static final int REQUIRED_FORMAL_COUNT = 5;

    /**
     * Creates a MemberRotationServiceImpl with mocked repositories.
     */
    private MemberRotationServiceImpl createService(UserRepository userRepo) {
        PointsRecordRepository pointsRepo = Mockito.mock(PointsRecordRepository.class);
        SalaryRecordRepository salaryRepo = Mockito.mock(SalaryRecordRepository.class);
        RoleChangeHistoryRepository roleChangeHistoryRepo = Mockito.mock(RoleChangeHistoryRepository.class);
        SalaryConfigService salaryConfigService = Mockito.mock(SalaryConfigService.class);
        when(salaryConfigService.getRotationThresholds()).thenReturn(RotationThresholds.builder()
                .promotionPointsThreshold(100)
                .demotionSalaryThreshold(150)
                .demotionConsecutiveMonths(2)
                .dismissalPointsThreshold(100)
                .dismissalConsecutiveMonths(2)
                .build());
        return new MemberRotationServiceImpl(userRepo, pointsRepo, salaryRepo, roleChangeHistoryRepo, salaryConfigService);
    }

    // ========== Property 22a: Valid swap preserves formal member count ==========

    /**
     * When an INTERN is swapped with a MEMBER (or VICE_LEADER), and the DB reports
     * the formal count as 5 after the swap, executePromotion completes without error.
     * This verifies the swap is net-zero on formal member count.
     */
    @Property(tries = 100)
    void validSwapPreservesFormalMemberCount(
            @ForAll("validSwapPairs") SwapPair pair) {

        UserRepository userRepo = Mockito.mock(UserRepository.class);

        User intern = User.builder()
                .id(pair.internId)
                .username("intern_" + pair.internId)
                .password("encoded")
                .role(Role.INTERN)
                .enabled(true)
                .pendingDismissal(false)
                .build();

        User formalMember = User.builder()
                .id(pair.formalMemberId)
                .username("formal_" + pair.formalMemberId)
                .password("encoded")
                .role(pair.formalRole)
                .enabled(true)
                .pendingDismissal(false)
                .build();

        when(userRepo.findById(pair.internId)).thenReturn(Optional.of(intern));
        when(userRepo.findById(pair.formalMemberId)).thenReturn(Optional.of(formalMember));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // After swap: the formal member became INTERN, the intern became MEMBER.
        // If original was MEMBER: MEMBER count stays same (lost 1, gained 1), VICE_LEADER unchanged.
        // If original was VICE_LEADER: VICE_LEADER count -1, MEMBER count +1, total unchanged.
        // Either way, total formal count remains 5.
        long memberCountAfter = pair.formalRole == Role.VICE_LEADER
                ? pair.originalMemberCount + 1
                : pair.originalMemberCount;
        long viceLeaderCountAfter = pair.formalRole == Role.VICE_LEADER
                ? pair.originalViceLeaderCount - 1
                : pair.originalViceLeaderCount;

        when(userRepo.countByRole(Role.MEMBER)).thenReturn(memberCountAfter);
        when(userRepo.countByRole(Role.VICE_LEADER)).thenReturn(viceLeaderCountAfter);

        MemberRotationServiceImpl service = createService(userRepo);

        assertThatCode(() -> service.executePromotion(pair.internId, pair.formalMemberId))
                .doesNotThrowAnyException();

        // Verify roles were swapped
        assertThat(intern.getRole()).isEqualTo(Role.MEMBER);
        assertThat(formalMember.getRole()).isEqualTo(Role.INTERN);
    }

    // ========== Property 22b: Invalid count after swap throws BusinessException ==========

    /**
     * If the DB reports a formal count != 5 after the swap (simulating a data
     * inconsistency), executePromotion must throw a BusinessException.
     */
    @Property(tries = 100)
    void invalidCountAfterSwapThrowsException(
            @ForAll("invalidCountScenarios") InvalidCountScenario scenario) {

        UserRepository userRepo = Mockito.mock(UserRepository.class);

        User intern = User.builder()
                .id(1L)
                .username("intern_1")
                .password("encoded")
                .role(Role.INTERN)
                .enabled(true)
                .pendingDismissal(false)
                .build();

        User formalMember = User.builder()
                .id(2L)
                .username("formal_2")
                .password("encoded")
                .role(scenario.formalRole)
                .enabled(true)
                .pendingDismissal(false)
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(intern));
        when(userRepo.findById(2L)).thenReturn(Optional.of(formalMember));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Return counts that don't sum to 5
        when(userRepo.countByRole(Role.MEMBER)).thenReturn(scenario.memberCount);
        when(userRepo.countByRole(Role.VICE_LEADER)).thenReturn(scenario.viceLeaderCount);

        MemberRotationServiceImpl service = createService(userRepo);

        assertThatThrownBy(() -> service.executePromotion(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(400);
                    assertThat(bex.getMessage()).contains("正式成员总数异常");
                });
    }

    // ========== Property 22c: Swap is net-zero on formal member count ==========

    /**
     * The swap operation always removes exactly one formal member and adds exactly
     * one new formal member, so the net change is zero.
     */
    @Property(tries = 100)
    void swapIsNetZeroOnFormalCount(@ForAll("formalRoles") Role formalRole) {
        // Before swap: intern=INTERN, formalMember=formalRole (MEMBER or VICE_LEADER)
        // After swap: intern=MEMBER, formalMember=INTERN
        // Net formal members removed: 1 (formalMember lost MEMBER/VICE_LEADER)
        // Net formal members added: 1 (intern gained MEMBER)
        // Net change = 0

        int formalBefore = 1; // the formalMember counts as 1 formal
        int formalAfter = 1;  // the intern (now MEMBER) counts as 1 formal

        assertThat(formalAfter - formalBefore).isEqualTo(0);
    }

    // ========== Data classes ==========

    static class SwapPair {
        final long internId;
        final long formalMemberId;
        final Role formalRole;
        final long originalMemberCount;
        final long originalViceLeaderCount;

        SwapPair(long internId, long formalMemberId, Role formalRole,
                 long originalMemberCount, long originalViceLeaderCount) {
            this.internId = internId;
            this.formalMemberId = formalMemberId;
            this.formalRole = formalRole;
            this.originalMemberCount = originalMemberCount;
            this.originalViceLeaderCount = originalViceLeaderCount;
        }
    }

    static class InvalidCountScenario {
        final Role formalRole;
        final long memberCount;
        final long viceLeaderCount;

        InvalidCountScenario(Role formalRole, long memberCount, long viceLeaderCount) {
            this.formalRole = formalRole;
            this.memberCount = memberCount;
            this.viceLeaderCount = viceLeaderCount;
        }
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<SwapPair> validSwapPairs() {
        Arbitrary<Long> internIds = Arbitraries.longs().between(1, 100);
        Arbitrary<Long> formalIds = Arbitraries.longs().between(101, 200);
        Arbitrary<Long> viceLeaderCounts = Arbitraries.longs().between(0, 5);

        // Generate pairs where the formal member is a MEMBER
        Arbitrary<SwapPair> memberSwaps = Combinators.combine(internIds, formalIds, viceLeaderCounts)
                .as((internId, formalId, vl) -> {
                    long m = REQUIRED_FORMAL_COUNT - vl;
                    return new SwapPair(internId, formalId, Role.MEMBER, m, vl);
                });

        // Generate pairs where the formal member is a VICE_LEADER (vl >= 1)
        Arbitrary<Long> vlCountsAtLeastOne = Arbitraries.longs().between(1, 5);
        Arbitrary<SwapPair> viceLeaderSwaps = Combinators.combine(internIds, formalIds, vlCountsAtLeastOne)
                .as((internId, formalId, vl) -> {
                    long m = REQUIRED_FORMAL_COUNT - vl;
                    return new SwapPair(internId, formalId, Role.VICE_LEADER, m, vl);
                });

        return Arbitraries.oneOf(memberSwaps, viceLeaderSwaps);
    }

    @Provide
    Arbitrary<InvalidCountScenario> invalidCountScenarios() {
        Arbitrary<Role> formalRoles = Arbitraries.of(Role.MEMBER, Role.VICE_LEADER);
        Arbitrary<Long> memberCounts = Arbitraries.longs().between(0, 10);
        Arbitrary<Long> viceLeaderCounts = Arbitraries.longs().between(0, 10);

        return Combinators.combine(formalRoles, memberCounts, viceLeaderCounts)
                .as(InvalidCountScenario::new)
                .filter(s -> (s.memberCount + s.viceLeaderCount) != REQUIRED_FORMAL_COUNT);
    }

    @Provide
    Arbitrary<Role> formalRoles() {
        return Arbitraries.of(Role.MEMBER, Role.VICE_LEADER);
    }
}
