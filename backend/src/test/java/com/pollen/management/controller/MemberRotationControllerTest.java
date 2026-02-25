package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.ExecutePromotionRequest;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.service.MemberRotationService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberRotationControllerTest {

    @Mock
    private MemberRotationService memberRotationService;

    @InjectMocks
    private MemberRotationController controller;

    // --- POST /api/member-rotation/check-promotion ---

    @Test
    void checkPromotionEligibility_shouldReturnEligibleInterns() {
        List<User> eligible = List.of(
                User.builder().id(1L).username("intern1").role(Role.INTERN).build(),
                User.builder().id(2L).username("intern2").role(Role.INTERN).build()
        );
        when(memberRotationService.checkPromotionEligibility()).thenReturn(eligible);

        ApiResponse<List<User>> response = controller.checkPromotionEligibility();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getUsername()).isEqualTo("intern1");
        verify(memberRotationService).checkPromotionEligibility();
    }

    @Test
    void checkPromotionEligibility_noEligible_shouldReturnEmptyList() {
        when(memberRotationService.checkPromotionEligibility()).thenReturn(Collections.emptyList());

        ApiResponse<List<User>> response = controller.checkPromotionEligibility();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- POST /api/member-rotation/check-demotion ---

    @Test
    void checkDemotionCandidates_shouldReturnCandidates() {
        List<User> candidates = List.of(
                User.builder().id(10L).username("member1").role(Role.MEMBER).build()
        );
        when(memberRotationService.checkDemotionCandidates()).thenReturn(candidates);

        ApiResponse<List<User>> response = controller.checkDemotionCandidates();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getRole()).isEqualTo(Role.MEMBER);
        verify(memberRotationService).checkDemotionCandidates();
    }

    @Test
    void checkDemotionCandidates_noCandidates_shouldReturnEmptyList() {
        when(memberRotationService.checkDemotionCandidates()).thenReturn(Collections.emptyList());

        ApiResponse<List<User>> response = controller.checkDemotionCandidates();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- POST /api/member-rotation/trigger-review ---

    @Test
    void triggerPromotionReview_conditionsMet_shouldReturnTrue() {
        when(memberRotationService.triggerPromotionReview()).thenReturn(true);

        ApiResponse<Boolean> response = controller.triggerPromotionReview();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isTrue();
        verify(memberRotationService).triggerPromotionReview();
    }

    @Test
    void triggerPromotionReview_conditionsNotMet_shouldReturnFalse() {
        when(memberRotationService.triggerPromotionReview()).thenReturn(false);

        ApiResponse<Boolean> response = controller.triggerPromotionReview();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isFalse();
    }

    // --- POST /api/member-rotation/execute ---

    @Test
    void executePromotion_validRequest_shouldReturnSuccess() {
        ExecutePromotionRequest request = ExecutePromotionRequest.builder()
                .internId(1L).memberId(10L).build();
        doNothing().when(memberRotationService).executePromotion(1L, 10L);

        ApiResponse<Void> response = controller.executePromotion(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        verify(memberRotationService).executePromotion(1L, 10L);
    }

    @Test
    void executePromotion_internNotFound_shouldPropagateException() {
        ExecutePromotionRequest request = ExecutePromotionRequest.builder()
                .internId(99L).memberId(10L).build();
        doThrow(new BusinessException(404, "实习成员不存在"))
                .when(memberRotationService).executePromotion(99L, 10L);

        assertThatThrownBy(() -> controller.executePromotion(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("实习成员不存在");
    }

    @Test
    void executePromotion_notInternRole_shouldPropagateException() {
        ExecutePromotionRequest request = ExecutePromotionRequest.builder()
                .internId(5L).memberId(10L).build();
        doThrow(new BusinessException(400, "该用户不是实习成员，无法执行转正"))
                .when(memberRotationService).executePromotion(5L, 10L);

        assertThatThrownBy(() -> controller.executePromotion(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该用户不是实习成员，无法执行转正");
    }

    @Test
    void executePromotion_formalMemberCountViolation_shouldPropagateException() {
        ExecutePromotionRequest request = ExecutePromotionRequest.builder()
                .internId(1L).memberId(10L).build();
        doThrow(new BusinessException(400, "角色流转后正式成员总数异常，当前 4 人，要求 5 人"))
                .when(memberRotationService).executePromotion(1L, 10L);

        assertThatThrownBy(() -> controller.executePromotion(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("角色流转后正式成员总数异常，当前 4 人，要求 5 人");
    }

    // --- POST /api/member-rotation/mark-dismissal ---

    @Test
    void markForDismissal_shouldReturnMarkedUsers() {
        List<User> marked = List.of(
                User.builder().id(3L).username("intern3").role(Role.INTERN).pendingDismissal(true).build()
        );
        when(memberRotationService.markForDismissal()).thenReturn(marked);

        ApiResponse<List<User>> response = controller.markForDismissal();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getPendingDismissal()).isTrue();
        verify(memberRotationService).markForDismissal();
    }

    @Test
    void markForDismissal_noOneToMark_shouldReturnEmptyList() {
        when(memberRotationService.markForDismissal()).thenReturn(Collections.emptyList());

        ApiResponse<List<User>> response = controller.markForDismissal();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- GET /api/member-rotation/pending-dismissal ---

    @Test
    void getPendingDismissalList_shouldReturnPendingUsers() {
        List<User> pending = List.of(
                User.builder().id(3L).username("intern3").role(Role.INTERN).pendingDismissal(true).build(),
                User.builder().id(4L).username("intern4").role(Role.INTERN).pendingDismissal(true).build()
        );
        when(memberRotationService.getPendingDismissalList()).thenReturn(pending);

        ApiResponse<List<User>> response = controller.getPendingDismissalList();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData()).allMatch(u -> Boolean.TRUE.equals(u.getPendingDismissal()));
        verify(memberRotationService).getPendingDismissalList();
    }

    @Test
    void getPendingDismissalList_noPending_shouldReturnEmptyList() {
        when(memberRotationService.getPendingDismissalList()).thenReturn(Collections.emptyList());

        ApiResponse<List<User>> response = controller.getPendingDismissalList();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }
}
