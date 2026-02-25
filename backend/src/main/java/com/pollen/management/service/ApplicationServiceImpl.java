package com.pollen.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.ApplicationTimeline;
import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.QuestionnaireResponse;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final QuestionnaireResponseService questionnaireResponseService;
    private final PublicLinkService publicLinkService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationScreeningService applicationScreeningService;
    private final ObjectMapper objectMapper;
    private final ApplicationTimelineService applicationTimelineService;

    /**
     * 未处理的申请状态列表，用于重复申请检测
     */
    private static final List<ApplicationStatus> PENDING_STATUSES = List.of(
            ApplicationStatus.PENDING_INITIAL_REVIEW,
            ApplicationStatus.INITIAL_REVIEW_PASSED,
            ApplicationStatus.AI_INTERVIEW_IN_PROGRESS,
            ApplicationStatus.PENDING_REVIEW
    );

    @Override
    @Transactional
    public Application createFromRegistration(Long userId, Long questionnaireResponseId, ApplicationFormData formData) {
        // 验证用户存在
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 重复申请检测
        checkDuplicateApplication(userId);

        // 执行自动筛选
        ScreeningResult screeningResult = applicationScreeningService.autoScreen(formData);

        // 创建申请记录
        Application application = buildApplication(userId, EntryType.REGISTRATION, questionnaireResponseId, formData, screeningResult);

        application = applicationRepository.save(application);

        // 记录时间线：申请提交
        applicationTimelineService.recordTimelineEvent(
                application.getId(),
                application.getStatus().name(),
                "系统",
                screeningResult.isPassed() ? "申请已提交，等待初审" : "申请已提交，自动筛选拒绝：" + screeningResult.getRejectReason());

        return application;
    }

    @Override
    @Transactional
    public Application createFromPublicLink(String linkToken, Map<String, Object> answers, ApplicationFormData formData) {
        // 验证公开链接有效性
        PublicLink link = publicLinkService.getActiveLink(linkToken);

        // 自动创建用户账户（APPLICANT, enabled=false）
        String generatedUsername = "public_" + UUID.randomUUID().toString().substring(0, 8);
        String generatedPassword = UUID.randomUUID().toString().substring(0, 12);

        User user = User.builder()
                .username(generatedUsername)
                .password(passwordEncoder.encode(generatedPassword))
                .role(Role.APPLICANT)
                .enabled(false)
                .build();
        user = userRepository.save(user);

        // 提交问卷回答
        QuestionnaireResponse response = questionnaireResponseService.submit(
                link.getVersionId(), user.getId(), answers);

        // 执行自动筛选
        ScreeningResult screeningResult = applicationScreeningService.autoScreen(formData);

        // 创建申请记录
        Application application = buildApplication(user.getId(), EntryType.PUBLIC_LINK, response.getId(), formData, screeningResult);

        application = applicationRepository.save(application);

        // 记录时间线：申请提交
        applicationTimelineService.recordTimelineEvent(
                application.getId(),
                application.getStatus().name(),
                "系统",
                screeningResult.isPassed() ? "通过公开链接提交申请，等待初审" : "通过公开链接提交申请，自动筛选拒绝：" + screeningResult.getRejectReason());

        return application;
    }

    @Override
    @Transactional
    public void initialReview(Long applicationId, boolean approved) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "申请记录不存在"));

        if (application.getStatus() != ApplicationStatus.PENDING_INITIAL_REVIEW) {
            throw new BusinessException(400, "当前申请状态不允许此操作");
        }

        if (approved) {
            application.setStatus(ApplicationStatus.INITIAL_REVIEW_PASSED);

            User user = userRepository.findById(application.getUserId())
                    .orElseThrow(() -> new BusinessException(404, "用户不存在"));
            user.setEnabled(true);
            userRepository.save(user);
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
        }

        applicationRepository.save(application);

        // 记录时间线：初审结果
        applicationTimelineService.recordTimelineEvent(
                applicationId,
                application.getStatus().name(),
                "审核人员",
                approved ? "初审通过，账户已启用" : "初审拒绝");
    }

    @Override
    public List<Application> listAll() {
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public void batchApprove(List<Long> applicationIds) {
        List<Application> applications = applicationRepository.findAllById(applicationIds);
        for (Application application : applications) {
            if (application.getStatus() != ApplicationStatus.PENDING_INITIAL_REVIEW) {
                continue;
            }
            application.setStatus(ApplicationStatus.INITIAL_REVIEW_PASSED);
            applicationRepository.save(application);

            User user = userRepository.findById(application.getUserId())
                    .orElse(null);
            if (user != null) {
                user.setEnabled(true);
                userRepository.save(user);
            }

            // 记录时间线：批量通过
            applicationTimelineService.recordTimelineEvent(
                    application.getId(),
                    ApplicationStatus.INITIAL_REVIEW_PASSED.name(),
                    "审核人员",
                    "批量初审通过，账户已启用");
        }
    }

    @Override
    @Transactional
    public void batchReject(List<Long> applicationIds) {
        List<Application> applications = applicationRepository.findAllById(applicationIds);
        for (Application application : applications) {
            if (application.getStatus() != ApplicationStatus.PENDING_INITIAL_REVIEW) {
                continue;
            }
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);

            // 记录时间线：批量拒绝
            applicationTimelineService.recordTimelineEvent(
                    application.getId(),
                    ApplicationStatus.REJECTED.name(),
                    "审核人员",
                    "批量初审拒绝");
        }
    }

    @Override
    @Transactional
    public void batchNotifyInterview(List<Long> applicationIds) {
        List<Application> applications = applicationRepository.findAllById(applicationIds);
        for (Application application : applications) {
            if (application.getStatus() != ApplicationStatus.INITIAL_REVIEW_PASSED) {
                continue;
            }
            application.setStatus(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS);
            applicationRepository.save(application);

            // 记录时间线：发送面试通知
            applicationTimelineService.recordTimelineEvent(
                    application.getId(),
                    ApplicationStatus.AI_INTERVIEW_IN_PROGRESS.name(),
                    "审核人员",
                    "已发送AI面试通知");
        }
    }

    @Override
    public byte[] exportToExcel(ApplicationStatus status) {
        List<Application> applications;
        if (status != null) {
            applications = applicationRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            applications = applicationRepository.findAllByOrderByCreatedAtDesc();
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("报名数据");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header row
            String[] headers = {"ID", "用户ID", "状态", "入口类型", "花粉UID", "出生日期",
                    "年龄", "教育阶段", "中高考标识", "考试类型", "考试日期",
                    "每周可用天数", "每日可用时长", "筛选通过", "筛选拒绝原因",
                    "需要重点审核", "创建时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int i = 0; i < applications.size(); i++) {
                Application app = applications.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(app.getId() != null ? app.getId() : 0);
                row.createCell(1).setCellValue(app.getUserId() != null ? app.getUserId() : 0);
                row.createCell(2).setCellValue(app.getStatus() != null ? app.getStatus().name() : "");
                row.createCell(3).setCellValue(app.getEntryType() != null ? app.getEntryType().name() : "");
                row.createCell(4).setCellValue(app.getPollenUid() != null ? app.getPollenUid() : "");
                row.createCell(5).setCellValue(app.getBirthDate() != null ? app.getBirthDate().toString() : "");
                row.createCell(6).setCellValue(app.getCalculatedAge() != null ? app.getCalculatedAge() : 0);
                row.createCell(7).setCellValue(app.getEducationStage() != null ? app.getEducationStage().name() : "");
                row.createCell(8).setCellValue(app.getExamFlag() != null && app.getExamFlag() ? "是" : "否");
                row.createCell(9).setCellValue(app.getExamType() != null ? app.getExamType().name() : "");
                row.createCell(10).setCellValue(app.getExamDate() != null ? app.getExamDate().toString() : "");
                row.createCell(11).setCellValue(app.getWeeklyAvailableDays() != null ? app.getWeeklyAvailableDays() : 0);
                row.createCell(12).setCellValue(app.getDailyAvailableHours() != null ? app.getDailyAvailableHours().doubleValue() : 0);
                row.createCell(13).setCellValue(app.getScreeningPassed() != null && app.getScreeningPassed() ? "是" : "否");
                row.createCell(14).setCellValue(app.getScreeningRejectReason() != null ? app.getScreeningRejectReason() : "");
                row.createCell(15).setCellValue(app.getNeedsAttention() != null && app.getNeedsAttention() ? "是" : "否");
                row.createCell(16).setCellValue(app.getCreatedAt() != null ? app.getCreatedAt().toString() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "Excel 导出失败: " + e.getMessage());
        }
    }

    @Override
    public List<ApplicationTimeline> getTimeline(Long applicationId) {
        // Verify application exists
        applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "申请记录不存在"));
        return applicationTimelineService.getTimeline(applicationId);
    }

    /**
     * 构建 Application 实体，填充表单数据和筛选结果
     */
    private Application buildApplication(Long userId, EntryType entryType, Long questionnaireResponseId,
                                         ApplicationFormData formData, ScreeningResult screeningResult) {
        ApplicationStatus status;
        if (screeningResult.isPassed()) {
            status = ApplicationStatus.PENDING_INITIAL_REVIEW;
        } else {
            status = ApplicationStatus.AUTO_REJECTED;
        }

        int calculatedAge = applicationScreeningService.calculateAge(formData.getBirthDate());

        String weeklyAvailableSlotsJson = null;
        if (formData.getWeeklyAvailableSlots() != null) {
            try {
                weeklyAvailableSlotsJson = objectMapper.writeValueAsString(formData.getWeeklyAvailableSlots());
            } catch (JsonProcessingException e) {
                // ignore serialization error for optional field
            }
        }

        String attentionFlagsJson = null;
        if (screeningResult.getAttentionFlags() != null && !screeningResult.getAttentionFlags().isEmpty()) {
            try {
                attentionFlagsJson = objectMapper.writeValueAsString(screeningResult.getAttentionFlags());
            } catch (JsonProcessingException e) {
                // ignore serialization error
            }
        }

        return Application.builder()
                .userId(userId)
                .status(status)
                .entryType(entryType)
                .questionnaireResponseId(questionnaireResponseId)
                // V3.1 表单数据
                .pollenUid(formData.getPollenUid())
                .birthDate(formData.getBirthDate())
                .calculatedAge(calculatedAge)
                .educationStage(formData.getEducationStage())
                .examFlag(formData.getExamFlag() != null ? formData.getExamFlag() : false)
                .examType(formData.getExamType())
                .examDate(formData.getExamDate())
                .weeklyAvailableSlots(weeklyAvailableSlotsJson)
                .weeklyAvailableDays(formData.getWeeklyAvailableDays())
                .dailyAvailableHours(formData.getDailyAvailableHours())
                // V3.1 筛选结果
                .screeningPassed(screeningResult.isPassed())
                .screeningRejectReason(screeningResult.getRejectReason())
                .needsAttention(screeningResult.isNeedsAttention())
                .attentionFlags(attentionFlagsJson)
                .build();
    }

    /**
     * 检测重复申请：已有未处理申请时拒绝
     */
    private void checkDuplicateApplication(Long userId) {
        boolean hasPending = applicationRepository.existsByUserIdAndStatusIn(userId, PENDING_STATUSES);
        if (hasPending) {
            throw new BusinessException(409, "已有未处理的申请");
        }
    }
}
