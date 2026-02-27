-- ============================================================
-- 花粉小组管理系统 V3.1 - 数据库建表脚本
-- 数据库: pollen_management
-- 字符集: utf8mb4
-- 策略: 先删除全部表，再统一创建
-- ============================================================

DROP DATABASE IF EXISTS pollen_management;

CREATE DATABASE pollen_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE pollen_management;

-- ============================================================
-- 删除全部表（按依赖关系倒序删除）
-- ============================================================
DROP TABLE IF EXISTS salary_config;
DROP TABLE IF EXISTS backup_record;
DROP TABLE IF EXISTS weekly_report;
DROP TABLE IF EXISTS internship_task;
DROP TABLE IF EXISTS internship;
DROP TABLE IF EXISTS email_config;
DROP TABLE IF EXISTS email_log;
DROP TABLE IF EXISTS email_template;
DROP TABLE IF EXISTS role_change_history;
DROP TABLE IF EXISTS user_activity_logs;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS activity_statistics;
DROP TABLE IF EXISTS activity_materials;
DROP TABLE IF EXISTS activity_feedback;
DROP TABLE IF EXISTS activity_groups;
DROP TABLE IF EXISTS activity_registrations;
DROP TABLE IF EXISTS activities;
DROP TABLE IF EXISTS salary_records;
DROP TABLE IF EXISTS points_records;
DROP TABLE IF EXISTS interview_reports;
DROP TABLE IF EXISTS interview_messages;
DROP TABLE IF EXISTS interviews;
DROP TABLE IF EXISTS application_timeline;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS public_links;
DROP TABLE IF EXISTS questionnaire_responses;
DROP TABLE IF EXISTS questionnaire_fields;
DROP TABLE IF EXISTS questionnaire_versions;
DROP TABLE IF EXISTS questionnaire_templates;
DROP TABLE IF EXISTS users;

-- ============================================================
-- 创建全部表
-- ============================================================

-- -----------------------------------------------------------
-- 1. 用户表（V3.1 增强：在线状态、最后活跃时间）
-- -----------------------------------------------------------
CREATE TABLE users (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    username          VARCHAR(255)    NOT NULL,
    password          VARCHAR(255)    NOT NULL,
    role              VARCHAR(20)     NOT NULL COMMENT 'ADMIN, LEADER, VICE_LEADER, MEMBER, INTERN, APPLICANT',
    enabled           TINYINT(1)      NOT NULL DEFAULT 0,
    pending_dismissal TINYINT(1)      NOT NULL DEFAULT 0,
    online_status     VARCHAR(20)     NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE, BUSY, OFFLINE',
    last_active_at    DATETIME        NULL,
    created_at        DATETIME        NULL,
    updated_at        DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. 问卷模板表
-- -----------------------------------------------------------
CREATE TABLE questionnaire_templates (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    title             VARCHAR(255)    NOT NULL,
    description       VARCHAR(255)    NULL,
    active_version_id BIGINT          NULL,
    created_by        BIGINT          NOT NULL,
    created_at        DATETIME        NULL,
    updated_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷模板表';

-- -----------------------------------------------------------
-- 3. 问卷版本表
-- -----------------------------------------------------------
CREATE TABLE questionnaire_versions (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    template_id       BIGINT          NOT NULL,
    version_number    INT             NOT NULL,
    schema_definition TEXT            NULL COMMENT '字段配置、分组、条件逻辑和验证规则 JSON',
    status            VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PUBLISHED',
    created_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷版本表';

-- -----------------------------------------------------------
-- 4. 问卷字段表
-- -----------------------------------------------------------
CREATE TABLE questionnaire_fields (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    version_id        BIGINT          NOT NULL,
    field_key         VARCHAR(255)    NOT NULL,
    field_type        VARCHAR(20)     NOT NULL COMMENT 'SINGLE_CHOICE, MULTI_CHOICE, TEXT, DATE, NUMBER, DROPDOWN',
    label             VARCHAR(255)    NOT NULL,
    group_name        VARCHAR(255)    NULL,
    sort_order        INT             NOT NULL DEFAULT 0,
    required          TINYINT(1)      NOT NULL DEFAULT 0,
    validation_rules  TEXT            NULL COMMENT '验证规则 JSON',
    options           TEXT            NULL COMMENT '选项 JSON',
    conditional_logic TEXT            NULL COMMENT '条件逻辑 JSON',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷字段表';

-- -----------------------------------------------------------
-- 5. 问卷回答表
-- -----------------------------------------------------------
CREATE TABLE questionnaire_responses (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    version_id        BIGINT          NOT NULL,
    user_id           BIGINT          NOT NULL,
    application_id    BIGINT          NULL,
    answers           TEXT            NOT NULL COMMENT '回答数据 JSON',
    submitted_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷回答表';

-- -----------------------------------------------------------
-- 6. 公开链接表
-- -----------------------------------------------------------
CREATE TABLE public_links (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    link_token        VARCHAR(255)    NOT NULL,
    template_id       BIGINT          NOT NULL,
    version_id        BIGINT          NOT NULL,
    created_by        BIGINT          NOT NULL,
    active            TINYINT(1)      NOT NULL DEFAULT 1,
    created_at        DATETIME        NULL,
    expires_at        DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_link_token (link_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公开问卷链接表';

-- -----------------------------------------------------------
-- 7. 申请表（V3.1 增强：报名表单增强与自动筛选字段）
-- -----------------------------------------------------------
CREATE TABLE applications (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT          NOT NULL,
    status                      VARCHAR(30)     NOT NULL DEFAULT 'PENDING_INITIAL_REVIEW' COMMENT 'PENDING_INITIAL_REVIEW, INITIAL_REVIEW_PASSED, REJECTED, AUTO_REJECTED, AI_INTERVIEW_IN_PROGRESS, PENDING_REVIEW, INTERN_OFFERED',
    entry_type                  VARCHAR(20)     NOT NULL COMMENT 'REGISTRATION, PUBLIC_LINK',
    questionnaire_response_id   BIGINT          NULL,
    review_comment              VARCHAR(255)    NULL,
    reviewed_by                 BIGINT          NULL,
    reviewed_at                 DATETIME        NULL,
    pollen_uid                  VARCHAR(50)     NULL COMMENT '花粉社区 UID（QQ号）',
    birth_date                  DATE            NULL COMMENT '出生年月',
    calculated_age              INT             NULL COMMENT '自动计算的年龄',
    education_stage             VARCHAR(20)     NULL COMMENT 'MIDDLE_SCHOOL, HIGH_SCHOOL, UNIVERSITY, GRADUATE, NON_STUDENT',
    exam_flag                   TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '中高考标识',
    exam_type                   VARCHAR(20)     NULL COMMENT 'ZHONGKAO, GAOKAO',
    exam_date                   DATE            NULL COMMENT '考试日期',
    weekly_available_slots      JSON            NULL COMMENT '每周可用时段',
    weekly_available_days       INT             NULL COMMENT '每周可用天数',
    daily_available_hours       DECIMAL(4,1)    NULL COMMENT '每日可用时长',
    screening_passed            TINYINT(1)      NULL COMMENT '自动筛选是否通过',
    screening_reject_reason     VARCHAR(200)    NULL COMMENT '自动筛选拒绝原因',
    needs_attention             TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否需要人工重点审核',
    attention_flags             JSON            NULL COMMENT '关注标记列表',
    created_at                  DATETIME        NULL,
    updated_at                  DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入组申请表';

-- -----------------------------------------------------------
-- 8. 申请流程时间线表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE application_timeline (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    application_id    BIGINT          NOT NULL,
    status            VARCHAR(50)     NOT NULL COMMENT '状态节点',
    operator          VARCHAR(100)    NULL COMMENT '操作人',
    description       VARCHAR(500)    NULL COMMENT '描述',
    created_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申请流程时间线表';

-- -----------------------------------------------------------
-- 9. 面试表
-- -----------------------------------------------------------
CREATE TABLE interviews (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    application_id    BIGINT          NOT NULL,
    user_id           BIGINT          NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED, IN_PROGRESS, COMPLETED, PENDING_REVIEW, REVIEWED',
    scenario_id       VARCHAR(255)    NULL,
    difficulty_level  VARCHAR(255)    NULL,
    created_at        DATETIME        NULL,
    completed_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试表';

-- -----------------------------------------------------------
-- 10. 面试消息表
-- -----------------------------------------------------------
CREATE TABLE interview_messages (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    interview_id        BIGINT          NOT NULL,
    role                VARCHAR(255)    NOT NULL COMMENT '消息角色: AI / USER',
    content             TEXT            NOT NULL,
    timestamp           DATETIME        NOT NULL,
    time_limit_seconds  INT             NULL DEFAULT 60,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试对话消息表';

-- -----------------------------------------------------------
-- 11. 面试报告表
-- -----------------------------------------------------------
CREATE TABLE interview_reports (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    interview_id          BIGINT          NOT NULL,
    rule_familiarity      INT             NOT NULL COMMENT '规则熟悉度 0-10',
    communication_score   INT             NOT NULL COMMENT '沟通能力评分',
    pressure_score        INT             NOT NULL COMMENT '抗压能力评分',
    total_score           INT             NOT NULL COMMENT '总分',
    ai_comment            TEXT            NULL COMMENT 'AI 评语',
    reviewer_comment      TEXT            NULL COMMENT '复审评语',
    review_result         VARCHAR(255)    NULL COMMENT '复审结果',
    suggested_mentor      VARCHAR(255)    NULL COMMENT '建议实习导师',
    recommendation_label  VARCHAR(255)    NULL COMMENT '推荐标签',
    manual_approved       TINYINT(1)      NULL,
    reviewed_at           DATETIME        NULL,
    created_at            DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_id (interview_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试评估报告表';

-- -----------------------------------------------------------
-- 12. 积分记录表
-- -----------------------------------------------------------
CREATE TABLE points_records (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    user_id           BIGINT          NOT NULL,
    points_type       VARCHAR(30)     NOT NULL COMMENT 'COMMUNITY_ACTIVITY, CHECKIN, VIOLATION_HANDLING, TASK_COMPLETION, ANNOUNCEMENT, EVENT_HOSTING, BIRTHDAY_BONUS, MONTHLY_EXCELLENT',
    amount            INT             NOT NULL,
    description       VARCHAR(255)    NULL,
    created_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分记录表';

-- -----------------------------------------------------------
-- 13. 薪资记录表（V3.1 增强：mini_coins 和 salary_amount 使用 AES 加密存储为 VARCHAR）
-- -----------------------------------------------------------
-- *** 数据迁移说明（薪酬周期功能上线时执行） ***
-- 背景：新增 period 字段（VARCHAR(7), DEFAULT '1970-01'）用于标识薪酬周期。
--       已有记录的 period 默认值为 '1970-01'，需基于 created_at 赋值为实际月份。
--
-- MySQL 迁移语句：
--   UPDATE salary_records SET period = DATE_FORMAT(created_at, '%Y-%m') WHERE period = '1970-01';
--
-- H2 迁移语句（开发/测试环境）：
--   UPDATE salary_records SET period = FORMATDATETIME(created_at, 'yyyy-MM') WHERE period = '1970-01';
--
-- 迁移后验证：
--   SELECT COUNT(*) FROM salary_records WHERE period = '1970-01';  -- 应返回 0
--   SELECT COUNT(*) FROM salary_records WHERE period IS NULL;       -- 应返回 0
-- *************************************************************
CREATE TABLE salary_records (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    user_id           BIGINT          NOT NULL,
    base_points       INT             NOT NULL DEFAULT 0,
    bonus_points      INT             NOT NULL DEFAULT 0,
    deductions        INT             NOT NULL DEFAULT 0,
    total_points      INT             NOT NULL DEFAULT 0,
    mini_coins        VARCHAR(500)    NOT NULL DEFAULT '0' COMMENT 'AES 加密存储',
    salary_amount     VARCHAR(500)    NOT NULL DEFAULT '0' COMMENT 'AES 加密存储',
    remark            VARCHAR(255)    NULL,
    version           INT             NULL COMMENT '乐观锁版本号',
    community_activity_points   INT NOT NULL DEFAULT 0 COMMENT '社群活跃度积分 0-100',
    checkin_count               INT NOT NULL DEFAULT 0 COMMENT '月度签到次数',
    checkin_points              INT NOT NULL DEFAULT 0 COMMENT '签到计算积分',
    violation_handling_count    INT NOT NULL DEFAULT 0 COMMENT '违规处理次数',
    violation_handling_points   INT NOT NULL DEFAULT 0 COMMENT '违规处理积分',
    task_completion_points      INT NOT NULL DEFAULT 0 COMMENT '任务完成积分',
    announcement_count          INT NOT NULL DEFAULT 0 COMMENT '公告发布次数',
    announcement_points         INT NOT NULL DEFAULT 0 COMMENT '公告发布积分',
    event_hosting_points        INT NOT NULL DEFAULT 0 COMMENT '活动举办积分',
    birthday_bonus_points       INT NOT NULL DEFAULT 0 COMMENT '生日福利积分',
    monthly_excellent_points    INT NOT NULL DEFAULT 0 COMMENT '月度优秀评议积分',
    period            VARCHAR(7)      NOT NULL DEFAULT '1970-01' COMMENT '薪酬周期 YYYY-MM',
    archived          TINYINT(1)      NOT NULL DEFAULT 0,
    archived_at       DATETIME        NULL,
    created_at        DATETIME        NULL,
    updated_at        DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_period (user_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资记录表';

-- -----------------------------------------------------------
-- 13.1 薪资配置表（V3.2 新增：薪酬池、签到分级、流转阈值等可配置参数）
-- -----------------------------------------------------------
CREATE TABLE salary_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    config_key   VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description  VARCHAR(255) NULL,
    updated_at   DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资配置表';

-- 默认配置数据
INSERT INTO salary_config (config_key, config_value, description, updated_at) VALUES
('salary_pool_total',          '2000', '薪酬池总额（迷你币）',       NOW()),
('formal_member_count',        '5',    '正式成员数量',               NOW()),
('base_allocation',            '400',  '基准分配额（迷你币/人）',    NOW()),
('mini_coins_min',             '200',  '个人最低迷你币',             NOW()),
('mini_coins_max',             '400',  '个人最高迷你币',             NOW()),
('points_to_coins_ratio',      '2',    '积分转迷你币比例',           NOW()),
('promotion_points_threshold', '100',  '转正积分阈值',               NOW()),
('demotion_salary_threshold',  '150',  '降级薪酬阈值（积分）',       NOW()),
('demotion_consecutive_months','2',    '降级检测连续月数',           NOW()),
('dismissal_points_threshold', '100',  '开除积分阈值',               NOW()),
('dismissal_consecutive_months','2',   '开除检测连续月数',           NOW()),
('checkin_tiers',              '[{"minCount":0,"maxCount":19,"points":-20,"label":"不合格"},{"minCount":20,"maxCount":29,"points":-10,"label":"需改进"},{"minCount":30,"maxCount":39,"points":0,"label":"合格"},{"minCount":40,"maxCount":49,"points":30,"label":"良好"},{"minCount":50,"maxCount":999,"points":50,"label":"优秀"}]', '签到奖惩分级表（JSON）', NOW());

-- -----------------------------------------------------------
-- 14. 活动表（V3.1 增强：封面图、类型、自定义表单、审核方式、二维码）
-- -----------------------------------------------------------
CREATE TABLE activities (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255)    NOT NULL,
    description         TEXT            NULL,
    cover_image_url     VARCHAR(500)    NULL COMMENT '活动封面图',
    activity_type       VARCHAR(20)     NULL COMMENT 'ONLINE, OFFLINE, TRAINING, TEAM_BUILDING, OTHER',
    custom_form_fields  JSON            NULL COMMENT '自定义报名表单字段',
    approval_mode       VARCHAR(20)     NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO, MANUAL',
    activity_time       DATETIME        NOT NULL,
    location            VARCHAR(255)    NULL,
    registration_count  INT             NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING' COMMENT 'UPCOMING, ONGOING, COMPLETED, ARCHIVED',
    qr_token            VARCHAR(64)     NULL COMMENT '签到二维码 Token',
    created_by          BIGINT          NOT NULL,
    created_at          DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

-- -----------------------------------------------------------
-- 15. 活动报名表（V3.1 增强：审核状态、额外字段）
-- -----------------------------------------------------------
CREATE TABLE activity_registrations (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    activity_id       BIGINT          NOT NULL,
    user_id           BIGINT          NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING, APPROVED, REJECTED',
    extra_fields      JSON            NULL COMMENT '自定义报名表单回答',
    checked_in        TINYINT(1)      NOT NULL DEFAULT 0,
    checked_in_at     DATETIME        NULL,
    registered_at     DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_user (activity_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

-- -----------------------------------------------------------
-- 16. 活动分组表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE activity_groups (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    activity_id       BIGINT          NOT NULL,
    group_name        VARCHAR(255)    NOT NULL,
    member_ids        JSON            NULL COMMENT '成员 ID 列表',
    created_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动分组表';

-- -----------------------------------------------------------
-- 17. 活动反馈表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE activity_feedback (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    activity_id       BIGINT          NOT NULL,
    user_id           BIGINT          NOT NULL,
    rating            INT             NOT NULL,
    comment           TEXT            NULL,
    created_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动反馈表';

-- -----------------------------------------------------------
-- 18. 活动资料表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE activity_materials (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    activity_id       BIGINT          NOT NULL,
    file_name         VARCHAR(255)    NOT NULL,
    file_url          VARCHAR(500)    NOT NULL,
    file_type         VARCHAR(255)    NULL,
    uploaded_by       BIGINT          NOT NULL,
    uploaded_at       DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动资料表';

-- -----------------------------------------------------------
-- 19. 活动统计表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE activity_statistics (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    activity_id           BIGINT          NOT NULL,
    total_registered      INT             NOT NULL DEFAULT 0,
    total_attended        INT             NOT NULL DEFAULT 0,
    check_in_rate         DECIMAL(5,2)    NULL DEFAULT 0.00,
    avg_feedback_rating   DECIMAL(3,2)    NULL DEFAULT 0.00,
    feedback_summary      JSON            NULL,
    generated_at          DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动统计表';

-- -----------------------------------------------------------
-- 20. 审计日志表
-- -----------------------------------------------------------
CREATE TABLE audit_logs (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    operator_id       BIGINT          NOT NULL,
    operation_type    VARCHAR(255)    NOT NULL,
    operation_time    DATETIME        NOT NULL,
    operation_detail  TEXT            NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- -----------------------------------------------------------
-- 21. 用户活跃日志表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE user_activity_logs (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    user_id           BIGINT          NOT NULL,
    action_type       VARCHAR(255)    NOT NULL,
    action_time       DATETIME        NOT NULL,
    duration_minutes  INT             NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户活跃日志表';

-- -----------------------------------------------------------
-- 22. 角色变更历史表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE role_change_history (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    user_id           BIGINT          NOT NULL,
    old_role          VARCHAR(20)     NOT NULL,
    new_role          VARCHAR(20)     NOT NULL,
    changed_by        VARCHAR(255)    NOT NULL,
    changed_at        DATETIME        NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色变更历史表';

-- -----------------------------------------------------------
-- 23. 邮件模板表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE email_template (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    template_code     VARCHAR(50)     NOT NULL COMMENT '模板编码',
    subject_template  VARCHAR(500)    NOT NULL COMMENT '邮件主题模板',
    body_template     TEXT            NOT NULL COMMENT 'HTML 邮件正文模板',
    created_at        DATETIME        NULL,
    updated_at        DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件模板表';

-- -----------------------------------------------------------
-- 24. 邮件发送日志表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE email_log (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    recipient         VARCHAR(200)    NOT NULL COMMENT '收件人',
    subject           VARCHAR(500)    NOT NULL COMMENT '邮件主题',
    template_code     VARCHAR(50)     NULL COMMENT '使用的模板编码',
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED',
    fail_reason       VARCHAR(1000)   NULL COMMENT '失败原因',
    retry_count       INT             NULL DEFAULT 0 COMMENT '重试次数',
    sent_at           DATETIME        NULL COMMENT '发送时间',
    created_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件发送日志表';

-- -----------------------------------------------------------
-- 25. 邮件 SMTP 配置表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE email_config (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    smtp_host                   VARCHAR(200)    NOT NULL,
    smtp_port                   INT             NOT NULL,
    smtp_username               VARCHAR(200)    NOT NULL,
    smtp_password_encrypted     VARCHAR(500)    NOT NULL COMMENT 'AES 加密存储',
    sender_name                 VARCHAR(100)    NULL,
    ssl_enabled                 TINYINT(1)      NULL DEFAULT 1,
    updated_at                  DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件 SMTP 配置表';

-- -----------------------------------------------------------
-- 26. 实习记录表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE internship (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,
    mentor_id           BIGINT          NULL COMMENT '导师 ID',
    start_date          DATE            NOT NULL,
    expected_end_date   DATE            NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS, PENDING_CONVERSION, PENDING_EVALUATION, CONVERTED, EXTENDED, TERMINATED',
    created_at          DATETIME        NULL,
    updated_at          DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实习记录表';

-- -----------------------------------------------------------
-- 27. 实习任务表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE internship_task (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    internship_id     BIGINT          NOT NULL,
    task_name         VARCHAR(200)    NOT NULL,
    task_description  TEXT            NULL,
    deadline          DATE            NOT NULL,
    completed         TINYINT(1)      NOT NULL DEFAULT 0,
    completed_at      DATETIME        NULL,
    created_at        DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实习任务表';

-- -----------------------------------------------------------
-- 28. 周报表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE weekly_report (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    week_start              DATE            NOT NULL,
    week_end                DATE            NOT NULL,
    new_applications        INT             NOT NULL DEFAULT 0,
    interviews_completed    INT             NOT NULL DEFAULT 0,
    new_members             INT             NOT NULL DEFAULT 0,
    activities_held         INT             NOT NULL DEFAULT 0,
    total_points_issued     INT             NOT NULL DEFAULT 0,
    detail_data             JSON            NULL COMMENT '详细数据',
    generated_at            DATETIME        NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周报表';

-- -----------------------------------------------------------
-- 29. 数据备份记录表（V3.1 新增）
-- -----------------------------------------------------------
CREATE TABLE backup_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    backup_type         VARCHAR(20)     NOT NULL COMMENT 'FULL, INCREMENTAL',
    file_name           VARCHAR(255)    NOT NULL,
    file_path           VARCHAR(255)    NOT NULL,
    file_size           BIGINT          NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL COMMENT 'SUCCESS, FAILED',
    error_message       VARCHAR(255)    NULL,
    cloud_sync_status   VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SYNCED, FAILED',
    created_at          DATETIME        NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据备份记录表';
