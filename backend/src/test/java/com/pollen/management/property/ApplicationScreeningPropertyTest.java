package com.pollen.management.property;

import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.ExamType;
import com.pollen.management.service.ApplicationScreeningServiceImpl;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ApplicationScreeningService.
 *
 * Property 28: 出生年月年龄计算正确性
 * Property 29: 自动筛选规则正确性
 * Property 30: 人工重点审核标记正确性
 *
 * **Validates: Requirements 5.2, 5.5, 5.6, 5.7, 5.8**
 */
class ApplicationScreeningPropertyTest {

    private final ApplicationScreeningServiceImpl service = new ApplicationScreeningServiceImpl();

    // ========================================================================
    // Property 28: 出生年月年龄计算正确性
    // For any birth date, calculated age = currentYear - birthYear,
    // minus 1 if current month/day is before birth month/day.
    // **Validates: Requirements 5.2**
    // ========================================================================

    @Property(tries = 500)
    void property28_ageCalculationMatchesYearDifferenceWithBirthdayAdjustment(
            @ForAll("pastBirthDates") LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int expectedAge = today.getYear() - birthDate.getYear();
        boolean birthdayNotYetReached =
                today.getMonthValue() < birthDate.getMonthValue()
                || (today.getMonthValue() == birthDate.getMonthValue()
                    && today.getDayOfMonth() < birthDate.getDayOfMonth());
        if (birthdayNotYetReached) {
            expectedAge--;
        }

        int actualAge = service.calculateAge(birthDate);
        assertThat(actualAge)
                .as("Age for birthDate %s should be %d", birthDate, expectedAge)
                .isEqualTo(expectedAge);
    }

    @Property(tries = 300)
    void property28_ageIsAlwaysNonNegativeForPastDates(
            @ForAll("pastBirthDates") LocalDate birthDate) {
        assertThat(service.calculateAge(birthDate))
                .as("Age for birthDate %s must be >= 0", birthDate)
                .isGreaterThanOrEqualTo(0);
    }

    @Property(tries = 1)
    void property28_bornTodayHasAgeZero() {
        assertThat(service.calculateAge(LocalDate.now())).isEqualTo(0);
    }

    // ========================================================================
    // Property 29: 自动筛选规则正确性
    // Auto-screening rejects iff: (a) age < 18, (b) examFlag=true AND exam
    // date within 3 months, (c) pollen UID format invalid.
    // **Validates: Requirements 5.5, 5.6, 5.7**
    // ========================================================================

    @Property(tries = 200)
    void property29_invalidPollenUidCausesRejection(
            @ForAll("invalidPollenUids") String uid) {
        ApplicationFormData form = passingFormData();
        form.setPollenUid(uid);

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed())
                .as("Invalid UID '%s' must cause rejection", uid)
                .isFalse();
        assertThat(result.getRejectReason()).isNotBlank();
    }

    @Property(tries = 200)
    void property29_validPollenUidIsAccepted(
            @ForAll("validPollenUids") String uid) {
        assertThat(service.validatePollenUid(uid))
                .as("Valid UID '%s' must be accepted", uid)
                .isTrue();
    }

    @Property(tries = 200)
    void property29_underAge18CausesRejection(
            @ForAll("underAgeBirthDates") LocalDate birthDate) {
        ApplicationFormData form = passingFormData();
        form.setBirthDate(birthDate);

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed())
                .as("Under-18 birthDate %s must cause rejection", birthDate)
                .isFalse();
        assertThat(result.getRejectReason()).isNotBlank();
    }

    @Property(tries = 200)
    void property29_examWithinThreeMonthsCausesRejection(
            @ForAll("examDatesWithinThreeMonths") LocalDate examDate) {
        ApplicationFormData form = passingFormData();
        form.setExamFlag(true);
        form.setExamType(ExamType.GAOKAO);
        form.setExamDate(examDate);

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed())
                .as("Exam date %s within 3 months must cause rejection", examDate)
                .isFalse();
        assertThat(result.getRejectReason()).isNotBlank();
    }

    @Property(tries = 200)
    void property29_validDataWithNoRejectConditionPasses(
            @ForAll("validPollenUids") String uid,
            @ForAll("adultBirthDates") LocalDate birthDate) {
        ApplicationFormData form = ApplicationFormData.builder()
                .pollenUid(uid)
                .birthDate(birthDate)
                .educationStage(EducationStage.UNIVERSITY)
                .examFlag(false)
                .weeklyAvailableDays(5)
                .dailyAvailableHours(BigDecimal.valueOf(4.0))
                .build();

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed())
                .as("Valid UID '%s', adult birthDate %s, no exam → must pass", uid, birthDate)
                .isTrue();
    }

    @Property(tries = 200)
    void property29_examBeyondThreeMonthsDoesNotReject(
            @ForAll("examDatesBeyondThreeMonths") LocalDate examDate) {
        ApplicationFormData form = passingFormData();
        form.setExamFlag(true);
        form.setExamType(ExamType.ZHONGKAO);
        form.setExamDate(examDate);

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed())
                .as("Exam date %s beyond 3 months must not cause rejection", examDate)
                .isTrue();
    }

    // ========================================================================
    // Property 30: 人工重点审核标记正确性
    // For passed applications: needs_attention = true when age 18-19
    // OR weekly available hours < 10.
    // **Validates: Requirements 5.8**
    // ========================================================================

    @Property(tries = 200)
    void property30_age18or19TriggersAttentionFlag(
            @ForAll("age18or19BirthDates") LocalDate birthDate) {
        ApplicationFormData form = passingFormData();
        form.setBirthDate(birthDate);
        form.setDailyAvailableHours(BigDecimal.valueOf(5.0));
        form.setWeeklyAvailableDays(5);

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention())
                .as("Age 18-19 (birthDate %s) must trigger needs_attention", birthDate)
                .isTrue();
        assertThat(result.getAttentionFlags()).isNotEmpty();
    }

    @Property(tries = 200)
    void property30_lowWeeklyHoursTriggersAttentionFlag(
            @ForAll("lowWeeklyHoursParams") BigDecimal[] params) {
        BigDecimal dailyHours = params[0];
        int weeklyDays = params[1].intValue();

        ApplicationFormData form = passingFormData();
        form.setBirthDate(LocalDate.now().minusYears(25));
        form.setDailyAvailableHours(dailyHours);
        form.setWeeklyAvailableDays(weeklyDays);

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention())
                .as("Weekly hours %s * %d < 10 must trigger needs_attention",
                        dailyHours, weeklyDays)
                .isTrue();
    }

    @Property(tries = 200)
    void property30_noAttentionWhenAge20PlusAndSufficientHours(
            @ForAll("matureAdultBirthDates") LocalDate birthDate,
            @ForAll("sufficientWeeklyHoursParams") BigDecimal[] params) {
        BigDecimal dailyHours = params[0];
        int weeklyDays = params[1].intValue();

        ApplicationFormData form = passingFormData();
        form.setBirthDate(birthDate);
        form.setDailyAvailableHours(dailyHours);
        form.setWeeklyAvailableDays(weeklyDays);

        ScreeningResult result = service.autoScreen(form);
        assertThat(result.isPassed()).isTrue();
        assertThat(result.isNeedsAttention())
                .as("Age >= 20 and weekly hours >= 10 must NOT trigger needs_attention")
                .isFalse();
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<LocalDate> pastBirthDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(1, 100)
                .flatMap(yearsAgo -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(today.getYear() - yearsAgo, month, day))));
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
        return Arbitraries.integers().between(5, 11)
                .flatMap(len -> Arbitraries.integers().between(1, 9)
                        .flatMap(firstDigit -> Arbitraries.strings().numeric().ofLength(len - 1)
                                .map(rest -> firstDigit + rest)));
    }

    @Provide
    Arbitrary<LocalDate> underAgeBirthDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(0, 17)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> {
                                    LocalDate candidate = LocalDate.of(today.getYear() - age, month, day);
                                    if (candidate.isAfter(today)) {
                                        candidate = candidate.minusYears(1);
                                    }
                                    return candidate;
                                })))
                .filter(bd -> service.calculateAge(bd) < 18);
    }

    @Provide
    Arbitrary<LocalDate> adultBirthDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(18, 80)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(today.getYear() - age, month, day))))
                .filter(bd -> service.calculateAge(bd) >= 18);
    }

    @Provide
    Arbitrary<LocalDate> examDatesWithinThreeMonths() {
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(1, 89)
                .map(today::plusDays)
                .filter(d -> ChronoUnit.MONTHS.between(today, d) < 3);
    }

    @Provide
    Arbitrary<LocalDate> examDatesBeyondThreeMonths() {
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(91, 365)
                .map(today::plusDays)
                .filter(d -> ChronoUnit.MONTHS.between(today, d) >= 3);
    }

    @Provide
    Arbitrary<LocalDate> age18or19BirthDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.of(18, 19)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(today.getYear() - age, month, day))))
                .filter(bd -> {
                    int a = service.calculateAge(bd);
                    return a == 18 || a == 19;
                });
    }

    @Provide
    Arbitrary<LocalDate> matureAdultBirthDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.integers().between(20, 60)
                .flatMap(age -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(today.getYear() - age, month, day))))
                .filter(bd -> service.calculateAge(bd) >= 20);
    }

    @Provide
    Arbitrary<BigDecimal[]> lowWeeklyHoursParams() {
        // Use integers (tenths of hours) to avoid floating point precision issues
        return Arbitraries.integers().between(1, 7)
                .flatMap(days -> {
                    int maxTenths = (int) (99.0 / days); // 9.9 / days in tenths
                    maxTenths = Math.max(5, Math.min(maxTenths, 99)); // clamp [0.5, 9.9]
                    return Arbitraries.integers().between(5, maxTenths)
                            .map(tenths -> new BigDecimal[]{
                                    BigDecimal.valueOf(tenths, 1), // e.g. 15 -> 1.5
                                    BigDecimal.valueOf(days)
                            });
                })
                .filter(p -> p[0].multiply(p[1]).compareTo(BigDecimal.TEN) < 0);
    }

    @Provide
    Arbitrary<BigDecimal[]> sufficientWeeklyHoursParams() {
        // Use integers (tenths of hours) to avoid floating point precision issues
        return Arbitraries.integers().between(2, 7)
                .flatMap(days -> {
                    int minTenths = (int) Math.ceil(100.0 / days); // 10.0 / days in tenths, rounded up
                    minTenths = Math.max(minTenths, 15); // at least 1.5
                    return Arbitraries.integers().between(minTenths, 120) // up to 12.0
                            .map(tenths -> new BigDecimal[]{
                                    BigDecimal.valueOf(tenths, 1),
                                    BigDecimal.valueOf(days)
                            });
                })
                .filter(p -> p[0].multiply(p[1]).compareTo(BigDecimal.TEN) >= 0);
    }

    // ========== Helper ==========

    private ApplicationFormData passingFormData() {
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
