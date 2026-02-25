package com.pollen.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pollen.management.entity.BackupRecord;
import com.pollen.management.entity.enums.BackupStatus;
import com.pollen.management.entity.enums.BackupType;
import com.pollen.management.entity.enums.CloudSyncStatus;
import com.pollen.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupServiceImpl implements BackupService {

    private final BackupRecordRepository backupRecordRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final ActivityRepository activityRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Value("${backup.retention-days:30}")
    private int retentionDays;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public BackupRecord executeDailyBackup() {
        return executeBackup(BackupType.DAILY);
    }

    @Override
    public BackupRecord manualBackup() {
        return executeBackup(BackupType.MANUAL);
    }

    /**
     * 每日凌晨 3 点自动备份
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledBackup() {
        log.info("开始执行每日自动备份...");
        try {
            BackupRecord record = executeDailyBackup();
            cleanOldBackups();
            syncToCloudStorage(record);
            log.info("每日自动备份完成: {}", record.getFileName());
        } catch (Exception e) {
            log.error("每日自动备份失败", e);
        }
    }

    @Override
    public void cleanOldBackups() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<BackupRecord> oldBackups = backupRecordRepository.findByCreatedAtBefore(cutoffDate);

        for (BackupRecord record : oldBackups) {
            try {
                Path filePath = Paths.get(record.getFilePath());
                Files.deleteIfExists(filePath);
                log.info("已删除过期备份文件: {}", record.getFilePath());
            } catch (IOException e) {
                log.warn("删除备份文件失败: {}", record.getFilePath(), e);
            }
            backupRecordRepository.delete(record);
        }

        if (!oldBackups.isEmpty()) {
            log.info("已清理 {} 个超过 {} 天的旧备份", oldBackups.size(), retentionDays);
        }
    }

    @Override
    public void syncToCloudStorage(BackupRecord backupRecord) {
        // 预留接口：云存储同步尚未配置
        log.info("Cloud sync not configured - 云存储同步未配置，跳过同步备份: {}", backupRecord.getFileName());
    }

    @Override
    public List<BackupRecord> listBackups() {
        return backupRecordRepository.findAllByOrderByCreatedAtDesc();
    }

    private BackupRecord executeBackup(BackupType backupType) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = "backup_" + timestamp + ".json";
        Path filePath = null;

        try {
            Path dirPath = Paths.get(backupDirectory);
            filePath = dirPath.resolve(fileName);
            Files.createDirectories(dirPath);

            Map<String, Object> backupData = exportAllEntities();

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), backupData);

            long fileSize = Files.size(filePath);

            BackupRecord record = BackupRecord.builder()
                    .backupType(backupType)
                    .fileName(fileName)
                    .filePath(filePath.toString())
                    .fileSize(fileSize)
                    .status(BackupStatus.SUCCESS)
                    .cloudSyncStatus(CloudSyncStatus.PENDING)
                    .build();

            return backupRecordRepository.save(record);

        } catch (Exception e) {
            log.error("备份执行失败: {}", e.getMessage(), e);

            BackupRecord record = BackupRecord.builder()
                    .backupType(backupType)
                    .fileName(fileName)
                    .filePath(filePath != null ? filePath.toString() : backupDirectory + "/" + fileName)
                    .fileSize(0L)
                    .status(BackupStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .cloudSyncStatus(CloudSyncStatus.PENDING)
                    .build();

            return backupRecordRepository.save(record);
        }
    }

    private Map<String, Object> exportAllEntities() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportTime", LocalDateTime.now().toString());
        data.put("users", userRepository.findAll());
        data.put("applications", applicationRepository.findAll());
        data.put("pointsRecords", pointsRecordRepository.findAll());
        data.put("salaryRecords", salaryRecordRepository.findAll());
        data.put("activities", activityRepository.findAll());
        data.put("weeklyReports", weeklyReportRepository.findAll());
        return data;
    }
}
