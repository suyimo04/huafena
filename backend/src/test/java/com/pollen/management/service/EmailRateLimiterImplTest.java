package com.pollen.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class EmailRateLimiterImplTest {

    private EmailRateLimiterImpl rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new EmailRateLimiterImpl();
    }

    @Test
    void isAllowed_shouldReturnTrueForNewRecipient() {
        assertTrue(rateLimiter.isAllowed("new@example.com"));
    }

    @Test
    void isAllowed_shouldReturnTrueWhenUnderLimit() {
        rateLimiter.recordSend("user@example.com");
        rateLimiter.recordSend("user@example.com");
        assertTrue(rateLimiter.isAllowed("user@example.com"));
    }

    @Test
    void isAllowed_shouldReturnFalseWhenAtLimit() {
        rateLimiter.recordSend("user@example.com");
        rateLimiter.recordSend("user@example.com");
        rateLimiter.recordSend("user@example.com");
        assertFalse(rateLimiter.isAllowed("user@example.com"));
    }

    @Test
    void isAllowed_shouldBeCaseInsensitive() {
        rateLimiter.recordSend("User@Example.COM");
        rateLimiter.recordSend("user@example.com");
        rateLimiter.recordSend("USER@EXAMPLE.COM");
        assertFalse(rateLimiter.isAllowed("user@example.com"));
    }

    @Test
    void isAllowed_shouldReturnFalseForNullRecipient() {
        assertFalse(rateLimiter.isAllowed(null));
    }

    @Test
    void isAllowed_shouldReturnFalseForBlankRecipient() {
        assertFalse(rateLimiter.isAllowed("  "));
    }

    @Test
    void recordSend_shouldIgnoreNullRecipient() {
        rateLimiter.recordSend(null);
        assertTrue(rateLimiter.getSendRecords().isEmpty());
    }

    @Test
    void recordSend_shouldIgnoreBlankRecipient() {
        rateLimiter.recordSend("  ");
        assertTrue(rateLimiter.getSendRecords().isEmpty());
    }

    @Test
    void isAllowed_shouldAllowAfterExpiredEntries() {
        // Manually insert old timestamps (older than 5 minutes)
        String key = "old@example.com";
        ConcurrentHashMap<String, Queue<Long>> records = rateLimiter.getSendRecords();
        Queue<Long> timestamps = new ConcurrentLinkedQueue<>();
        long fiveMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000L);
        timestamps.add(fiveMinutesAgo);
        timestamps.add(fiveMinutesAgo);
        timestamps.add(fiveMinutesAgo);
        records.put(key, timestamps);

        // Should be allowed because all entries are expired
        assertTrue(rateLimiter.isAllowed(key));
    }

    @Test
    void cleanup_shouldRemoveExpiredEntries() {
        String key = "cleanup@example.com";
        ConcurrentHashMap<String, Queue<Long>> records = rateLimiter.getSendRecords();
        Queue<Long> timestamps = new ConcurrentLinkedQueue<>();
        long sixMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000L);
        timestamps.add(sixMinutesAgo);
        timestamps.add(sixMinutesAgo);
        records.put(key, timestamps);

        rateLimiter.cleanup();

        // Entry should be removed entirely since all timestamps expired
        assertFalse(records.containsKey(key));
    }

    @Test
    void cleanup_shouldKeepRecentEntries() {
        rateLimiter.recordSend("recent@example.com");
        rateLimiter.cleanup();
        assertTrue(rateLimiter.getSendRecords().containsKey("recent@example.com"));
    }

    @Test
    void differentRecipients_shouldHaveIndependentLimits() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.recordSend("a@example.com");
        }
        assertFalse(rateLimiter.isAllowed("a@example.com"));
        assertTrue(rateLimiter.isAllowed("b@example.com"));
    }
}
