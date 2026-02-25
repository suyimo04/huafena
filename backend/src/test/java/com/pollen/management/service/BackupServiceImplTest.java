package com.pollen.management.service;

import com.pollen.management.entity.BackupRecord;
import com.pollen.management.entity.enums.BackupStatus;
import com.pollen.management.entity.enums.BackupType;
import com.pollen.management.entity.enums.CloudSyncStatus;
import com.pollen.management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceImplTest {

    @Mock
    private BackupRecordRepository backupRecordRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private PointsRecordRepository pointsRecordRepository;
    @Mock
    private SalaryRecordRepository salaryRecordRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @InjectMocks
    private BackupServiceImpl backupService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(backupService, "backupDirectory", tempDir.toString());
        ReflectionTestUtils.setField(backupService, "retentionDays", 30);
    }

    private void stubRepositoriesEmpty() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(pointsRecordRepository.findAll()).thenReturn(Collections.emptyList());
        when(salaryRecordRepository.findAll()).thenReturn(Collections.emptyList());
        when(activityRepository.findAll()).thenReturn(Collections.emptyList());
        when(weeklyReportRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    void executeDailyBackup_shouldCreateJsonFileAndReturnSuccessRecord() {
        stubRepositoriesEmpty();
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> {
            BackupRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        BackupRecord result = backupService.executeDailyBackup();

        assertThat(result.getBackupType()).isEqualTo(BackupType.DAILY);
        assertThat(result.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(result.getFileName()).startsWith("backup_").endsWith(".json");
        assertThat(result.getFileSize()).isGreaterThan(0);
        assertThat(result.getErrorMessage()).isNull();

        // Verify the file was actually created
        Path backupFile = Path.of(result.getFilePath());
        assertThat(Files.exists(backupFile)).isTrue();
    }

    @Test
    void manualBackup_shouldCreateBackupWithManualType() {
        stubRepositoriesEmpty();
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> {
            BackupRecord r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        BackupRecord result = backupService.manualBackup();

        assertThat(result.getBackupType()).isEqualTo(BackupType.MANUAL);
        assertThat(result.getStatus()).isEqualTo(BackupStatus.SUCCESS);
    }

    @Test
    void executeDailyBackup_shouldSaveFailedRecordOnError() {
        // Simulate failure by making the repository throw during export
        when(userRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));
        ReflectionTestUtils.setField(backupService, "backupDirectory", tempDir.resolve("backup_fail_test").toString());
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> {
            BackupRecord r = inv.getArgument(0);
            r.setId(3L);
            return r;
        });

        BackupRecord result = backupService.executeDailyBackup();

        assertThat(result.getStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(result.getErrorMessage()).isNotNull();
        assertThat(result.getFileSize()).isZero();
    }

    @Test
    void cleanOldBackups_shouldDeleteOldRecordsAndFiles() throws IOException {
        // Create a temp file to simulate an old backup
        Path oldFile = tempDir.resolve("old_backup.json");
        Files.writeString(oldFile, "{}");

        BackupRecord oldRecord = BackupRecord.builder()
                .id(1L)
                .fileName("old_backup.json")
                .filePath(oldFile.toString())
                .status(BackupStatus.SUCCESS)
                .createdAt(LocalDateTime.now().minusDays(31))
                .build();

        when(backupRecordRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(oldRecord));

        backupService.cleanOldBackups();

        assertThat(Files.exists(oldFile)).isFalse();
        verify(backupRecordRepository).delete(oldRecord);
    }

    @Test
    void cleanOldBackups_shouldHandleMissingFilesGracefully() {
        BackupRecord oldRecord = BackupRecord.builder()
                .id(2L)
                .fileName("missing_backup.json")
                .filePath(tempDir.resolve("missing_backup.json").toString())
                .status(BackupStatus.SUCCESS)
                .createdAt(LocalDateTime.now().minusDays(31))
                .build();

        when(backupRecordRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(oldRecord));

        // Should not throw even if file doesn't exist
        backupService.cleanOldBackups();

        verify(backupRecordRepository).delete(oldRecord);
    }

    @Test
    void cleanOldBackups_shouldDoNothingWhenNoOldBackups() {
        when(backupRecordRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        backupService.cleanOldBackups();

        verify(backupRecordRepository, never()).delete(any(BackupRecord.class));
    }

    @Test
    void syncToCloudStorage_shouldLogAndNotThrow() {
        BackupRecord record = BackupRecord.builder()
                .id(1L)
                .fileName("test_backup.json")
                .build();

        // Should not throw - just logs "Cloud sync not configured"
        backupService.syncToCloudStorage(record);
    }

    @Test
    void listBackups_shouldReturnRecordsOrderedByCreatedAtDesc() {
        BackupRecord r1 = BackupRecord.builder().id(1L).createdAt(LocalDateTime.now().minusDays(1)).build();
        BackupRecord r2 = BackupRecord.builder().id(2L).createdAt(LocalDateTime.now()).build();
        when(backupRecordRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(r2, r1));

        List<BackupRecord> result = backupService.listBackups();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
    }

    @Test
    void listBackups_shouldReturnEmptyListWhenNoBackups() {
        when(backupRecordRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        List<BackupRecord> result = backupService.listBackups();

        assertThat(result).isEmpty();
    }

    @Test
    void executeDailyBackup_shouldSetCloudSyncStatusToPending() {
        stubRepositoriesEmpty();
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> {
            BackupRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        BackupRecord result = backupService.executeDailyBackup();

        assertThat(result.getCloudSyncStatus()).isEqualTo(CloudSyncStatus.PENDING);
    }

    @Test
    void executeDailyBackup_backupFileContainsValidJson() throws IOException {
        stubRepositoriesEmpty();
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> {
            BackupRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        BackupRecord result = backupService.executeDailyBackup();

        String content = Files.readString(Path.of(result.getFilePath()));
        assertThat(content).contains("\"exportTime\"");
        assertThat(content).contains("\"users\"");
        assertThat(content).contains("\"applications\"");
        assertThat(content).contains("\"pointsRecords\"");
        assertThat(content).contains("\"salaryRecords\"");
        assertThat(content).contains("\"activities\"");
        assertThat(content).contains("\"weeklyReports\"");
    }
}
