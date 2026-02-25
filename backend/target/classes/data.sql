-- ============================================================
-- 花粉小组管理系统 - 初始数据脚本
-- 注意: 密码使用 BCrypt 加密，明文均为 admin123
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- 如果通过 Spring Boot 启动，DataInitializer 会自动创建这些账户，
-- 此脚本用于手动初始化数据库时使用。
-- ============================================================

USE pollen_management;

-- -----------------------------------------------------------
-- 默认账户（4个）
-- admin/admin123  - 管理员
-- leader/admin123 - 组长
-- teacher/admin123 - 副组长
-- intern/admin123 - 实习成员
-- -----------------------------------------------------------
INSERT INTO users (username, password, role, enabled, pending_dismissal, created_at, updated_at)
SELECT 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 1, 0, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password, role, enabled, pending_dismissal, created_at, updated_at)
SELECT 'leader', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LEADER', 1, 0, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'leader');

INSERT INTO users (username, password, role, enabled, pending_dismissal, created_at, updated_at)
SELECT 'teacher', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'VICE_LEADER', 1, 0, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'teacher');

INSERT INTO users (username, password, role, enabled, pending_dismissal, created_at, updated_at)
SELECT 'intern', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'INTERN', 1, 0, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'intern');
