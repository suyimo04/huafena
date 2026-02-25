package com.pollen.management.service;

import com.pollen.management.dto.DashboardStatsDTO;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private PointsRecordRepository pointsRecordRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getDashboardStats_shouldReturnCorrectCounts() {
        when(userRepository.count()).thenReturn(20L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(Role.LEADER)).thenReturn(1L);
        when(userRepository.countByRole(Role.VICE_LEADER)).thenReturn(1L);
        when(userRepository.countByRole(Role.MEMBER)).thenReturn(4L);
        when(userRepository.countByRole(Role.INTERN)).thenReturn(3L);
        when(userRepository.countByRole(Role.APPLICANT)).thenReturn(10L);
        when(activityRepository.count()).thenReturn(5L);
        when(pointsRecordRepository.count()).thenReturn(100L);

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertEquals(20L, stats.getTotalMembers());
        assertEquals(1L, stats.getAdminCount());
        assertEquals(1L, stats.getLeaderCount());
        assertEquals(1L, stats.getViceLeaderCount());
        assertEquals(4L, stats.getMemberCount());
        assertEquals(3L, stats.getInternCount());
        assertEquals(10L, stats.getApplicantCount());
        assertEquals(5L, stats.getTotalActivities());
        assertEquals(100L, stats.getTotalPointsRecords());
    }

    @Test
    void getDashboardStats_shouldReturnZerosWhenEmpty() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(any(Role.class))).thenReturn(0L);
        when(activityRepository.count()).thenReturn(0L);
        when(pointsRecordRepository.count()).thenReturn(0L);

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertEquals(0L, stats.getTotalMembers());
        assertEquals(0L, stats.getAdminCount());
        assertEquals(0L, stats.getLeaderCount());
        assertEquals(0L, stats.getViceLeaderCount());
        assertEquals(0L, stats.getMemberCount());
        assertEquals(0L, stats.getInternCount());
        assertEquals(0L, stats.getApplicantCount());
        assertEquals(0L, stats.getTotalActivities());
        assertEquals(0L, stats.getTotalPointsRecords());
    }

    @Test
    void getDashboardStats_shouldSumRoleCountsToTotalMembers() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(Role.LEADER)).thenReturn(1L);
        when(userRepository.countByRole(Role.VICE_LEADER)).thenReturn(1L);
        when(userRepository.countByRole(Role.MEMBER)).thenReturn(4L);
        when(userRepository.countByRole(Role.INTERN)).thenReturn(2L);
        when(userRepository.countByRole(Role.APPLICANT)).thenReturn(1L);
        when(activityRepository.count()).thenReturn(3L);
        when(pointsRecordRepository.count()).thenReturn(50L);

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        long roleSum = stats.getAdminCount() + stats.getLeaderCount()
                + stats.getViceLeaderCount() + stats.getMemberCount()
                + stats.getInternCount() + stats.getApplicantCount();
        assertEquals(stats.getTotalMembers(), roleSum);
    }

    @Test
    void getDashboardStats_shouldCallAllRepositories() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(any(Role.class))).thenReturn(0L);
        when(activityRepository.count()).thenReturn(0L);
        when(pointsRecordRepository.count()).thenReturn(0L);

        dashboardService.getDashboardStats();

        verify(userRepository).count();
        verify(userRepository).countByRole(Role.ADMIN);
        verify(userRepository).countByRole(Role.LEADER);
        verify(userRepository).countByRole(Role.VICE_LEADER);
        verify(userRepository).countByRole(Role.MEMBER);
        verify(userRepository).countByRole(Role.INTERN);
        verify(userRepository).countByRole(Role.APPLICANT);
        verify(activityRepository).count();
        verify(pointsRecordRepository).count();
    }
}
