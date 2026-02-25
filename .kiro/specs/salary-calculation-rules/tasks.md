# 实现计划：工资计算规则系统

## 概述

基于现有 Spring Boot 3.x + JPA 后端和 Vue 3 + Element Plus 前端，增强薪资计算模块。实现配置管理、积分维度明细、自动计算引擎、薪酬池分配算法和前端配置界面。

## 任务

- [x] 1. 数据库与实体层扩展
  - [x] 1.1 创建 `salary_config` 表和 `SalaryConfig` 实体
    - 在 `schema.sql` 中添加 `salary_config` 建表语句
    - 创建 `SalaryConfig.java` 实体类（id, configKey, configValue, description, updatedAt）
    - 创建 `SalaryConfigRepository.java` 接口
    - 插入默认配置数据（薪酬池总额、成员数、浮动范围、签到分级表、流转阈值等）
    - _Requirements: 1.1, 1.3, 6.1_

  - [x] 1.2 扩展 `salary_records` 表和 `SalaryRecord` 实体
    - 在 `schema.sql` 中添加积分维度明细字段（community_activity_points, checkin_count, checkin_points, violation_handling_count, violation_handling_points, task_completion_points, announcement_count, announcement_points, event_hosting_points, birthday_bonus_points, monthly_excellent_points）
    - 在 `SalaryRecord.java` 实体中添加对应字段
    - _Requirements: 2.1, 2.2_

  - [x] 1.3 创建 DTO 类
    - 创建 `CheckinTier.java`（minCount, maxCount, points, label）
    - 创建 `RotationThresholds.java`（promotionPointsThreshold, demotionSalaryThreshold 等）
    - 创建 `SalaryDimensionInput.java`（各维度录入字段）
    - 创建 `SalaryCalculationResult.java`（basePoints, bonusPoints, totalPoints, miniCoins, checkinPoints 等）
    - _Requirements: 2.1, 2.2, 3.1-3.5_

- [x] 2. 配置管理服务
  - [x] 2.1 实现 `SalaryConfigService` 接口和 `SalaryConfigServiceImpl`
    - 实现 getAllConfig()、getConfigValue()、getIntConfig()、saveConfig()
    - 实现 getSalaryPoolTotal()、getFormalMemberCount()、getMiniCoinsRange()、getPointsToCoinsRatio()
    - 实现 getCheckinTiers()（解析 JSON 配置）和 getRotationThresholds()
    - 实现配置校验逻辑：min > max 拒绝、allocation × count > pool 拒绝、负数阈值拒绝
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.3_

  - [x] 2.2 编写配置服务属性测试
    - **Property 1: 配置读写一致性（Round-Trip）**
    - **Validates: Requirements 1.2**

  - [x] 2.3 编写配置校验属性测试
    - **Property 2: 配置参数校验拒绝非法组合**
    - **Validates: Requirements 1.4, 1.5, 6.3**

- [x] 3. 计算引擎核心逻辑
  - [x] 3.1 实现积分维度汇总计算方法 `calculateMemberPoints`
    - 基础积分 = 社群活跃度 + 签到积分 + 违规处理积分 + 任务完成积分 + 公告积分
    - 奖励积分 = 活动举办积分 + 生日福利积分 + 月度优秀评议积分
    - 总积分 = 基础积分 + 奖励积分
    - 签到积分通过 lookupCheckinTier 查表计算
    - 违规处理积分 = 次数 × 3，公告积分 = 次数 × 5
    - 积分维度输入范围校验（社群活跃度 0-100，任务完成 1-10/次 等）
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 3.1-3.5_

  - [x] 3.2 编写积分维度汇总属性测试
    - **Property 3: 积分维度汇总计算正确性**
    - **Validates: Requirements 2.3, 2.4, 2.5**

  - [x] 3.3 编写积分维度输入范围校验属性测试
    - **Property 4: 积分维度输入范围校验**
    - **Validates: Requirements 2.6**

  - [x] 3.4 编写签到奖惩分级查表属性测试
    - **Property 5: 签到奖惩分级查表正确性**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

  - [x] 3.5 编写积分转迷你币换算属性测试
    - **Property 6: 积分转迷你币换算正确性**
    - **Validates: Requirements 4.1, 5.1, 5.2**

- [x] 4. 薪酬池分配算法
  - [x] 4.1 重构 `adjustToPool` 和 `performanceAdjust` 方法使用配置参数
    - 从 SalaryConfigService 读取薪酬池总额、个人迷你币范围
    - 替代现有硬编码常量 SALARY_POOL_MINI_COINS=2000、范围 [200, 400]
    - 实现等比例缩减逻辑（总和超过薪酬池时）
    - 实现范围裁剪与调剂逻辑
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6_

  - [x] 4.2 编写薪酬池分配比例属性测试
    - **Property 7: 薪酬池分配比例保持性**
    - **Validates: Requirements 4.2, 4.3**

  - [x] 4.3 编写薪酬分配范围与总额不变量属性测试
    - **Property 8: 薪酬分配范围与总额不变量**
    - **Validates: Requirements 4.4, 4.6**

- [x] 5. Checkpoint - 确保后端核心逻辑测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [x] 6. 重构 MemberRotationService 使用配置阈值
  - [x] 6.1 修改 `MemberRotationServiceImpl` 从 SalaryConfigService 读取流转阈值
    - 替代硬编码常量 PROMOTION_POINTS_THRESHOLD、DEMOTION_SALARY_THRESHOLD、DEMOTION_CONSECUTIVE_MONTHS、DISMISSAL_POINTS_THRESHOLD、DISMISSAL_CONSECUTIVE_MONTHS
    - 注入 SalaryConfigService 依赖
    - _Requirements: 6.1, 6.2_

  - [x] 6.2 编写流转阈值配置生效属性测试
    - **Property 11: 流转阈值配置生效性**
    - **Validates: Requirements 6.2**

- [x] 7. 后端 API 层
  - [x] 7.1 创建 `SalaryConfigController`
    - GET /api/salary-config → 获取所有配置
    - PUT /api/salary-config → 批量更新配置（含校验）
    - GET /api/salary-config/checkin-tiers → 获取签到奖惩表
    - PUT /api/salary-config/checkin-tiers → 更新签到奖惩表
    - 权限控制：仅 ADMIN/LEADER 可修改
    - _Requirements: 1.1, 1.2, 3.6_

  - [x] 7.2 扩展 `SalaryController` 和 `SalaryService`
    - 新增 calculateAndDistribute 端点，整合维度计算 + 薪酬池分配
    - 扩展 getSalaryMembers 返回各维度明细字段
    - 扩展 batchSave 接收各维度明细字段
    - 扩展 generateSalaryReport 包含各维度明细
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 7.3 编写薪酬报表完整性属性测试
    - **Property 9: 薪酬报表完整性**
    - **Validates: Requirements 7.1, 7.2, 7.3**

  - [x] 7.4 编写归档操作完整性属性测试
    - **Property 10: 归档操作完整性**
    - **Validates: Requirements 7.4**

- [x] 8. Checkpoint - 确保后端全部测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [x] 9. 前端 API 客户端与类型定义
  - [x] 9.1 创建 `frontend/src/api/salaryConfig.ts`
    - 定义 CheckinTier、RotationThresholds、SalaryConfigMap 类型
    - 实现 getSalaryConfig、updateSalaryConfig、getCheckinTiers、updateCheckinTiers API 方法
    - _Requirements: 1.1, 1.2_

  - [x] 9.2 扩展 `frontend/src/api/salary.ts` 类型定义
    - 在 SalaryMemberDTO 中添加各积分维度明细字段
    - 在 SalaryRecord 中添加各积分维度明细字段
    - _Requirements: 2.1, 2.2_

- [x] 10. 前端薪资管理页面增强
  - [x] 10.1 重构 `SalaryPage.vue` 积分维度录入表格
    - 将现有 basePoints/bonusPoints/deductions 列替换为各积分维度明细列（社群活跃度、签到次数、违规处理次数、任务完成积分、公告次数、活动举办积分、生日福利积分、月度评议积分）
    - 实现前端本地实时计算：签到积分查表、违规处理积分=次数×3、公告积分=次数×5、基础积分汇总、奖励积分汇总、总积分、迷你币
    - 对实习成员仅展示积分记录列，隐藏薪酬相关列
    - 输入范围校验（超出范围显示红色边框和错误提示）
    - _Requirements: 8.1, 8.2, 8.4, 8.5, 2.6_

  - [x] 10.2 新增配置管理面板组件
    - 创建配置面板（可折叠/抽屉式），展示所有可配置参数
    - 薪酬池参数区：薪酬池总额、正式成员数量、基准分配额、个人最低/最高迷你币、换算比例
    - 签到奖惩表区：可编辑的分级表格（次数范围、积分、等级标记）
    - 流转阈值区：转正积分阈值、降级薪酬阈值、降级连续月数、开除积分阈值、开除连续月数
    - 保存时调用后端校验 API
    - _Requirements: 8.3, 1.1, 3.6, 6.1_

- [x] 11. Final Checkpoint - 确保前后端全部测试通过
  - 确保所有测试通过，如有问题请向用户确认。

## 备注

- 标记 `*` 的任务为可选测试任务，可跳过以加速 MVP 开发
- 每个任务引用了具体的需求编号以确保可追溯性
- 属性测试使用 jqwik 1.9.1，每个属性至少运行 100 次迭代
- 单元测试使用 JUnit 5 + Mockito
- 前端测试使用 Vitest
