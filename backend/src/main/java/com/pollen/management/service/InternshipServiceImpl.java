package com.pollen.management.service;

import com.pollen.management.dto.CreateInternshipTaskRequest;
import com.pollen.management.dto.InternshipProgress;
import com.pollen.management.entity.Internship;
import com.pollen.management.entity.InternshipTask;
import com.pollen.management.entity.RoleChangeHistory;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.InternshipStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.InternshipRepository;
import com.pollen.management.repository.InternshipTaskRepository;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternshipServiceImpl implements InternshipService {

    private final InternshipRepository internshipRepository;
    private final InternshipTaskRepository internshipTaskRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final EmailService emailService;
    private final RoleChangeHistoryRepository roleChangeHistoryRepository;

    @Override
    @Transactional
    public Internship createForNewIntern(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // Check if user already has an active internship
        internshipRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.getStatus() == InternshipStatus.IN_PROGRESS) {
                throw new BusinessException(409, "该用户已有进行中的实习记录");
            }
        });

        LocalDate now = LocalDate.now();
        Internship internship = Internship.builder()
                .userId(userId)
                .startDate(now)
                .expectedEndDate(now.plusDays(30))
                .status(InternshipStatus.IN_PROGRESS)
                .build();

        return internshipRepository.save(internship);
    }

    @Override
    public Internship getById(Long id) {
        return internshipRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "实习记录不存在"));
    }

    @Override
    @Transactional
    public InternshipTask createTask(Long internshipId, CreateInternshipTaskRequest request) {
        Internship internship = getById(internshipId);

        if (internship.getStatus() != InternshipStatus.IN_PROGRESS) {
            throw new BusinessException(400, "只能为进行中的实习创建任务");
        }

        InternshipTask task = InternshipTask.builder()
                .internshipId(internshipId)
                .taskName(request.getTaskName())
                .taskDescription(request.getTaskDescription())
                .deadline(request.getDeadline())
                .completed(false)
                .build();

        return internshipTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void completeTask(Long internshipId, Long taskId) {
        getById(internshipId); // validate internship exists

        InternshipTask task = internshipTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "任务不存在"));

        if (!task.getInternshipId().equals(internshipId)) {
            throw new BusinessException(400, "任务不属于该实习记录");
        }

        if (task.getCompleted()) {
            throw new BusinessException(400, "任务已完成");
        }

        task.setCompleted(true);
        task.setCompletedAt(LocalDateTime.now());
        internshipTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void assignMentor(Long internshipId, Long mentorId) {
        Internship internship = getById(internshipId);

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new BusinessException(404, "导师用户不存在"));

        if (mentor.getRole() != Role.MEMBER && mentor.getRole() != Role.VICE_LEADER) {
            throw new BusinessException(400, "导师必须是正式成员（MEMBER）或副组长（VICE_LEADER）");
        }

        internship.setMentorId(mentorId);
        internshipRepository.save(internship);
    }

    @Override
    public InternshipProgress getProgress(Long internshipId) {
        Internship internship = getById(internshipId);

        List<InternshipTask> tasks = internshipTaskRepository.findByInternshipId(internshipId);
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(InternshipTask::getCompleted).count();

        double taskCompletionRate = totalTasks == 0 ? 0.0 : (double) completedTasks / totalTasks;

        int totalPoints = pointsService.getTotalPoints(internship.getUserId());

        // Calculate remaining days
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), internship.getExpectedEndDate());
        if (remainingDays < 0) {
            remainingDays = 0;
        }

        // Mentor comment placeholder - could be extended with a mentor evaluation entity
        String mentorComment = null;

        return InternshipProgress.builder()
                .taskCompletionRate(taskCompletionRate)
                .totalPoints(totalPoints)
                .mentorComment(mentorComment)
                .remainingDays((int) remainingDays)
                .tasks(tasks)
                .build();
    }

    @Override
    @Transactional
    public void approveConversion(Long internshipId) {
        Internship internship = getById(internshipId);

        if (internship.getStatus() != InternshipStatus.PENDING_CONVERSION) {
            throw new BusinessException(400, "只有待转正状态的实习记录才能批准转正");
        }

        User user = userRepository.findById(internship.getUserId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Role oldRole = user.getRole();
        user.setRole(Role.MEMBER);
        userRepository.save(user);

        // 记录角色变更历史
        roleChangeHistoryRepository.save(RoleChangeHistory.builder()
                .userId(user.getId())
                .oldRole(oldRole)
                .newRole(Role.MEMBER)
                .changedBy("system")
                .build());

        internship.setStatus(InternshipStatus.CONVERTED);
        internshipRepository.save(internship);

        try {
            emailService.sendTemplateEmail(
                    "CONVERSION_NOTIFICATION",
                    Map.of("memberName", user.getUsername()),
                    user.getUsername()
            );
        } catch (Exception e) {
            log.warn("转正邮件通知发送失败: userId={}, error={}", user.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void extendInternship(Long internshipId, int additionalDays) {
        Internship internship = getById(internshipId);

        if (internship.getStatus() != InternshipStatus.IN_PROGRESS
                && internship.getStatus() != InternshipStatus.PENDING_EVALUATION) {
            throw new BusinessException(400, "只有进行中或待评估状态的实习记录才能延期");
        }

        if (additionalDays <= 0) {
            throw new BusinessException(400, "延期天数必须大于 0");
        }

        internship.setExpectedEndDate(internship.getExpectedEndDate().plusDays(additionalDays));
        internship.setStatus(InternshipStatus.IN_PROGRESS);
        internshipRepository.save(internship);
    }

    @Override
    @Transactional
    public void terminateInternship(Long internshipId) {
        Internship internship = getById(internshipId);

        if (internship.getStatus() != InternshipStatus.IN_PROGRESS
                && internship.getStatus() != InternshipStatus.PENDING_EVALUATION) {
            throw new BusinessException(400, "只有进行中或待评估状态的实习记录才能终止");
        }

        internship.setStatus(InternshipStatus.TERMINATED);
        internshipRepository.save(internship);
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkAndTriggerConversion() {
        List<Internship> inProgressList = internshipRepository.findByStatus(InternshipStatus.IN_PROGRESS);
        LocalDate today = LocalDate.now();

        for (Internship internship : inProgressList) {
            if (!internship.getExpectedEndDate().isAfter(today)) {
                List<InternshipTask> tasks = internshipTaskRepository.findByInternshipId(internship.getId());
                long totalTasks = tasks.size();
                long completedTasks = tasks.stream().filter(InternshipTask::getCompleted).count();
                double completionRate = totalTasks == 0 ? 0.0 : (double) completedTasks / totalTasks;

                if (completionRate >= 0.8) {
                    internship.setStatus(InternshipStatus.PENDING_CONVERSION);
                } else {
                    internship.setStatus(InternshipStatus.PENDING_EVALUATION);
                }
                internshipRepository.save(internship);
                log.info("实习期满检查: internshipId={}, completionRate={}, newStatus={}",
                        internship.getId(), completionRate, internship.getStatus());
            }
        }
    }
}
