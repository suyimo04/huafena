package com.pollen.management.service;

import com.pollen.management.dto.RotationThresholds;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.RoleChangeHistory;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberRotationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointsRecordRepository pointsRecordRepository;

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @Mock
    private RoleChangeHistoryRepository roleChangeHistoryRepository;

    @Mock
    private SalaryConfigService salaryConfigService;

    @InjectMocks
    private MemberRotationServiceImpl memberRotationService;

    private static final RotationThresholds DEFAULT_THRESHOLDS = RotationThresholds.builder()
            .promotionPointsThreshold(100)
            .demotionSalaryThreshold(150)
            .demotionConsecutiveMonths(2)
            .dismissalPointsThreshold(100)
            .dismissalConsecutiveMonths(2)
            .build();

    @BeforeEach
    void setUp() {
        lenient().when(salaryConfigService.getRotationThresholds()).thenReturn(DEFAULT_THRESHOLDS);
    }

    // --- checkPromotionEligibility tests ---

    @Test
    void checkPromotionEligibility_shouldReturnInternsWithMonthlyPointsAboveThreshold() {
        User intern1 = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        User intern2 = User.builder().id(2L).username("intern2").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern1, intern2));

        // intern1 has 120 points this month (eligible)
        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(120).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(r1));

        // intern2 has 80 points this month (not eligible)
        PointsRecord r2 = PointsRecord.builder().userId(2L).amount(80).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(2L), any(), any()))
                .thenReturn(List.of(r2));

        List<User> eligible = memberRotationService.checkPromotionEligibility();

        assertEquals(1, eligible.size());
        assertEquals(1L, eligible.get(0).getId());
    }

    @Test
    void checkPromotionEligibility_shouldReturnEmptyWhenNoInternsExist() {
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of());

        List<User> eligible = memberRotationService.checkPromotionEligibility();

        assertTrue(eligible.isEmpty());
    }

    @Test
    void checkPromotionEligibility_shouldReturnEmptyWhenAllInternsBelowThreshold() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(50).build();
        PointsRecord r2 = PointsRecord.builder().userId(1L).amount(30).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(r1, r2));

        List<User> eligible = memberRotationService.checkPromotionEligibility();

        assertTrue(eligible.isEmpty());
    }

    @Test
    void checkPromotionEligibility_shouldIncludeInternWithExactly100Points() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(60).build();
        PointsRecord r2 = PointsRecord.builder().userId(1L).amount(40).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(r1, r2));

        List<User> eligible = memberRotationService.checkPromotionEligibility();

        assertEquals(1, eligible.size());
    }

    @Test
    void checkPromotionEligibility_shouldHandleMultiplePointsRecordsPerIntern() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        // Multiple small records summing to 105
        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(30).build();
        PointsRecord r2 = PointsRecord.builder().userId(1L).amount(25).build();
        PointsRecord r3 = PointsRecord.builder().userId(1L).amount(50).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(r1, r2, r3));

        List<User> eligible = memberRotationService.checkPromotionEligibility();

        assertEquals(1, eligible.size());
    }

    // --- checkDemotionCandidates tests ---

    @Test
    void checkDemotionCandidates_shouldReturnMembersWithLowSalaryForTwoMonths() {
        User member = User.builder().id(1L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));

        // Two archived records with totalPoints below 150
        SalaryRecord s1 = SalaryRecord.builder().userId(1L).totalPoints(120).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(1)).build();
        SalaryRecord s2 = SalaryRecord.builder().userId(1L).totalPoints(130).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(2)).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(1L))
                .thenReturn(List.of(s1, s2));

        List<User> candidates = memberRotationService.checkDemotionCandidates();

        assertEquals(1, candidates.size());
        assertEquals(1L, candidates.get(0).getId());
    }

    @Test
    void checkDemotionCandidates_shouldReturnEmptyWhenNoFormalMembers() {
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of());

        List<User> candidates = memberRotationService.checkDemotionCandidates();

        assertTrue(candidates.isEmpty());
    }

    @Test
    void checkDemotionCandidates_shouldReturnEmptyWhenMemberHasOnlyOneArchivedRecord() {
        User member = User.builder().id(1L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));

        SalaryRecord s1 = SalaryRecord.builder().userId(1L).totalPoints(100).archived(true).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(1L))
                .thenReturn(List.of(s1));

        List<User> candidates = memberRotationService.checkDemotionCandidates();

        assertTrue(candidates.isEmpty());
    }

    @Test
    void checkDemotionCandidates_shouldNotIncludeMemberWhenOneMonthAboveThreshold() {
        User member = User.builder().id(1L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));

        // First month below, second month above threshold
        SalaryRecord s1 = SalaryRecord.builder().userId(1L).totalPoints(100).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(1)).build();
        SalaryRecord s2 = SalaryRecord.builder().userId(1L).totalPoints(200).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(2)).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(1L))
                .thenReturn(List.of(s1, s2));

        List<User> candidates = memberRotationService.checkDemotionCandidates();

        assertTrue(candidates.isEmpty());
    }

    @Test
    void checkDemotionCandidates_shouldNotIncludeMemberWithExactly150Points() {
        User member = User.builder().id(1L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));

        // Both months at exactly 150 (not below)
        SalaryRecord s1 = SalaryRecord.builder().userId(1L).totalPoints(150).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(1)).build();
        SalaryRecord s2 = SalaryRecord.builder().userId(1L).totalPoints(150).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(2)).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(1L))
                .thenReturn(List.of(s1, s2));

        List<User> candidates = memberRotationService.checkDemotionCandidates();

        assertTrue(candidates.isEmpty());
    }

    @Test
    void checkDemotionCandidates_shouldIncludeViceLeaderWithLowSalary() {
        User viceLeader = User.builder().id(1L).username("vl1").role(Role.VICE_LEADER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(viceLeader));

        SalaryRecord s1 = SalaryRecord.builder().userId(1L).totalPoints(100).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(1)).build();
        SalaryRecord s2 = SalaryRecord.builder().userId(1L).totalPoints(80).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(2)).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(1L))
                .thenReturn(List.of(s1, s2));

        List<User> candidates = memberRotationService.checkDemotionCandidates();

        assertEquals(1, candidates.size());
        assertEquals(Role.VICE_LEADER, candidates.get(0).getRole());
    }

    // --- triggerPromotionReview tests ---

    @Test
    void triggerPromotionReview_shouldReturnTrueWhenBothConditionsMet() {
        // Setup eligible intern
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));
        PointsRecord pr = PointsRecord.builder().userId(1L).amount(120).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(pr));

        // Setup demotion candidate
        User member = User.builder().id(2L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));
        SalaryRecord s1 = SalaryRecord.builder().userId(2L).totalPoints(100).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(1)).build();
        SalaryRecord s2 = SalaryRecord.builder().userId(2L).totalPoints(80).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(2)).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(2L))
                .thenReturn(List.of(s1, s2));

        boolean result = memberRotationService.triggerPromotionReview();

        assertTrue(result);
    }

    @Test
    void triggerPromotionReview_shouldReturnFalseWhenNoEligibleInterns() {
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of());

        // Setup demotion candidate
        User member = User.builder().id(2L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));
        SalaryRecord s1 = SalaryRecord.builder().userId(2L).totalPoints(100).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(1)).build();
        SalaryRecord s2 = SalaryRecord.builder().userId(2L).totalPoints(80).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(2)).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(2L))
                .thenReturn(List.of(s1, s2));

        boolean result = memberRotationService.triggerPromotionReview();

        assertFalse(result);
    }

    @Test
    void triggerPromotionReview_shouldReturnFalseWhenNoDemotionCandidates() {
        // Setup eligible intern
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));
        PointsRecord pr = PointsRecord.builder().userId(1L).amount(120).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(pr));

        // No demotion candidates - all members have good salary
        User member = User.builder().id(2L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));
        SalaryRecord s1 = SalaryRecord.builder().userId(2L).totalPoints(200).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(1)).build();
        SalaryRecord s2 = SalaryRecord.builder().userId(2L).totalPoints(180).archived(true)
                .archivedAt(LocalDateTime.now().minusMonths(2)).build();
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(2L))
                .thenReturn(List.of(s1, s2));

        boolean result = memberRotationService.triggerPromotionReview();

        assertFalse(result);
    }

    @Test
    void triggerPromotionReview_shouldReturnFalseWhenBothConditionsNotMet() {
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of());
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of());

        boolean result = memberRotationService.triggerPromotionReview();

        assertFalse(result);
    }

    // --- Edge cases ---

    @Test
    void checkPromotionEligibility_shouldHandleInternWithNoPointsRecords() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        List<User> eligible = memberRotationService.checkPromotionEligibility();

        assertTrue(eligible.isEmpty());
    }

    @Test
    void checkDemotionCandidates_shouldHandleMemberWithNoArchivedRecords() {
        User member = User.builder().id(1L).username("member1").role(Role.MEMBER).build();
        when(userRepository.findByRoleIn(List.of(Role.MEMBER, Role.VICE_LEADER)))
                .thenReturn(List.of(member));
        when(salaryRecordRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(1L))
                .thenReturn(List.of());

        List<User> candidates = memberRotationService.checkDemotionCandidates();

        assertTrue(candidates.isEmpty());
    }

    @Test
    void checkPromotionEligibility_shouldHandleNegativePointsRecords() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        // Positive and negative records summing to 90 (below threshold)
        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(110).build();
        PointsRecord r2 = PointsRecord.builder().userId(1L).amount(-20).build();
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(r1, r2));

        List<User> eligible = memberRotationService.checkPromotionEligibility();

        assertTrue(eligible.isEmpty());
    }

    // --- executePromotion tests ---

    @Test
    void executePromotion_shouldSwapRolesSuccessfully() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        User member = User.builder().id(2L).username("member1").role(Role.MEMBER).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(userRepository.countByRole(Role.MEMBER)).thenReturn(4L);
        when(userRepository.countByRole(Role.VICE_LEADER)).thenReturn(1L);
        when(roleChangeHistoryRepository.save(any(RoleChangeHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        memberRotationService.executePromotion(1L, 2L);

        assertEquals(Role.MEMBER, intern.getRole());
        assertEquals(Role.INTERN, member.getRole());
        verify(userRepository, times(2)).save(any(User.class));
        // Verify role change history recorded for both users
        verify(roleChangeHistoryRepository, times(2)).save(any(RoleChangeHistory.class));
    }

    @Test
    void executePromotion_shouldRecordRoleChangeHistoryForBothUsers() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        User member = User.builder().id(2L).username("member1").role(Role.MEMBER).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(userRepository.countByRole(Role.MEMBER)).thenReturn(4L);
        when(userRepository.countByRole(Role.VICE_LEADER)).thenReturn(1L);
        when(roleChangeHistoryRepository.save(any(RoleChangeHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        memberRotationService.executePromotion(1L, 2L);

        ArgumentCaptor<RoleChangeHistory> captor = ArgumentCaptor.forClass(RoleChangeHistory.class);
        verify(roleChangeHistoryRepository, times(2)).save(captor.capture());

        List<RoleChangeHistory> histories = captor.getAllValues();
        // Intern promoted: INTERN -> MEMBER
        RoleChangeHistory internHistory = histories.stream()
                .filter(h -> h.getUserId().equals(1L)).findFirst().orElseThrow();
        assertEquals(Role.INTERN, internHistory.getOldRole());
        assertEquals(Role.MEMBER, internHistory.getNewRole());
        assertEquals("system", internHistory.getChangedBy());

        // Formal member demoted: MEMBER -> INTERN
        RoleChangeHistory memberHistory = histories.stream()
                .filter(h -> h.getUserId().equals(2L)).findFirst().orElseThrow();
        assertEquals(Role.MEMBER, memberHistory.getOldRole());
        assertEquals(Role.INTERN, memberHistory.getNewRole());
        assertEquals("system", memberHistory.getChangedBy());
    }

    @Test
    void executePromotion_shouldSwapWithViceLeader() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        User viceLeader = User.builder().id(2L).username("vl1").role(Role.VICE_LEADER).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(userRepository.findById(2L)).thenReturn(Optional.of(viceLeader));
        when(userRepository.countByRole(Role.MEMBER)).thenReturn(5L);
        when(userRepository.countByRole(Role.VICE_LEADER)).thenReturn(0L);
        when(roleChangeHistoryRepository.save(any(RoleChangeHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        memberRotationService.executePromotion(1L, 2L);

        assertEquals(Role.MEMBER, intern.getRole());
        assertEquals(Role.INTERN, viceLeader.getRole());
        verify(roleChangeHistoryRepository, times(2)).save(any(RoleChangeHistory.class));
    }

    @Test
    void executePromotion_shouldThrowWhenInternNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberRotationService.executePromotion(1L, 2L));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("实习成员不存在"));
    }

    @Test
    void executePromotion_shouldThrowWhenFormalMemberNotFound() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberRotationService.executePromotion(1L, 2L));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("正式成员不存在"));
    }

    @Test
    void executePromotion_shouldThrowWhenUserIsNotIntern() {
        User notIntern = User.builder().id(1L).username("member1").role(Role.MEMBER).build();
        User member = User.builder().id(2L).username("member2").role(Role.MEMBER).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(notIntern));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberRotationService.executePromotion(1L, 2L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不是实习成员"));
    }

    @Test
    void executePromotion_shouldThrowWhenTargetIsNotFormalMember() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        User notFormal = User.builder().id(2L).username("intern2").role(Role.INTERN).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(userRepository.findById(2L)).thenReturn(Optional.of(notFormal));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberRotationService.executePromotion(1L, 2L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不是正式成员"));
    }

    @Test
    void executePromotion_shouldThrowWhenFormalCountNotFiveAfterSwap() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).build();
        User member = User.builder().id(2L).username("member1").role(Role.MEMBER).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        // Simulate abnormal count after swap
        when(userRepository.countByRole(Role.MEMBER)).thenReturn(3L);
        when(userRepository.countByRole(Role.VICE_LEADER)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberRotationService.executePromotion(1L, 2L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("正式成员总数异常"));
    }

    // --- markForDismissal tests ---

    @Test
    void markForDismissal_shouldMarkInternWithTwoConsecutiveMonthsBelowThreshold() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).pendingDismissal(false).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        // Last month: 80 points (below 100)
        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(80).build();
        // Two months ago: 70 points (below 100)
        PointsRecord r2 = PointsRecord.builder().userId(1L).amount(70).build();

        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        YearMonth twoMonthsAgo = YearMonth.now().minusMonths(2);

        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(
                eq(1L),
                eq(lastMonth.atDay(1).atStartOfDay()),
                eq(lastMonth.atEndOfMonth().atTime(23, 59, 59))))
                .thenReturn(List.of(r1));
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(
                eq(1L),
                eq(twoMonthsAgo.atDay(1).atStartOfDay()),
                eq(twoMonthsAgo.atEndOfMonth().atTime(23, 59, 59))))
                .thenReturn(List.of(r2));

        List<User> marked = memberRotationService.markForDismissal();

        assertEquals(1, marked.size());
        assertTrue(marked.get(0).getPendingDismissal());
        verify(userRepository).save(intern);
    }

    @Test
    void markForDismissal_shouldNotMarkInternWhenOneMonthAboveThreshold() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).pendingDismissal(false).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(120).build();

        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(
                eq(1L),
                eq(lastMonth.atDay(1).atStartOfDay()),
                eq(lastMonth.atEndOfMonth().atTime(23, 59, 59))))
                .thenReturn(List.of(r1));

        List<User> marked = memberRotationService.markForDismissal();

        assertTrue(marked.isEmpty());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void markForDismissal_shouldNotMarkWhenBothMonthsAboveThreshold() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).pendingDismissal(false).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(150).build();

        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(
                eq(1L),
                eq(lastMonth.atDay(1).atStartOfDay()),
                eq(lastMonth.atEndOfMonth().atTime(23, 59, 59))))
                .thenReturn(List.of(r1));

        List<User> marked = memberRotationService.markForDismissal();

        assertTrue(marked.isEmpty());
    }

    @Test
    void markForDismissal_shouldReturnEmptyWhenNoInterns() {
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of());

        List<User> marked = memberRotationService.markForDismissal();

        assertTrue(marked.isEmpty());
    }

    @Test
    void markForDismissal_shouldNotMarkWhenExactly100PointsBothMonths() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).pendingDismissal(false).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        PointsRecord r1 = PointsRecord.builder().userId(1L).amount(100).build();

        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(
                eq(1L),
                eq(lastMonth.atDay(1).atStartOfDay()),
                eq(lastMonth.atEndOfMonth().atTime(23, 59, 59))))
                .thenReturn(List.of(r1));

        List<User> marked = memberRotationService.markForDismissal();

        assertTrue(marked.isEmpty());
    }

    @Test
    void markForDismissal_shouldHandleInternWithNoPointsRecords() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).pendingDismissal(false).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        YearMonth twoMonthsAgo = YearMonth.now().minusMonths(2);

        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(
                eq(1L),
                eq(lastMonth.atDay(1).atStartOfDay()),
                eq(lastMonth.atEndOfMonth().atTime(23, 59, 59))))
                .thenReturn(List.of());
        when(pointsRecordRepository.findByUserIdAndCreatedAtBetween(
                eq(1L),
                eq(twoMonthsAgo.atDay(1).atStartOfDay()),
                eq(twoMonthsAgo.atEndOfMonth().atTime(23, 59, 59))))
                .thenReturn(List.of());

        // 0 points for both months, both below 100 → should be marked
        List<User> marked = memberRotationService.markForDismissal();

        assertEquals(1, marked.size());
        assertTrue(marked.get(0).getPendingDismissal());
    }

    // --- getPendingDismissalList tests ---

    @Test
    void getPendingDismissalList_shouldReturnOnlyMarkedInterns() {
        User marked = User.builder().id(1L).username("intern1").role(Role.INTERN).pendingDismissal(true).build();
        User notMarked = User.builder().id(2L).username("intern2").role(Role.INTERN).pendingDismissal(false).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(marked, notMarked));

        List<User> result = memberRotationService.getPendingDismissalList();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getPendingDismissalList_shouldReturnEmptyWhenNoMarkedInterns() {
        User intern = User.builder().id(1L).username("intern1").role(Role.INTERN).pendingDismissal(false).build();
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of(intern));

        List<User> result = memberRotationService.getPendingDismissalList();

        assertTrue(result.isEmpty());
    }

    @Test
    void getPendingDismissalList_shouldReturnEmptyWhenNoInterns() {
        when(userRepository.findByRole(Role.INTERN)).thenReturn(List.of());

        List<User> result = memberRotationService.getPendingDismissalList();

        assertTrue(result.isEmpty());
    }
}
