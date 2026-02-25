package com.pollen.management.service;

import com.pollen.management.dto.DashboardStatsDTO;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final PointsRecordRepository pointsRecordRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        return DashboardStatsDTO.builder()
                .totalMembers(userRepository.count())
                .adminCount(userRepository.countByRole(Role.ADMIN))
                .leaderCount(userRepository.countByRole(Role.LEADER))
                .viceLeaderCount(userRepository.countByRole(Role.VICE_LEADER))
                .memberCount(userRepository.countByRole(Role.MEMBER))
                .internCount(userRepository.countByRole(Role.INTERN))
                .applicantCount(userRepository.countByRole(Role.APPLICANT))
                .totalActivities(activityRepository.count())
                .totalPointsRecords(pointsRecordRepository.count())
                .build();
    }
}
