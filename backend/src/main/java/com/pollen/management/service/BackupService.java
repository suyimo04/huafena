package com.pollen.management.service;

import com.pollen.management.entity.BackupRecord;

import java.util.List;

/**
 * 数据备份服务接口
 * 负责每日自动备份、旧备份清理、云存储同步
 */
public interface BackupService {

    /**
     * 执行每日数据备份（JPA 导出为 JSON）
     * @return 备份记录
     */
    BackupRecord executeDailyBackup();

    /**
     * 清理超过 30 天的旧备份
     */
    void cleanOldBackups();

    /**
     * 同步备份文件到云存储（预留接口）
     * @param backupRecord 备份记录
     */
    void syncToCloudStorage(BackupRecord backupRecord);

    /**
     * 获取备份记录列表，按创建时间降序
     * @return 备份记录列表
     */
    List<BackupRecord> listBackups();

    /**
     * 手动触发备份
     * @return 备份记录
     */
    BackupRecord manualBackup();
}
