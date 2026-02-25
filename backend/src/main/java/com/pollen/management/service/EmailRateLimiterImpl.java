package com.pollen.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 基于内存的邮件防滥发限流器。
 * 规则：5 分钟内同一收件人不超过 3 封。
 */
@Component
@Slf4j
public class EmailRateLimiterImpl implements EmailRateLimiter {

    static final int MAX_SENDS = 3;
    static final long WINDOW_MILLIS = 5 * 60 * 1000L; // 5 minutes

    private final ConcurrentHashMap<String, Queue<Long>> sendRecords = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return false;
        }
        String key = recipient.toLowerCase().trim();
        Queue<Long> timestamps = sendRecords.get(key);
        if (timestamps == null) {
            return true;
        }
        long cutoff = Instant.now().toEpochMilli() - WINDOW_MILLIS;
        purgeExpired(timestamps, cutoff);
        return timestamps.size() < MAX_SENDS;
    }

    @Override
    public void recordSend(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        String key = recipient.toLowerCase().trim();
        sendRecords.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>())
                .add(Instant.now().toEpochMilli());
    }

    /**
     * 定时清理过期记录，每 5 分钟执行一次。
     */
    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        long cutoff = Instant.now().toEpochMilli() - WINDOW_MILLIS;
        Iterator<Map.Entry<String, Queue<Long>>> it = sendRecords.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Queue<Long>> entry = it.next();
            purgeExpired(entry.getValue(), cutoff);
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    private void purgeExpired(Queue<Long> timestamps, long cutoff) {
        while (!timestamps.isEmpty() && timestamps.peek() < cutoff) {
            timestamps.poll();
        }
    }

    // Visible for testing
    ConcurrentHashMap<String, Queue<Long>> getSendRecords() {
        return sendRecords;
    }
}
