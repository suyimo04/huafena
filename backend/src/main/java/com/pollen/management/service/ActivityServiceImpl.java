package com.pollen.management.service;

import com.pollen.management.dto.CreateActivityRequest;
import com.pollen.management.dto.FeedbackRequest;
import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityFeedback;
import com.pollen.management.entity.ActivityGroup;
import com.pollen.management.entity.ActivityMaterial;
import com.pollen.management.entity.ActivityRegistration;
import com.pollen.management.entity.ActivityStatistics;
import com.pollen.management.entity.enums.ActivityStatus;
import com.pollen.management.entity.enums.ApprovalMode;
import com.pollen.management.entity.enums.PointsType;
import com.pollen.management.entity.enums.RegistrationStatus;
import com.pollen.management.repository.ActivityFeedbackRepository;
import com.pollen.management.repository.ActivityGroupRepository;
import com.pollen.management.repository.ActivityMaterialRepository;
import com.pollen.management.repository.ActivityRegistrationRepository;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.repository.ActivityStatisticsRepository;
import com.pollen.management.util.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final ActivityGroupRepository activityGroupRepository;
    private final ActivityFeedbackRepository activityFeedbackRepository;
    private final ActivityStatisticsRepository activityStatisticsRepository;
    private final ActivityMaterialRepository activityMaterialRepository;
    private final PointsService pointsService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Activity createActivity(String name, String description, LocalDateTime eventTime, String location, Long createdBy) {
        Activity activity = Activity.builder()
                .name(name)
                .description(description)
                .activityTime(eventTime)
                .location(location)
                .createdBy(createdBy)
                .status(ActivityStatus.UPCOMING)
                .registrationCount(0)
                .build();
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public Activity createActivity(CreateActivityRequest request) {
        Activity activity = Activity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .activityTime(request.getEventTime())
                .location(request.getLocation())
                .createdBy(request.getCreatedBy())
                .coverImageUrl(request.getCoverImageUrl())
                .activityType(request.getActivityType())
                .customFormFields(request.getCustomFormFields())
                .approvalMode(request.getApprovalMode() != null ? request.getApprovalMode() : ApprovalMode.AUTO)
                .status(ActivityStatus.UPCOMING)
                .registrationCount(0)
                .build();
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public ActivityRegistration registerForActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        if (activity.getStatus() == ActivityStatus.ARCHIVED) {
            throw new BusinessException(400, "活动已归档，无法报名");
        }

        if (registrationRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new BusinessException(409, "不可重复报名同一活动");
        }

        RegistrationStatus initialStatus = activity.getApprovalMode() == ApprovalMode.MANUAL
                ? RegistrationStatus.PENDING
                : RegistrationStatus.APPROVED;

        ActivityRegistration registration = ActivityRegistration.builder()
                .activityId(activityId)
                .userId(userId)
                .status(initialStatus)
                .checkedIn(false)
                .build();
        registration = registrationRepository.save(registration);

        if (initialStatus == RegistrationStatus.APPROVED) {
            activity.setRegistrationCount(activity.getRegistrationCount() + 1);
            activityRepository.save(activity);
        }

        return registration;
    }

    @Override
    @Transactional
    public ActivityRegistration checkIn(Long activityId, Long userId) {
        return checkIn(activityId, userId, null);
    }

    @Override
    @Transactional
    public ActivityRegistration checkIn(Long activityId, Long userId, String qrToken) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        if (qrToken != null && !qrToken.isEmpty()) {
            if (activity.getQrToken() == null || !activity.getQrToken().equals(qrToken)) {
                throw new BusinessException(400, "二维码无效或已过期");
            }
        }

        ActivityRegistration registration = registrationRepository.findByActivityIdAndUserId(activityId, userId)
                .orElseThrow(() -> new BusinessException(403, "未报名该活动，无法签到"));

        if (registration.getCheckedIn()) {
            throw new BusinessException(400, "已签到，请勿重复签到");
        }

        registration.setCheckedIn(true);
        registration.setCheckedInAt(LocalDateTime.now());
        registration = registrationRepository.save(registration);

        // 发放签到奖励积分
        pointsService.addPoints(userId, PointsType.CHECKIN, 5, "活动签到奖励");

        return registration;
    }

    @Override
    @Transactional
    public Activity archiveActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        activity.setStatus(ActivityStatus.ARCHIVED);
        return activityRepository.save(activity);
    }

    @Override
    public List<Activity> listActivities() {
        return activityRepository.findAll();
    }

    @Override
    @Transactional
    public void awardActivityPoints(Long activityId, Long userId, int score) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        if (score < 5 || score > 25) {
            throw new BusinessException(400, "活动积分奖励范围为 5-25 分");
        }

        pointsService.addPoints(userId, PointsType.EVENT_HOSTING, score, "举办活动积分奖励");
    }

    @Override
    @Transactional
    public void approveRegistration(Long activityId, Long registrationId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        ActivityRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException(404, "报名记录不存在"));

        if (!registration.getActivityId().equals(activityId)) {
            throw new BusinessException(400, "报名记录不属于该活动");
        }

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException(400, "只能审批待审核的报名记录");
        }

        registration.setStatus(RegistrationStatus.APPROVED);
        registrationRepository.save(registration);

        activity.setRegistrationCount(activity.getRegistrationCount() + 1);
        activityRepository.save(activity);
    }

    @Override
    @Transactional
    public ActivityGroup createGroup(Long activityId, String groupName, List<Long> memberIds) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        String memberIdsJson;
        try {
            memberIdsJson = objectMapper.writeValueAsString(memberIds);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "序列化成员ID列表失败");
        }

        ActivityGroup group = ActivityGroup.builder()
                .activityId(activityId)
                .groupName(groupName)
                .memberIds(memberIdsJson)
                .build();
        return activityGroupRepository.save(group);
    }

    @Override
    @Transactional
    public void updateGroupMembers(Long activityId, Long groupId, List<Long> memberIds) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        ActivityGroup group = activityGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(404, "分组不存在"));

        if (!group.getActivityId().equals(activityId)) {
            throw new BusinessException(400, "分组不属于该活动");
        }

        String memberIdsJson;
        try {
            memberIdsJson = objectMapper.writeValueAsString(memberIds);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "序列化成员ID列表失败");
        }

        group.setMemberIds(memberIdsJson);
        activityGroupRepository.save(group);
    }

    @Override
    public List<ActivityGroup> getGroups(Long activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        return activityGroupRepository.findByActivityId(activityId);
    }

    @Override
    @Transactional
    public String generateQrCode(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        String token = UUID.randomUUID().toString();
        activity.setQrToken(token);
        activityRepository.save(activity);

        return token;
    }

    @Override
    @Transactional
    public void submitFeedback(Long activityId, Long userId, FeedbackRequest request) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        if (activityFeedbackRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new BusinessException(409, "已提交过反馈，不可重复提交");
        }

        ActivityFeedback feedback = ActivityFeedback.builder()
                .activityId(activityId)
                .userId(userId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        activityFeedbackRepository.save(feedback);
    }

    @Override
    public List<ActivityFeedback> getFeedback(Long activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        return activityFeedbackRepository.findByActivityId(activityId);
    }

    @Override
    @Transactional
    public ActivityStatistics getStatistics(Long activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        long totalRegistered = registrationRepository.countByActivityId(activityId);
        long totalAttended = registrationRepository.countByActivityIdAndCheckedInTrue(activityId);

        BigDecimal checkInRate = BigDecimal.ZERO;
        if (totalRegistered > 0) {
            checkInRate = BigDecimal.valueOf(totalAttended)
                    .divide(BigDecimal.valueOf(totalRegistered), 2, RoundingMode.HALF_UP);
        }

        List<ActivityFeedback> feedbackList = activityFeedbackRepository.findByActivityId(activityId);
        BigDecimal avgRating = BigDecimal.ZERO;
        String feedbackSummaryJson = null;
        if (!feedbackList.isEmpty()) {
            double avg = feedbackList.stream()
                    .mapToInt(ActivityFeedback::getRating)
                    .average()
                    .orElse(0.0);
            avgRating = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

            try {
                feedbackSummaryJson = objectMapper.writeValueAsString(
                        feedbackList.stream()
                                .map(f -> java.util.Map.of(
                                        "userId", f.getUserId(),
                                        "rating", f.getRating(),
                                        "comment", f.getComment() != null ? f.getComment() : ""))
                                .toList());
            } catch (JsonProcessingException e) {
                throw new BusinessException(500, "序列化反馈汇总失败");
            }
        }

        ActivityStatistics stats = activityStatisticsRepository.findByActivityId(activityId)
                .orElse(ActivityStatistics.builder().activityId(activityId).build());

        stats.setTotalRegistered((int) totalRegistered);
        stats.setTotalAttended((int) totalAttended);
        stats.setCheckInRate(checkInRate);
        stats.setAvgFeedbackRating(avgRating);
        stats.setFeedbackSummary(feedbackSummaryJson);
        stats.setGeneratedAt(LocalDateTime.now());

        return activityStatisticsRepository.save(stats);
    }

    @Override
    @Transactional
    public void uploadMaterial(Long activityId, MultipartFile file, Long uploadedBy) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = file.getContentType();
        String storedFileName = UUID.randomUUID() + "_" + (originalFilename != null ? originalFilename : "unknown");

        try {
            Path uploadDir = Paths.get("uploads", "activities", activityId.toString());
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(storedFileName);
            Files.write(filePath, file.getBytes());

            ActivityMaterial material = ActivityMaterial.builder()
                    .activityId(activityId)
                    .fileName(originalFilename != null ? originalFilename : "unknown")
                    .fileUrl(filePath.toString())
                    .fileType(fileType)
                    .uploadedBy(uploadedBy)
                    .build();
            activityMaterialRepository.save(material);
        } catch (java.io.IOException e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public List<ActivityMaterial> getMaterials(Long activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));

        return activityMaterialRepository.findByActivityId(activityId);
    }
}
