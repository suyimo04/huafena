package com.pollen.management.service;

import com.pollen.management.entity.Internship;
import com.pollen.management.entity.InternshipTask;
import com.pollen.management.dto.CreateInternshipTaskRequest;
import com.pollen.management.dto.InternshipProgress;

/**
 * 实习期管理服务接口
 */
public interface InternshipService {

    /**
     * 为新实习成员自动创建实习记录，默认 30 天
     */
    Internship createForNewIntern(Long userId);

    /**
     * 根据 ID 获取实习记录
     */
    Internship getById(Long id);

    /**
     * 创建实习任务
     */
    InternshipTask createTask(Long internshipId, CreateInternshipTaskRequest request);

    /**
     * 完成实习任务
     */
    void completeTask(Long internshipId, Long taskId);

    /**
     * 指派导师（导师必须是 MEMBER 或 VICE_LEADER）
     */
    void assignMentor(Long internshipId, Long mentorId);

    /**
     * 获取实习进度（任务完成率、积分累计、导师评价、剩余天数）
     */
    InternshipProgress getProgress(Long internshipId);

    /**
     * 批准转正：角色变更为 MEMBER + 邮件通知
     */
    void approveConversion(Long internshipId);

    /**
     * 延期实习
     */
    void extendInternship(Long internshipId, int additionalDays);

    /**
     * 终止实习
     */
    void terminateInternship(Long internshipId);

    /**
     * 定时任务：检查实习期满且完成率 >= 80% 自动触发转正
     */
    void checkAndTriggerConversion();
}
