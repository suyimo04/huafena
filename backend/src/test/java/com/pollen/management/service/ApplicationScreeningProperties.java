package com.pollen.management.service;

import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.ExamType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management
 * Property 28: 出生年月年龄计算正确性
 * Property 29: 自动筛选规则正确性
 * Property 30: 人工重点审核标记正确性
 * **Validates: Requirements 5.2, 5.5, 5.6, 5.7, 5.8**
 */
class ApplicationScreeningProperties {

    private final ApplicationScreeningServiceImpl service = new ApplicationScreeningServiceImpl();

    // ========================================================================
    // Property 28: 出生年月年龄计算正确性
    // For any birth date, the calculated age should equal current year minus
    // birth year, minus 1 if current month/day is before birth month/day.
    // **Validates: Requirements 5.2**
    // ========================================================================

    /**
     * Property 28a: Age equals year difference minus adjustment for birthday not yet reached.
     */
    @Property(tries = 500)
    void ageEqualsYearDifferenceWithBirthdayAdjustment(@ForAll("pastBirthDates") LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int expectedAge = today.getYear() - birthDate.getYear();
        boolean birthdayNotReached = today.getMonthValue() < birthDate.getMonthValue()
                || (today.getMonthValue() == birthDate.getMonthValue()
                    && today.getDayOfMonth() < birthDate.getDayOfMonth());
        if (birthdayNotReached) {
            expectedAge--;
        }

        int actualAge = service.calculateAge(birthDate);
        assertThat(actualAge)
                .as("Age for birthDate %s should be %d", birthDate, expectedAge)
                .isEqualTo(expectedAge);
    }

    /**
     * Property 28b: Age is always non-negative for birth dates in the past.
     */
    @Property(tries = 500)
    void ageIsNonNegativeForPastDates(@ForAll("pastBirthDates") LocalDate birthDate) {
        int age = service.calculateAge(birthDate);
        assertThat(age)
                .as("Age for birthDate %s should be >= 0", birthDate)
                .isGreaterThanOrEqualTo(0);
    }

    /**
     * Property 28c: A person born exactly today has age 0.
     */
    @Property(tries = 1)
    void bornTodayHasAgeZero() {
        LocalDate today = LocalDate.now();
        assertThat(service.calculateAge(today)).isEqualTo(0);
    }

    // ========================================================================
    // Property 29: 自动筛选规则正确性
    // For any application form data, auto-screening should reject if and only if:
    // (a) age < 18, (b) examFlag=true AND exam date within 3 months,
    // (c) pollen UID format invalid (not 5-11 digits or starts with 0).
    // **Validates: Requirements 5.5, 5.6, 5.7**
    // ========================================================================

    /**
     * Property 29a: Invalid pollen UID always causes rejection.
     */
    @Property(tries = 200)
    void invalidPollenUidAlwaysRejected(@ForAll("invalidPollenUids") String uid) {
        ApplicationFormData formData = buildPassingFormData();
        formData.setPollenUid(uid);

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed())
                .as("Invalid UID '%s' should cause rejection", uid)
                .isFalse();
        assertThat(result.getRejectReason()).isNotBlank();
    }

    /**
     * Property 29b: Age under 18 always causes rejection (with valid UID).
     */
    @Property(tries = 200)
    void underAgeAlwaysRejected(@ForAll("underAgeBirthDates") LocalDate birthDate) {
        ApplicationFormData formData = buildPassingFormData();
        formData.setBirthDate(birthDate);

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed())
                .as("Under-18 birthDate %s should cause rejection", birthDate)
                .isFalse();
        assertThat(result.getRejectReason()).isNotBlank();
    }

    /**
     * Property 29c: Exam flag true with exam date within 3 months always causes rejection.
     */
    @Property(tries = 200)
    void examPeriodAlwaysRejected(@ForAll("examDatesWithinThreeMonths") LocalDate examDate) {
        ApplicationFormData formData = buildPassingFormData();
        formData.setExamFlag(true);
        formData.setExamType(ExamType.GAOKAO);
        formData.setExamDate(examDate);

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed())
                .as("Exam date %s within 3 months should cause rejection", examDate)
                .isFalse();
        assertThat(result.getRejectReason()).isNotBlank();
    }

    /**
     * Property 29d: Valid UID + age >= 18 + no exam period → screening passes.
     */
    @Property(tries = 200)
    void validDataWithNoRejectConditionPasses(
            @ForAll("validPollenUids") String uid,
            @ForAll("adultBirthDates") LocalDate birthDate) {
        ApplicationFormData formData = ApplicationFormData.builder()
                .pollenUid(uid)
                .birthDate(birthDate)
                .educationStage(EducationStage.UNIVERSITY)
                .examFlag(false)
                .weeklyAvailableDays(5)
                .dailyAvailableHours(BigDecimal.valueOf(4.0))
                .build();

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed())
                .as("Valid UID '%s', adult birthDate %s, no exam → should pass", uid, birthDate)
                .isTrue();
    }

    /**
     * Property 29e: Valid UID format is accepted by validatePollenUid.
     */
    @Property(tries = 200)
    void validPollenUidAccepted(@ForAll("validPollenUids") String uid) {
        assertThat(service.validatePollenUid(uid))
                .as("Valid UID '%s' should be accepted", uid)
                .isTrue();
    }

    /**
     * Property 29f: Exam flag false or exam date beyond 3 months does not trigger exam rejection.
     */
    @Property(tries = 200)
    void examFlagFalseOrDistantExamDoesNotReject(
            @ForAll("examDatesBeyondThreeMonths") LocalDate examDate) {
        ApplicationFormData formData = buildPassingFormData();
        formData.setExamFlag(true);
        formData.setExamType(ExamType.ZHONGKAO);
        formData.setExamDate(examDate);

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed())
                .as("Exam date %s beyond 3 months should not cause rejection", examDate)
                .isTrue();
    }

    // ========================================================================
    // Property 30: 人工重点审核标记正确性
    // For any application that passes auto-screening, needs_attention should be
    // true when age is 18-19 OR weekly available hours < 10.
    // **Validates: Requirements 5.8**
    // ========================================================================

    /**
     * Property 30a: Age 18-19 triggers needs_attention flag.
     */
    @Property(tries = 200)
    void age18or19TriggersAttention(@ForAll("age18or19BirthDates") LocalDate birthDate) {
        ApplicationFormData formData = buildPassingFormData();
        formData.setBirthDate(birthDate);
        // Ensure weekly hours >= 10 so only age triggers attention
        formData.setDailyAvailableHours(BigDecimal.valueOf(5.0));
        formData.setWeeklyAvailableDays(5);

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention())
                .as("Age 18-19 (birthDate %s) should trigger needs_attention", birthDate)
                .isTrue();
        assertThat(result.getAttentionFlags()).isNotEmpty();
    }

    /**
     * Property 30b: Weekly available hours < 10 triggers needs_attention flag.
     */
    @Property(tries = 200)
    void lowWeeklyHoursTriggersAttention(
            @ForAll("lowWeeklyHoursParams") BigDecimal[] params) {
        BigDecimal dailyHours = params[0];
        int weeklyDays = params[1].intValue();

        ApplicationFormData formData = buildPassingFormData();
        // Use age > 19 so only weekly hours triggers attention
        formData.setBirthDate(LocalDate.now().minusYears(25));
        formData.setDailyAvailableHours(dailyHours);
        formData.setWeeklyAvailableDays(weeklyDays);

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention())
                .as("Weekly hours %s * %d = %s < 10 should trigger needs_attention",
                        dailyHours, weeklyDays, dailyHours.multiply(BigDecimal.valueOf(weeklyDays)))
                .isTrue();
    }

    /**
     * Property 30c: Age >= 20 AND weekly hours >= 10 → needs_attention is false.
     */
    @Property(tries = 200)
    void noAttentionWhenAgeAbove19AndSufficientHours(
            @ForAll("matureAdultBirthDates") LocalDate birthDate,
            @ForAll("sufficientWeeklyHoursParams") BigDecimal[] params) {
        BigDecimal dailyHours = params[0];
        int weeklyDays = params[1].intValue();

        ApplicationFormData formData = buildPassingFormData();
        formData.setBirthDate(birthDate);
        formData.setDailyAvailableHours(dailyHours);
        formData.setWeeklyAvailableDays(weeklyDays);

        ScreeningResult result = service.autoScreen(formData);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention())
                .as("Age >= 20 (birthDate %s) and weekly hours >= 10 should NOT trigger needs_attention", birthDate)
                .isFalse();
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<LocalDate> pastBirthDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(1, 100)
                .flatMap(yearsAgo -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> {
                                    int year = today.getYear() - yearsAgo;
                                    return LocalDate.of(year, month, day);
                                })));
    }

    @Provide
    Arbitrary<String> invalidPollenUids() {
        return Arbitraries.oneOf(
                // Too short (1-4 digits)
                Arbitraries.integers().between(1, 4)
                        .flatMap(len -> Arbitraries.strings().numeric().ofLength(len)
                                .filter(s -> !s.isEmpty() && s.charAt(0) != '0')),
                // Too long (12+ digits)
                Arbitraries.integers().between(12, 15)
                        .flatMap(len -> Arbitraries.strings().numeric().ofLength(len)),
                // Starts with 0
                Arbitraries.strings().numeric().ofMinLength(4).ofMaxLength(10)
                        .map(s -> "0" + s),
                // Contains non-digit characters
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(11),
                // Empty or blank
                Arbitraries.of("", " ", "  ")
        );
    }

    @Provide
    Arbitrary<String> validPollenUids() {
        // 5-11 digits, first digit 1-9
        return Arbitraries.integers().between(5, 11)
                .flatMap(len -> Arbitraries.integers().between(1, 9)
                        .flatMap(firstDigit -> Arbitraries.strings().numeric().ofLength(len - 1)
                                .map(rest -> firstDigit + rest)));
    }

    @Provide
    Arbitrary<LocalDate> underAgeBirthDates() {
        // Birth dates that result in age < 18
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(0, 17)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> {
                                    // Ensure age is strictly < 18
                                    LocalDate candidate = LocalDate.of(today.getYear() - age, month, day);
                                    // If candidate is in the future, shift back one year
                                    if (candidate.isAfter(today)) {
                                        candidate = candidate.minusYears(1);
                                    }
                                    return candidate;
                                })))
                .filter(bd -> service.calculateAge(bd) < 18);
    }

    @Provide
    Arbitrary<LocalDate> adultBirthDates() {
        // Birth dates that result in age >= 18
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(18, 80)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(today.getYear() - age, month, day))))
                .filter(bd -> service.calculateAge(bd) >= 18);
    }

    @Provide
    Arbitrary<LocalDate> examDatesWithinThreeMonths() {
        // Exam dates that are in the future and within 3 months
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(1, 89) // 1 to 89 days from now
                .map(today::plusDays)
                .filter(d -> {
                    long months = java.time.temporal.ChronoUnit.MONTHS.between(today, d);
                    return months < 3;
                });
    }

    @Provide
    Arbitrary<LocalDate> examDatesBeyondThreeMonths() {
        // Exam dates that are >= 3 months away
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(91, 365)
                .map(today::plusDays)
                .filter(d -> {
                    long months = java.time.temporal.ChronoUnit.MONTHS.between(today, d);
                    return months >= 3;
                });
    }

    @Provide
    Arbitrary<LocalDate> age18or19BirthDates() {
        // Birth dates that result in age exactly 18 or 19
        LocalDate today = LocalDate.now();
        return Arbitraries.of(18, 19)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(today.getYear() - age, month, day))))
                .filter(bd -> {
                    int age = service.calculateAge(bd);
                    return age == 18 || age == 19;
                });
    }

    @Provide
    Arbitrary<LocalDate> matureAdultBirthDates() {
        // Birth dates that result in age >= 20
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(20, 60)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(today.getYear() - age, month, day))))
                .filter(bd -> service.calculateAge(bd) >= 20);
    }

    @Provide
    Arbitrary<BigDecimal[]> lowWeeklyHoursParams() {
        // dailyHours * weeklyDays < 10
        // Use integer tenths to avoid floating-point precision issues
        return Arbitraries.integers().between(1, 7)
                .flatMap(days -> {
                    // Max daily hours (in tenths) so that (tenths/10) * days < 10
                    int maxTenths = Math.max(5, (int) (99.0 / days));
                    return Arbitraries.integers().between(5, maxTenths)
                            .map(tenths -> new BigDecimal[]{
                                    BigDecimal.valueOf(tenths, 1), // e.g. 15 -> 1.5
                                    BigDecimal.valueOf(days)
                            });
                })
                .filter(params -> params[0].multiply(params[1]).compareTo(BigDecimal.TEN) < 0);
    }

    @Provide
    Arbitrary<BigDecimal[]> sufficientWeeklyHoursParams() {
        // dailyHours * weeklyDays >= 10
        // Use integer tenths to avoid floating-point precision issues
        return Arbitraries.integers().between(2, 7)
                .flatMap(days -> {
                    int minTenths = Math.max(15, (int) Math.ceil(100.0 / days));
                    return Arbitraries.integers().between(minTenths, 120)
                            .map(tenths -> new BigDecimal[]{
                                    BigDecimal.valueOf(tenths, 1), // e.g. 25 -> 2.5
                                    BigDecimal.valueOf(days)
                            });
                })
                .filter(params -> params[0].multiply(params[1]).compareTo(BigDecimal.TEN) >= 0);
    }

    // ========== Helper ==========

    private ApplicationFormData buildPassingFormData() {
        return ApplicationFormData.builder()
                .pollenUid("123456789")
                .birthDate(LocalDate.now().minusYears(25))
                .educationStage(EducationStage.UNIVERSITY)
                .examFlag(false)
                .weeklyAvailableDays(5)
                .dailyAvailableHours(BigDecimal.valueOf(4.0))
                .build();
    }
}
