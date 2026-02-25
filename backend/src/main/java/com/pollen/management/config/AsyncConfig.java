package com.pollen.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步、重试与定时任务配置。
 */
@Configuration
@EnableAsync
@EnableRetry
@EnableScheduling
public class AsyncConfig {
}
