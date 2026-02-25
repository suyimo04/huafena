package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.AwardPointsRequest;
import com.pollen.management.dto.CreateActivityRequest;
import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityRegistration;
import com.pollen.management.entity.enums.ActivityStatus;
import com.pollen.management.service.ActivityService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ActivityController controller;

    // --- POST /api/activities (create) ---

    @Test
    void createActivity_shouldDelegateToServiceAndReturnSuccess() {
        LocalDateTime eventTime = LocalDateTime.of(2025, 8, 1, 14, 0);
        var request = CreateActivityRequest.builder()
                .name("团建活动")
                .description("年度团建")
                .eventTime(eventTime)
                .location("公园")
                .createdBy(1L)
                .build();
        var activity = Activity.builder()
                .id(10L)
                .name("团建活动")
                .description("年度团建")
                .activityTime(eventTime)
                .location("公园")
                .createdBy(1L)
                .status(ActivityStatus.UPCOMING)
                .registrationCount(0)
                .build();
        when(activityService.createActivity(any(CreateActivityRequest.class)))
                .thenReturn(activity);

        ApiResponse<Activity> response = controller.createActivity(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(10L);
        assertThat(response.getData().getName()).isEqualTo("团建活动");
        assertThat(response.getData().getLocation()).isEqualTo("公园");
        assertThat(response.getData().getStatus()).isEqualTo(ActivityStatus.UPCOMING);
        assertThat(response.getData().getRegistrationCount()).isEqualTo(0);
        verify(activityService).createActivity(any(CreateActivityRequest.class));
    }

    // --- GET /api/activities (list) ---

    @Test
    void listActivities_shouldReturnAllActivities() {
        var a1 = Activity.builder().id(1L).name("活动A").status(ActivityStatus.UPCOMING).build();
        var a2 = Activity.builder().id(2L).name("活动B").status(ActivityStatus.ARCHIVED).build();
        when(activityService.listActivities()).thenReturn(List.of(a1, a2));

        ApiResponse<List<Activity>> response = controller.listActivities();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getName()).isEqualTo("活动A");
        assertThat(response.getData().get(1).getName()).isEqualTo("活动B");
        verify(activityService).listActivities();
    }

    @Test
    void listActivities_empty_shouldReturnEmptyList() {
        when(activityService.listActivities()).thenReturn(Collections.emptyList());

        ApiResponse<List<Activity>> response = controller.listActivities();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- POST /api/activities/{id}/register ---

    @Test
    void registerForActivity_shouldDelegateToServiceAndReturnSuccess() {
        var registration = ActivityRegistration.builder()
                .id(5L)
                .activityId(1L)
                .userId(2L)
                .checkedIn(false)
                .build();
        when(activityService.registerForActivity(1L, 2L)).thenReturn(registration);

        ApiResponse<ActivityRegistration> response = controller.registerForActivity(1L, 2L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(5L);
        assertThat(response.getData().getActivityId()).isEqualTo(1L);
        assertThat(response.getData().getUserId()).isEqualTo(2L);
        assertThat(response.getData().getCheckedIn()).isFalse();
        verify(activityService).registerForActivity(1L, 2L);
    }

    @Test
    void registerForActivity_activityNotFound_shouldPropagateException() {
        when(activityService.registerForActivity(99L, 1L))
                .thenThrow(new BusinessException(404, "活动不存在"));

        assertThatThrownBy(() -> controller.registerForActivity(99L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动不存在");
    }

    @Test
    void registerForActivity_duplicateRegistration_shouldPropagateException() {
        when(activityService.registerForActivity(1L, 2L))
                .thenThrow(new BusinessException(409, "不可重复报名同一活动"));

        assertThatThrownBy(() -> controller.registerForActivity(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不可重复报名同一活动");
    }

    @Test
    void registerForActivity_archivedActivity_shouldPropagateException() {
        when(activityService.registerForActivity(1L, 2L))
                .thenThrow(new BusinessException(400, "活动已归档，无法报名"));

        assertThatThrownBy(() -> controller.registerForActivity(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动已归档，无法报名");
    }

    // --- POST /api/activities/{id}/check-in ---

    @Test
    void checkIn_shouldDelegateToServiceAndReturnSuccess() {
        var registration = ActivityRegistration.builder()
                .id(5L)
                .activityId(1L)
                .userId(2L)
                .checkedIn(true)
                .checkedInAt(LocalDateTime.now())
                .build();
        when(activityService.checkIn(1L, 2L, null)).thenReturn(registration);

        ApiResponse<ActivityRegistration> response = controller.checkIn(1L, 2L, null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getCheckedIn()).isTrue();
        assertThat(response.getData().getCheckedInAt()).isNotNull();
        verify(activityService).checkIn(1L, 2L, null);
    }

    @Test
    void checkIn_notRegistered_shouldPropagateException() {
        when(activityService.checkIn(1L, 3L, null))
                .thenThrow(new BusinessException(403, "未报名该活动，无法签到"));

        assertThatThrownBy(() -> controller.checkIn(1L, 3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未报名该活动，无法签到");
    }

    @Test
    void checkIn_alreadyCheckedIn_shouldPropagateException() {
        when(activityService.checkIn(1L, 2L, null))
                .thenThrow(new BusinessException(400, "已签到，请勿重复签到"));

        assertThatThrownBy(() -> controller.checkIn(1L, 2L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已签到，请勿重复签到");
    }

    // --- POST /api/activities/{id}/archive ---

    @Test
    void archiveActivity_shouldDelegateToServiceAndReturnSuccess() {
        var activity = Activity.builder()
                .id(1L)
                .name("团建活动")
                .status(ActivityStatus.ARCHIVED)
                .build();
        when(activityService.archiveActivity(1L)).thenReturn(activity);

        ApiResponse<Activity> response = controller.archiveActivity(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getStatus()).isEqualTo(ActivityStatus.ARCHIVED);
        verify(activityService).archiveActivity(1L);
    }

    @Test
    void archiveActivity_notFound_shouldPropagateException() {
        when(activityService.archiveActivity(99L))
                .thenThrow(new BusinessException(404, "活动不存在"));

        assertThatThrownBy(() -> controller.archiveActivity(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动不存在");
    }

    // --- POST /api/activities/{id}/award-points ---

    @Test
    void awardActivityPoints_shouldDelegateToServiceAndReturnSuccess() {
        var request = AwardPointsRequest.builder()
                .userId(2L)
                .score(15)
                .build();
        doNothing().when(activityService).awardActivityPoints(1L, 2L, 15);

        ApiResponse<Void> response = controller.awardActivityPoints(1L, request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(activityService).awardActivityPoints(1L, 2L, 15);
    }

    @Test
    void awardActivityPoints_invalidScore_shouldPropagateException() {
        var request = AwardPointsRequest.builder()
                .userId(2L)
                .score(30)
                .build();
        doThrow(new BusinessException(400, "活动积分奖励范围为 5-25 分"))
                .when(activityService).awardActivityPoints(1L, 2L, 30);

        assertThatThrownBy(() -> controller.awardActivityPoints(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动积分奖励范围为 5-25 分");
    }

    @Test
    void awardActivityPoints_activityNotFound_shouldPropagateException() {
        var request = AwardPointsRequest.builder()
                .userId(2L)
                .score(10)
                .build();
        doThrow(new BusinessException(404, "活动不存在"))
                .when(activityService).awardActivityPoints(99L, 2L, 10);

        assertThatThrownBy(() -> controller.awardActivityPoints(99L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动不存在");
    }
}
