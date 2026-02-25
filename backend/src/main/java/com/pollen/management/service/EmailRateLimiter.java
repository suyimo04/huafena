package com.pollen.management.service;

/**
 * 邮件防滥发限流器：5 分钟内同一收件人不超过 3 封。
 */
public interface EmailRateLimiter {

    /**
     * 检查是否允许向该收件人发送邮件。
     * @param recipient 收件人邮箱
     * @return true 表示允许发送，false 表示已超限
     */
    boolean isAllowed(String recipient);

    /**
     * 记录一次发送。
     * @param recipient 收件人邮箱
     */
    void recordSend(String recipient);
}
