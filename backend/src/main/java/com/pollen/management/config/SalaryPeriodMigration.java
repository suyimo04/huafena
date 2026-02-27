package com.pollen.management.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 薪酬周期数据迁移：将 period='1970-01' 的历史记录基于 created_at 赋值为实际月份。
 * 迁移完成后所有记录都应具有有效的 period 值（YYYY-MM 格式）。
 *
 * 使用 JDBC 直接执行 SQL，兼容 MySQL 和 H2。
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10) // 在 DataInitializer 之后执行
public class SalaryPeriodMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        migratePeriodValues();
        verifyMigration();
    }

    private void migratePeriodValues() {
        // 检查是否有需要迁移的记录
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'",
                Integer.class
        );

        if (count == null || count == 0) {
            log.info("薪酬周期迁移：无需迁移的记录");
            return;
        }

        log.info("薪酬周期迁移：发现 {} 条待迁移记录（period='1970-01'）", count);

        // 使用 FORMATDATETIME（H2 兼容）或 DATE_FORMAT（MySQL 兼容）
        // 先尝试 H2 语法，失败则回退到 MySQL 语法
        int updated;
        try {
            updated = jdbcTemplate.update(
                    "UPDATE salary_records SET period = FORMATDATETIME(created_at, 'yyyy-MM') " +
                    "WHERE period = '1970-01' AND created_at IS NOT NULL"
            );
        } catch (Exception e) {
            log.info("薪酬周期迁移：H2 语法不可用，尝试 MySQL 语法");
            updated = jdbcTemplate.update(
                    "UPDATE salary_records SET period = DATE_FORMAT(created_at, '%Y-%m') " +
                    "WHERE period = '1970-01' AND created_at IS NOT NULL"
            );
        }

        log.info("薪酬周期迁移：成功迁移 {} 条记录", updated);
    }

    private void verifyMigration() {
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'",
                Integer.class
        );

        if (remaining != null && remaining > 0) {
            log.warn("薪酬周期迁移验证：仍有 {} 条记录 period='1970-01'（可能 created_at 为 NULL）", remaining);
        } else {
            log.info("薪酬周期迁移验证：所有记录均已具有有效的 period 值");
        }
    }
}
