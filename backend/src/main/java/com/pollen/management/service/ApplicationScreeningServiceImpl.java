package com.pollen.management.service;

import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * V3.1 自动筛选服务实现
 */
@Service
public class ApplicationScreeningServiceImpl implements ApplicationScreeningService {

    /** QQ号格式：5-11位纯数字，首位不为0 */
    private static final Pattern POLLEN_UID_PATTERN = Pattern.compile("^[1-9]\\d{4,10}$");

    @Override
    public ScreeningResult autoScreen(ApplicationFormData formData) {
        List<String> attentionFlags = new ArrayList<>();

        // === 自动拒绝规则 ===

        // 规则1：花粉 UID 格式校验
        if (!validatePollenUid(formData.getPollenUid())) {
            return ScreeningResult.builder()
                    .passed(false)
                    .needsAttention(false)
                    .rejectReason("花粉社区UID格式不合法，需为5-11位纯数字且首位不为0")
                    .attentionFlags(List.of())
                    .build();
        }

        // 规则2：年龄 < 18 岁
        int age = calculateAge(formData.getBirthDate());
        if (age < 18) {
            return ScreeningResult.builder()
                    .passed(false)
                    .needsAttention(false)
                    .rejectReason("年龄不符合要求，申请者未满18岁")
                    .attentionFlags(List.of())
                    .build();
        }

        // 规则3：中高考备考期（examFlag=true 且距考试时间不足3个月）
        boolean examFlag = formData.getExamFlag() != null && formData.getExamFlag();
        if (examFlag && formData.getExamDate() != null && isExamPeriod(true, formData.getExamDate())) {
            return ScreeningResult.builder()
                    .passed(false)
                    .needsAttention(false)
                    .rejectReason("备考期间不宜申请，距离考试时间不足3个月")
                    .attentionFlags(List.of())
                    .build();
        }

        // === 人工重点审核标记 ===

        // 标记1：年龄 18-19 岁
        if (age >= 18 && age <= 19) {
            attentionFlags.add("年龄刚满18岁（" + age + "岁），建议重点审核");
        }

        // 标记2：每周可用时间 < 10 小时
        BigDecimal weeklyHours = calculateWeeklyAvailableHours(
                formData.getDailyAvailableHours(), formData.getWeeklyAvailableDays());
        if (weeklyHours != null && weeklyHours.compareTo(BigDecimal.TEN) < 0) {
            attentionFlags.add("每周可用时间较少（" + weeklyHours + "小时），建议重点审核");
        }

        boolean needsAttention = !attentionFlags.isEmpty();

        return ScreeningResult.builder()
                .passed(true)
                .needsAttention(needsAttention)
                .rejectReason(null)
                .attentionFlags(attentionFlags)
                .build();
    }

    @Override
    public boolean validatePollenUid(String uid) {
        if (uid == null || uid.isBlank()) {
            return false;
        }
        return POLLEN_UID_PATTERN.matcher(uid).matches();
    }

    @Override
    public int calculateAge(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int age = today.getYear() - birthDate.getYear();
        // 如果当前月日早于出生月日，则年龄减1
        if (today.getMonthValue() < birthDate.getMonthValue()
                || (today.getMonthValue() == birthDate.getMonthValue()
                    && today.getDayOfMonth() < birthDate.getDayOfMonth())) {
            age--;
        }
        return age;
    }

    @Override
    public boolean isExamPeriod(boolean examFlag, LocalDate examDate) {
        if (!examFlag || examDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        // 考试日期已过，不算备考期
        if (examDate.isBefore(today)) {
            return false;
        }
        long monthsBetween = ChronoUnit.MONTHS.between(today, examDate);
        return monthsBetween < 3;
    }

    /**
     * 计算每周可用时间 = 每日可用时长 × 每周可用天数
     */
    private BigDecimal calculateWeeklyAvailableHours(BigDecimal dailyHours, Integer weeklyDays) {
        if (dailyHours == null || weeklyDays == null) {
            return null;
        }
        return dailyHours.multiply(BigDecimal.valueOf(weeklyDays));
    }
}
