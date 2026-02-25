package com.pollen.management.service;

import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.ExamType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApplicationScreeningServiceImpl
 * Validates: Requirements 5.5, 5.6, 5.7, 5.8
 */
class ApplicationScreeningServiceImplTest {

    private ApplicationScreeningServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApplicationScreeningServiceImpl();
    }

    // === validatePollenUid ===

    @Test
    void validatePollenUid_validFiveDigit_returnsTrue() {
        assertThat(service.validatePollenUid("10000")).isTrue();
    }

    @Test
    void validatePollenUid_validElevenDigit_returnsTrue() {
        assertThat(service.validatePollenUid("12345678901")).isTrue();
    }

    @Test
    void validatePollenUid_null_returnsFalse() {
        assertThat(service.validatePollenUid(null)).isFalse();
    }

    @Test
    void validatePollenUid_empty_returnsFalse() {
        assertThat(service.validatePollenUid("")).isFalse();
    }

    @Test
    void validatePollenUid_startsWithZero_returnsFalse() {
        assertThat(service.validatePollenUid("01234")).isFalse();
    }

    @Test
    void validatePollenUid_tooShort_returnsFalse() {
        assertThat(service.validatePollenUid("1234")).isFalse();
    }

    @Test
    void validatePollenUid_tooLong_returnsFalse() {
        assertThat(service.validatePollenUid("123456789012")).isFalse();
    }

    @Test
    void validatePollenUid_containsLetters_returnsFalse() {
        assertThat(service.validatePollenUid("1234a")).isFalse();
    }

    // === calculateAge ===

    @Test
    void calculateAge_birthdayAlreadyPassedThisYear() {
        LocalDate today = LocalDate.now();
        LocalDate birthDate = LocalDate.of(today.getYear() - 20, 1, 1);
        assertThat(service.calculateAge(birthDate)).isEqualTo(20);
    }

    @Test
    void calculateAge_birthdayNotYetThisYear() {
        LocalDate today = LocalDate.now();
        LocalDate birthDate = LocalDate.of(today.getYear() - 20, 12, 31);
        // If today is Dec 31, age is 20; otherwise age is 19
        int expected = (today.getMonthValue() == 12 && today.getDayOfMonth() == 31) ? 20 : 19;
        assertThat(service.calculateAge(birthDate)).isEqualTo(expected);
    }

    @Test
    void calculateAge_birthdayIsToday() {
        LocalDate today = LocalDate.now();
        LocalDate birthDate = LocalDate.of(today.getYear() - 18, today.getMonthValue(), today.getDayOfMonth());
        assertThat(service.calculateAge(birthDate)).isEqualTo(18);
    }

    // === isExamPeriod ===

    @Test
    void isExamPeriod_examFlagFalse_returnsFalse() {
        assertThat(service.isExamPeriod(false, LocalDate.now().plusMonths(1))).isFalse();
    }

    @Test
    void isExamPeriod_examDateNull_returnsFalse() {
        assertThat(service.isExamPeriod(true, null)).isFalse();
    }

    @Test
    void isExamPeriod_examDatePast_returnsFalse() {
        assertThat(service.isExamPeriod(true, LocalDate.now().minusDays(1))).isFalse();
    }

    @Test
    void isExamPeriod_withinThreeMonths_returnsTrue() {
        assertThat(service.isExamPeriod(true, LocalDate.now().plusMonths(2))).isTrue();
    }

    @Test
    void isExamPeriod_exactlyThreeMonthsAway_returnsFalse() {
        assertThat(service.isExamPeriod(true, LocalDate.now().plusMonths(3))).isFalse();
    }

    @Test
    void isExamPeriod_moreThanThreeMonths_returnsFalse() {
        assertThat(service.isExamPeriod(true, LocalDate.now().plusMonths(4))).isFalse();
    }

    // === autoScreen ===

    @Test
    void autoScreen_invalidUid_rejected() {
        ApplicationFormData formData = buildValidFormData();
        formData.setPollenUid("0123");
        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getRejectReason()).contains("UID");
    }

    @Test
    void autoScreen_underAge_rejected() {
        ApplicationFormData formData = buildValidFormData();
        formData.setBirthDate(LocalDate.now().minusYears(17));
        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getRejectReason()).contains("18");
    }

    @Test
    void autoScreen_examPeriod_rejected() {
        ApplicationFormData formData = buildValidFormData();
        formData.setExamFlag(true);
        formData.setExamType(ExamType.GAOKAO);
        formData.setExamDate(LocalDate.now().plusMonths(2));
        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getRejectReason()).contains("备考");
    }

    @Test
    void autoScreen_validAdult_passed() {
        ApplicationFormData formData = buildValidFormData();
        formData.setBirthDate(LocalDate.now().minusYears(25));
        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention()).isFalse();
    }

    @Test
    void autoScreen_age18_needsAttention() {
        ApplicationFormData formData = buildValidFormData();
        // Set birthDate so age is exactly 18
        formData.setBirthDate(LocalDate.now().minusYears(18));
        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention()).isTrue();
        assertThat(result.getAttentionFlags()).anyMatch(f -> f.contains("18"));
    }

    @Test
    void autoScreen_lowWeeklyHours_needsAttention() {
        ApplicationFormData formData = buildValidFormData();
        formData.setBirthDate(LocalDate.now().minusYears(25));
        formData.setDailyAvailableHours(BigDecimal.valueOf(1.0));
        formData.setWeeklyAvailableDays(5);
        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention()).isTrue();
        assertThat(result.getAttentionFlags()).anyMatch(f -> f.contains("可用时间"));
    }

    // === Helper ===

    private ApplicationFormData buildValidFormData() {
        return ApplicationFormData.builder()
                .pollenUid("123456789")
                .birthDate(LocalDate.now().minusYears(22))
                .educationStage(EducationStage.UNIVERSITY)
                .examFlag(false)
                .weeklyAvailableDays(5)
                .dailyAvailableHours(BigDecimal.valueOf(4.0))
                .build();
    }
}
