package com.pollen.management.property;

import com.pollen.management.service.EmailRateLimiterImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for email rate limiting mechanism.
 *
 * Property 38: 邮件防滥发机制
 * For any recipient, within a 5-minute window the number of emails sent
 * should not exceed 3. When exceeded, rate limiting should trigger and
 * pause sending.
 *
 * **Validates: Requirements 13.6**
 */
class EmailRateLimiterPropertyTest {

    /** Max sends per recipient within the window (mirrors EmailRateLimiterImpl.MAX_SENDS). */
    private static final int MAX_SENDS = 3;

    // ========================================================================
    // Property 38: After exactly 3 recordSend calls within the window,
    // isAllowed should return false for that recipient.
    // **Validates: Requirements 13.6**
    // ========================================================================

    @Property(tries = 200)
    void property38_afterThreeSendsIsAllowedReturnsFalse(
            @ForAll("validEmails") String recipient) {
        EmailRateLimiterImpl limiter = new EmailRateLimiterImpl();

        // Before any sends, should be allowed
        assertThat(limiter.isAllowed(recipient)).isTrue();

        // Record exactly 3 sends
        for (int i = 0; i < MAX_SENDS; i++) {
            limiter.recordSend(recipient);
        }

        // After 3 sends, should be blocked
        assertThat(limiter.isAllowed(recipient)).isFalse();
    }

    @Property(tries = 200)
    void property38_underLimitIsAllowed(
            @ForAll("validEmails") String recipient,
            @ForAll @IntRange(min = 1, max = 2) int sendCount) {
        EmailRateLimiterImpl limiter = new EmailRateLimiterImpl();

        for (int i = 0; i < sendCount; i++) {
            limiter.recordSend(recipient);
        }

        // Under the limit, should still be allowed
        assertThat(limiter.isAllowed(recipient)).isTrue();
    }

    @Property(tries = 200)
    void property38_differentRecipientsHaveIndependentLimits(
            @ForAll("validEmails") String recipientA,
            @ForAll("validEmails") String recipientB) {
        // Skip if both resolve to the same key (case-insensitive)
        Assume.that(!recipientA.toLowerCase().trim().equals(recipientB.toLowerCase().trim()));

        EmailRateLimiterImpl limiter = new EmailRateLimiterImpl();

        // Exhaust limit for recipientA
        for (int i = 0; i < MAX_SENDS; i++) {
            limiter.recordSend(recipientA);
        }

        // recipientA should be blocked
        assertThat(limiter.isAllowed(recipientA)).isFalse();
        // recipientB should still be allowed
        assertThat(limiter.isAllowed(recipientB)).isTrue();
    }

    @Property(tries = 200)
    void property38_caseInsensitiveRecipientTracking(
            @ForAll("validEmails") String recipient) {
        EmailRateLimiterImpl limiter = new EmailRateLimiterImpl();

        // Send using different cases of the same recipient
        limiter.recordSend(recipient.toLowerCase());
        limiter.recordSend(recipient.toUpperCase());
        limiter.recordSend(recipient);

        // All three should count toward the same recipient
        assertThat(limiter.isAllowed(recipient)).isFalse();
        assertThat(limiter.isAllowed(recipient.toLowerCase())).isFalse();
        assertThat(limiter.isAllowed(recipient.toUpperCase())).isFalse();
    }

    @Property(tries = 100)
    void property38_nullAndBlankRecipientsAreRejected(
            @ForAll("blankStrings") String blankRecipient) {
        EmailRateLimiterImpl limiter = new EmailRateLimiterImpl();

        assertThat(limiter.isAllowed(blankRecipient)).isFalse();
    }

    @Property(tries = 100)
    void property38_nullRecipientIsRejected() {
        EmailRateLimiterImpl limiter = new EmailRateLimiterImpl();
        assertThat(limiter.isAllowed(null)).isFalse();
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> validEmails() {
        Arbitrary<String> localPart = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(15);
        Arbitrary<String> domain = Arbitraries.of(
                "example.com", "test.org", "mail.cn",
                "qq.com", "163.com", "gmail.com");
        return Combinators.combine(localPart, domain)
                .as((local, dom) -> local + "@" + dom);
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", "  ", "\t", " \t ");
    }
}
