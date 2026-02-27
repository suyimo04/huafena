package com.pollen.management.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryPeriodMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SalaryPeriodMigration migration;

    @Test
    void shouldSkipMigrationWhenNoRecordsNeedMigration() throws Exception {
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'"),
                eq(Integer.class)
        )).thenReturn(0);

        migration.run();

        verify(jdbcTemplate, never()).update(anyString());
    }

    @Test
    void shouldMigrateRecordsUsingH2SyntaxFirst() throws Exception {
        // First call: count for migration check
        // Second call: count for verification
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'"),
                eq(Integer.class)
        )).thenReturn(3).thenReturn(0);

        when(jdbcTemplate.update(
                eq("UPDATE salary_records SET period = FORMATDATETIME(created_at, 'yyyy-MM') " +
                   "WHERE period = '1970-01' AND created_at IS NOT NULL")
        )).thenReturn(3);

        migration.run();

        verify(jdbcTemplate).update(
                eq("UPDATE salary_records SET period = FORMATDATETIME(created_at, 'yyyy-MM') " +
                   "WHERE period = '1970-01' AND created_at IS NOT NULL")
        );
    }

    @Test
    void shouldFallbackToMySqlSyntaxWhenH2Fails() throws Exception {
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'"),
                eq(Integer.class)
        )).thenReturn(5).thenReturn(0);

        // H2 syntax fails
        when(jdbcTemplate.update(
                eq("UPDATE salary_records SET period = FORMATDATETIME(created_at, 'yyyy-MM') " +
                   "WHERE period = '1970-01' AND created_at IS NOT NULL")
        )).thenThrow(new RuntimeException("FORMATDATETIME not supported"));

        // MySQL syntax succeeds
        when(jdbcTemplate.update(
                eq("UPDATE salary_records SET period = DATE_FORMAT(created_at, '%Y-%m') " +
                   "WHERE period = '1970-01' AND created_at IS NOT NULL")
        )).thenReturn(5);

        migration.run();

        verify(jdbcTemplate).update(
                eq("UPDATE salary_records SET period = DATE_FORMAT(created_at, '%Y-%m') " +
                   "WHERE period = '1970-01' AND created_at IS NOT NULL")
        );
    }

    @Test
    void shouldVerifyNoRemainingRecordsAfterMigration() throws Exception {
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'"),
                eq(Integer.class)
        )).thenReturn(2).thenReturn(0);

        when(jdbcTemplate.update(anyString())).thenReturn(2);

        migration.run();

        // Verify the count query was called twice: once for migration check, once for verification
        verify(jdbcTemplate, times(2)).queryForObject(
                eq("SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'"),
                eq(Integer.class)
        );
    }

    @Test
    void shouldWarnWhenRecordsRemainAfterMigration() throws Exception {
        // Some records have null created_at, so they can't be migrated
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'"),
                eq(Integer.class)
        )).thenReturn(3).thenReturn(1);

        when(jdbcTemplate.update(anyString())).thenReturn(2);

        // Should not throw - just logs a warning
        migration.run();

        verify(jdbcTemplate, times(2)).queryForObject(
                eq("SELECT COUNT(*) FROM salary_records WHERE period = '1970-01'"),
                eq(Integer.class)
        );
    }
}
