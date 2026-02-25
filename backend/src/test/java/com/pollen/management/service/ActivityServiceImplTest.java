package com.pollen.management.service;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityGroup;
import com.pollen.management.entity.ActivityRegistration;
import com.pollen.management.entity.enums.ActivityStatus;
import com.pollen.management.entity.enums.ApprovalMode;
import com.pollen.management.entity.enums.PointsType;
import com.pollen.management.entity.enums.RegistrationStatus;
import com.pollen.management.repository.ActivityGroupRepository;
import com.pollen.management.repository.ActivityRegistrationRepository;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.util.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ActivityGroupRepository activityGroupRepository;

    @Mock
    private PointsService pointsService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ActivityServiceImpl activityService;

    // --- createActivity tests ---

    @Test
    void createActivity_shouldCreateWithCorrectFields() {
        LocalDateTime eventTime = LocalDateTime.of(2025, 8, 1, 14, 0);
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
            Activity a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        Activity result = activityService.createActivity("团建活动", "年度团建", eventTime, "公园", 10L);

        assertEquals("团建活动", result.getName());
        assertEquals("年度团建", result.getDescription());
        assertEquals(eventTime, result.getActivityTime());
        assertEquals("公园", result.getLocation());
        assertEquals(10L, result.getCreatedBy());
        assertEquals(ActivityStatus.UPCOMING, result.getStatus());
        assertEquals(0, result.getRegistrationCount());
    }

    // --- registerForActivity tests ---

    @Test
    void registerForActivity_shouldRecordRegistrationAndUpdateCount() {
        Activity activity = Activity.builder().id(1L).name("活动").status(ActivityStatus.UPCOMING).registrationCount(0).build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.existsByActivityIdAndUserId(1L, 2L)).thenReturn(false);
        when(registrationRepository.save(any(ActivityRegistration.class))).thenAnswer(inv -> {
            ActivityRegistration r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityRegistration result = activityService.registerForActivity(1L, 2L);

        assertEquals(1L, result.getActivityId());
        assertEquals(2L, result.getUserId());
        assertFalse(result.getCheckedIn());
        assertEquals(1, activity.getRegistrationCount());
    }

    @Test
    void registerForActivity_shouldRejectDuplicateRegistration() {
        Activity activity = Activity.builder().id(1L).name("活动").status(ActivityStatus.UPCOMING).registrationCount(1).build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.existsByActivityIdAndUserId(1L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.registerForActivity(1L, 2L));
        assertEquals(409, ex.getCode());
        assertTrue(ex.getMessage().contains("重复报名"));
    }

    @Test
    void registerForActivity_shouldRejectForArchivedActivity() {
        Activity activity = Activity.builder().id(1L).name("活动").status(ActivityStatus.ARCHIVED).registrationCount(0).build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.registerForActivity(1L, 2L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("归档"));
    }

    @Test
    void registerForActivity_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.registerForActivity(99L, 2L));
        assertEquals(404, ex.getCode());
    }

    // --- checkIn tests ---

    @Test
    void checkIn_shouldRecordTimeAndAwardPoints() {
        Activity activity = Activity.builder().id(1L).name("活动").status(ActivityStatus.ONGOING).build();
        ActivityRegistration reg = ActivityRegistration.builder()
                .id(1L).activityId(1L).userId(2L).checkedIn(false).build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findByActivityIdAndUserId(1L, 2L)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(any(ActivityRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityRegistration result = activityService.checkIn(1L, 2L);

        assertTrue(result.getCheckedIn());
        assertNotNull(result.getCheckedInAt());
        verify(pointsService).addPoints(2L, PointsType.CHECKIN, 5, "活动签到奖励");
    }

    @Test
    void checkIn_shouldRejectUnregisteredMember() {
        Activity activity = Activity.builder().id(1L).name("活动").status(ActivityStatus.ONGOING).build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findByActivityIdAndUserId(1L, 3L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.checkIn(1L, 3L));
        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("未报名"));
    }

    @Test
    void checkIn_shouldRejectDuplicateCheckIn() {
        Activity activity = Activity.builder().id(1L).name("活动").status(ActivityStatus.ONGOING).build();
        ActivityRegistration reg = ActivityRegistration.builder()
                .id(1L).activityId(1L).userId(2L).checkedIn(true).checkedInAt(LocalDateTime.now()).build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findByActivityIdAndUserId(1L, 2L)).thenReturn(Optional.of(reg));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.checkIn(1L, 2L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已签到"));
    }

    @Test
    void checkIn_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.checkIn(99L, 2L));
        assertEquals(404, ex.getCode());
    }

    // --- archiveActivity tests ---

    @Test
    void archiveActivity_shouldSetStatusToArchived() {
        Activity activity = Activity.builder().id(1L).name("活动").status(ActivityStatus.UPCOMING).build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.archiveActivity(1L);

        assertEquals(ActivityStatus.ARCHIVED, result.getStatus());
    }

    @Test
    void archiveActivity_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.archiveActivity(99L));
        assertEquals(404, ex.getCode());
    }

    // --- listActivities tests ---

    @Test
    void listActivities_shouldReturnAllActivities() {
        Activity a1 = Activity.builder().id(1L).name("活动1").build();
        Activity a2 = Activity.builder().id(2L).name("活动2").build();
        when(activityRepository.findAll()).thenReturn(List.of(a1, a2));

        List<Activity> result = activityService.listActivities();

        assertEquals(2, result.size());
    }

    // --- awardActivityPoints tests ---

    @Test
    void awardActivityPoints_shouldAwardValidScore() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        activityService.awardActivityPoints(1L, 2L, 15);

        verify(pointsService).addPoints(2L, PointsType.EVENT_HOSTING, 15, "举办活动积分奖励");
    }

    @Test
    void awardActivityPoints_shouldAwardMinScore() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        activityService.awardActivityPoints(1L, 2L, 5);

        verify(pointsService).addPoints(2L, PointsType.EVENT_HOSTING, 5, "举办活动积分奖励");
    }

    @Test
    void awardActivityPoints_shouldAwardMaxScore() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        activityService.awardActivityPoints(1L, 2L, 25);

        verify(pointsService).addPoints(2L, PointsType.EVENT_HOSTING, 25, "举办活动积分奖励");
    }

    @Test
    void awardActivityPoints_shouldRejectScoreBelowRange() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.awardActivityPoints(1L, 2L, 4));
        assertEquals(400, ex.getCode());
    }

    @Test
    void awardActivityPoints_shouldRejectScoreAboveRange() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.awardActivityPoints(1L, 2L, 26));
        assertEquals(400, ex.getCode());
    }

    @Test
    void awardActivityPoints_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.awardActivityPoints(99L, 2L, 10));
        assertEquals(404, ex.getCode());
    }

    // --- approveRegistration tests ---

    @Test
    void approveRegistration_shouldApproveAndIncrementCount() {
        Activity activity = Activity.builder().id(1L).name("活动")
                .approvalMode(ApprovalMode.MANUAL).registrationCount(0).build();
        ActivityRegistration reg = ActivityRegistration.builder()
                .id(10L).activityId(1L).userId(2L).status(RegistrationStatus.PENDING).build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(any(ActivityRegistration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        activityService.approveRegistration(1L, 10L);

        assertEquals(RegistrationStatus.APPROVED, reg.getStatus());
        assertEquals(1, activity.getRegistrationCount());
    }

    @Test
    void approveRegistration_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.approveRegistration(99L, 10L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void approveRegistration_shouldRejectForNonExistentRegistration() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.approveRegistration(1L, 99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void approveRegistration_shouldRejectIfRegistrationNotBelongToActivity() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        ActivityRegistration reg = ActivityRegistration.builder()
                .id(10L).activityId(2L).userId(3L).status(RegistrationStatus.PENDING).build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(reg));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.approveRegistration(1L, 10L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void approveRegistration_shouldRejectIfNotPendingStatus() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        ActivityRegistration reg = ActivityRegistration.builder()
                .id(10L).activityId(1L).userId(2L).status(RegistrationStatus.APPROVED).build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(reg));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.approveRegistration(1L, 10L));
        assertEquals(400, ex.getCode());
    }

    // --- createGroup tests ---

    @Test
    void createGroup_shouldCreateGroupWithMembers() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityGroupRepository.save(any(ActivityGroup.class))).thenAnswer(inv -> {
            ActivityGroup g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        ActivityGroup result = activityService.createGroup(1L, "A组", List.of(1L, 2L, 3L));

        assertEquals(1L, result.getActivityId());
        assertEquals("A组", result.getGroupName());
        assertNotNull(result.getMemberIds());
        assertTrue(result.getMemberIds().contains("1"));
        assertTrue(result.getMemberIds().contains("2"));
        assertTrue(result.getMemberIds().contains("3"));
    }

    @Test
    void createGroup_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.createGroup(99L, "A组", List.of(1L)));
        assertEquals(404, ex.getCode());
    }

    // --- updateGroupMembers tests ---

    @Test
    void updateGroupMembers_shouldUpdateMemberIds() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        ActivityGroup group = ActivityGroup.builder()
                .id(10L).activityId(1L).groupName("A组").memberIds("[1,2]").build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(activityGroupRepository.save(any(ActivityGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        activityService.updateGroupMembers(1L, 10L, List.of(3L, 4L, 5L));

        assertTrue(group.getMemberIds().contains("3"));
        assertTrue(group.getMemberIds().contains("4"));
        assertTrue(group.getMemberIds().contains("5"));
    }

    @Test
    void updateGroupMembers_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.updateGroupMembers(99L, 10L, List.of(1L)));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateGroupMembers_shouldRejectForNonExistentGroup() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityGroupRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.updateGroupMembers(1L, 99L, List.of(1L)));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateGroupMembers_shouldRejectIfGroupNotBelongToActivity() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        ActivityGroup group = ActivityGroup.builder()
                .id(10L).activityId(2L).groupName("A组").memberIds("[1]").build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityGroupRepository.findById(10L)).thenReturn(Optional.of(group));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.updateGroupMembers(1L, 10L, List.of(1L)));
        assertEquals(400, ex.getCode());
    }

    // --- getGroups tests ---

    @Test
    void getGroups_shouldReturnGroupsForActivity() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        ActivityGroup g1 = ActivityGroup.builder().id(1L).activityId(1L).groupName("A组").memberIds("[1,2]").build();
        ActivityGroup g2 = ActivityGroup.builder().id(2L).activityId(1L).groupName("B组").memberIds("[3,4]").build();

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityGroupRepository.findByActivityId(1L)).thenReturn(List.of(g1, g2));

        List<ActivityGroup> result = activityService.getGroups(1L);

        assertEquals(2, result.size());
        assertEquals("A组", result.get(0).getGroupName());
        assertEquals("B组", result.get(1).getGroupName());
    }

    @Test
    void getGroups_shouldRejectForNonExistentActivity() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.getGroups(99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getGroups_shouldReturnEmptyListWhenNoGroups() {
        Activity activity = Activity.builder().id(1L).name("活动").build();
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityGroupRepository.findByActivityId(1L)).thenReturn(List.of());

        List<ActivityGroup> result = activityService.getGroups(1L);

        assertTrue(result.isEmpty());
    }
}
