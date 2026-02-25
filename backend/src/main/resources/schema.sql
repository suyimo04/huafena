-- ============================================================
-- 花粉小组管理系统 - 数据库建表脚本
-- 数据库: pollen_management
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS pollen_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE pollen_management;

-- -----------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(255)    NOT NULL,
    password        VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL COMMENT 'ADMIN, LEADER, VICE_LEADER, MEMBER, INTERN, APPLICANT',
    enabled         TINYINT(1)      NOT NULL DEFAULT 0,
    pending_dismissal TINYINT(1)    NOT NULL DEFAULT 0,
    created_at      DATETIME        NULL,
    updated_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. 问卷模板表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS questionnaire_templates (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(255)    NOT NULL,
    description     VARCHAR(255)    NULL,
    active_version_id BIGINT        NULL,
    created_by      BIGINT          NOT NULL,
    created_at      DATETIME        NULL,
    updated_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷模板表';

-- -----------------------------------------------------------
-- 3. 问卷版本表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS questionnaire_versions (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    template_id     BIGINT          NOT NULL,
    version_number  INT             NOT NULL,
    schema_definition TEXT          NULL COMMENT '字段配置、分组、条件逻辑和验证规则 JSON',
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PUBLISHED',
    created_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷版本表';


-- -----------------------------------------------------------
-- 4. 问卷字段表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS questionnaire_fields (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    version_id      BIGINT          NOT NULL,
    field_key       VARCHAR(255)    NOT NULL,
    field_type      VARCHAR(20)     NOT NULL COMMENT 'SINGLE_CHOICE, MULTI_CHOICE, TEXT, DATE, NUMBER, DROPDOWN',
    label           VARCHAR(255)    NOT NULL,
    group_name      VARCHAR(255)    NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    required        TINYINT(1)      NOT NULL DEFAULT 0,
    validation_rules TEXT           NULL COMMENT '验证规则 JSON',
    options         TEXT            NULL COMMENT '选项 JSON',
    conditional_logic TEXT          NULL COMMENT '条件逻辑 JSON',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷字段表';

-- -----------------------------------------------------------
-- 5. 问卷回答表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS questionnaire_responses (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    version_id      BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    application_id  BIGINT          NULL,
    answers         TEXT            NOT NULL COMMENT '回答数据 JSON',
    submitted_at    DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷回答表';

-- -----------------------------------------------------------
-- 6. 公开链接表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS public_links (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    link_token      VARCHAR(255)    NOT NULL,
    template_id     BIGINT          NOT NULL,
    version_id      BIGINT          NOT NULL,
    created_by      BIGINT          NOT NULL,
    active          TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NULL,
    expires_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_link_token (link_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公开问卷链接表';

-- -----------------------------------------------------------
-- 7. 申请表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS applications (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING_INITIAL_REVIEW' COMMENT 'PENDING_INITIAL_REVIEW, INITIAL_REVIEW_PASSED, REJECTED, AI_INTERVIEW_IN_PROGRESS, PENDING_REVIEW, INTERN_OFFERED',
    entry_type      VARCHAR(20)     NOT NULL COMMENT 'REGISTRATION, PUBLIC_LINK',
    questionnaire_response_id BIGINT NULL,
    review_comment  VARCHAR(255)    NULL,
    reviewed_by     BIGINT          NULL,
    reviewed_at     DATETIME        NULL,
    created_at      DATETIME        NULL,
    updated_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入组申请表';

-- -----------------------------------------------------------
-- 8. 面试表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS interviews (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    application_id  BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED, IN_PROGRESS, COMPLETED, PENDING_REVIEW, REVIEWED',
    scenario_id     VARCHAR(255)    NULL,
    difficulty_level VARCHAR(255)   NULL,
    created_at      DATETIME        NULL,
    completed_at    DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试表';

-- -----------------------------------------------------------
-- 9. 面试消息表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS interview_messages (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    interview_id    BIGINT          NOT NULL,
    role            VARCHAR(255)    NOT NULL COMMENT '消息角色: AI / USER',
    content         TEXT            NOT NULL,
    timestamp       DATETIME        NOT NULL,
    time_limit_seconds INT          NULL DEFAULT 60,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试对话消息表';

-- -----------------------------------------------------------
-- 10. 面试报告表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS interview_reports (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    interview_id    BIGINT          NOT NULL,
    rule_familiarity INT            NOT NULL COMMENT '规则熟悉度 0-10',
    communication_score INT         NOT NULL COMMENT '沟通能力评分',
    pressure_score  INT             NOT NULL COMMENT '抗压能力评分',
    total_score     INT             NOT NULL COMMENT '总分',
    ai_comment      TEXT            NULL COMMENT 'AI 评语',
    reviewer_comment TEXT           NULL COMMENT '复审评语',
    review_result   VARCHAR(255)    NULL COMMENT '复审结果',
    suggested_mentor VARCHAR(255)   NULL COMMENT '建议实习导师',
    recommendation_label VARCHAR(255) NULL COMMENT '推荐标签',
    manual_approved TINYINT(1)      NULL,
    reviewed_at     DATETIME        NULL,
    created_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_id (interview_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试评估报告表';

-- -----------------------------------------------------------
-- 11. 积分记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS points_records (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    points_type     VARCHAR(30)     NOT NULL COMMENT 'COMMUNITY_ACTIVITY, CHECKIN, VIOLATION_HANDLING, TASK_COMPLETION, ANNOUNCEMENT, EVENT_HOSTING, BIRTHDAY_BONUS, MONTHLY_EXCELLENT',
    amount          INT             NOT NULL,
    description     VARCHAR(255)    NULL,
    created_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分记录表';

-- -----------------------------------------------------------
-- 12. 薪资记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS salary_records (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    base_points     INT             NOT NULL DEFAULT 0,
    bonus_points    INT             NOT NULL DEFAULT 0,
    deductions      INT             NOT NULL DEFAULT 0,
    total_points    INT             NOT NULL DEFAULT 0,
    mini_coins      INT             NOT NULL DEFAULT 0,
    salary_amount   DECIMAL(19,2)   NOT NULL DEFAULT 0.00,
    remark          VARCHAR(255)    NULL,
    version         INT             NULL COMMENT '乐观锁版本号',
    archived        TINYINT(1)      NOT NULL DEFAULT 0,
    archived_at     DATETIME        NULL,
    created_at      DATETIME        NULL,
    updated_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资记录表';

-- -----------------------------------------------------------
-- 13. 活动表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS activities (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            NULL,
    activity_time   DATETIME        NOT NULL,
    location        VARCHAR(255)    NULL,
    registration_count INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING' COMMENT 'UPCOMING, ONGOING, COMPLETED, ARCHIVED',
    created_by      BIGINT          NOT NULL,
    created_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

-- -----------------------------------------------------------
-- 14. 活动报名表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_registrations (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    activity_id     BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    checked_in      TINYINT(1)      NOT NULL DEFAULT 0,
    checked_in_at   DATETIME        NULL,
    registered_at   DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_user (activity_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

-- -----------------------------------------------------------
-- 15. 审计日志表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    operator_id     BIGINT          NOT NULL,
    operation_type  VARCHAR(255)    NOT NULL,
    operation_time  DATETIME        NOT NULL,
    operation_detail TEXT           NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
