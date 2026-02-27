# Implementation Plan: 薪酬周期管理

## Overview

基于现有薪资系统，增加 period 字段和周期管理功能。采用自底向上的实现顺序：数据层 → 服务层 → 控制层 → 前端，每步增量构建并验证。

## Tasks

- [x] 1. 数据层变更：SalaryRecord 实体和 Repository 扩展
  - [x] 1.1 扩展 SalaryRecord 实体，新增 period 字段
    - 在 `SalaryRecord.java` 中添加 `private String period` 字段，带 `@Column(nullable = false, length = 7)` 注解
    - 新增 PeriodUtils 工具类，包含 `currentPeriod()` 和 `isValidPeriod()` 方法
    - _Requirements: 1.1_
  - [x] 1.2 扩展 SalaryRecordRepository，新增按周期查询方法
    - 添加 `findByPeriod`、`findByPeriodAndArchivedFalse`、`findByUserIdAndPeriod`、`findDistinctPeriods`、`findDistinctActivePeriods`、`countByPeriod`、`existsByPeriodAndArchivedTrue` 方法
    - _Requirements: 1.3, 2.4, 3.1_
  - [x] 1.3 更新 schema.sql，新增 period 字段和联合唯一约束
    - 在 salary_records 表定义中添加 `period VARCHAR(7) NOT NULL DEFAULT '1970-01'` 字段
    - 添加 `UNIQUE KEY uk_user_period (user_id, period)` 约束
    - _Requirements: 1.1, 1.3_
  - [x] 1.4 编写 PeriodUtils 属性测试
    - **Property 1: 周期格式不变量**
    - **Validates: Requirements 1.1**

- [x] 2. 服务层变更：SalaryService 周期功能实现
  - [x] 2.1 新增 SalaryPeriodDTO 和 CreatePeriodRequest DTO
    - 创建 `SalaryPeriodDTO`（period, archived, recordCount）
    - 创建 `CreatePeriodRequest`（period 字段带 @Pattern 校验）
    - _Requirements: 2.4_
  - [x] 2.2 扩展 SalaryService 接口，新增周期相关方法签名
    - 添加 `createPeriod`、`getPeriodList`、`getLatestActivePeriod` 方法签名
    - 修改现有 `getSalaryMembers`、`calculateAndDistribute`、`batchSaveWithValidation`、`archiveSalaryRecords`、`generateSalaryReport` 方法签名，增加 period 参数
    - _Requirements: 2.1, 2.2, 2.4, 3.1, 6.1, 6.2_
  - [x] 2.3 实现 SalaryServiceImpl 中的 createPeriod 方法
    - 校验周期格式、检查周期是否已存在
    - 获取所有正式成员，为每人创建空白薪资记录（period 字段赋值）
    - _Requirements: 2.1, 2.2, 2.3_
  - [x] 2.4 实现 SalaryServiceImpl 中的 getPeriodList 和 getLatestActivePeriod 方法
    - getPeriodList：查询所有 distinct period，聚合归档状态和记录数量，按周期降序排列
    - getLatestActivePeriod：查询最新的未归档周期
    - _Requirements: 2.4, 3.2_
  - [x] 2.5 重构 SalaryServiceImpl 现有方法，增加 period 参数支持
    - getSalaryMembers(period)：按周期过滤记录
    - calculateAndDistribute(period)：仅计算指定周期的记录
    - batchSaveWithValidation(records, operatorId, period)：仅保存指定周期的记录
    - archiveSalaryRecords(operatorId, period)：仅归档指定周期的记录
    - generateSalaryReport(period)：仅生成指定周期的报表
    - 所有写操作增加已归档周期检查，已归档则抛出 BusinessException
    - _Requirements: 3.1, 4.1, 4.2, 6.1, 6.2, 6.3_
  - [x] 2.6 编写 createPeriod 属性测试
    - **Property 2: 周期创建初始化完整性**
    - **Validates: Requirements 1.2, 2.2**
  - [x] 2.7 编写用户-周期唯一性属性测试
    - **Property 3: 用户-周期唯一性**
    - **Validates: Requirements 1.3, 1.4, 2.3**
  - [x] 2.8 编写周期列表排序与状态属性测试
    - **Property 4: 周期列表排序与状态正确性**
    - **Validates: Requirements 2.4**
  - [x] 2.9 编写周期查询隔离性属性测试
    - **Property 5: 周期查询隔离性**
    - **Validates: Requirements 3.1, 6.1, 6.2**
  - [x] 2.10 编写最新活跃周期选择属性测试
    - **Property 6: 最新活跃周期选择正确性**
    - **Validates: Requirements 3.2**
  - [x] 2.11 编写按周期归档完整性属性测试
    - **Property 7: 按周期归档完整性**
    - **Validates: Requirements 4.1**
  - [x] 2.12 编写已归档周期写保护属性测试
    - **Property 8: 已归档周期写保护**
    - **Validates: Requirements 4.2, 6.3**

- [x] 3. Checkpoint - 后端核心功能验证
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. 控制层变更：SalaryController API 扩展
  - [x] 4.1 新增周期管理 API 端点
    - `GET /api/salary/periods` → 获取周期列表
    - `POST /api/salary/periods` → 创建新周期（接收 CreatePeriodRequest）
    - _Requirements: 2.1, 2.4_
  - [x] 4.2 修改现有 API 端点，增加 period 查询参数
    - `GET /api/salary/members?period=` → 按周期获取成员数据
    - `POST /api/salary/calculate-distribute?period=` → 按周期计算
    - `POST /api/salary/batch-save?period=` → 按周期批量保存
    - `POST /api/salary/archive?period=` → 按周期归档
    - `GET /api/salary/report?period=` → 按周期生成报表
    - period 参数可选，缺失时使用 getLatestActivePeriod() 默认值
    - _Requirements: 3.1, 6.1, 6.2_
  - [x] 4.3 编写 SalaryController 周期端点单元测试
    - 测试新增端点的请求/响应格式
    - 测试 period 参数缺失时的默认行为
    - _Requirements: 2.4, 3.1_

- [x] 5. 前端变更：周期选择器和 API 适配
  - [x] 5.1 扩展前端 API 客户端 salary.ts
    - 新增 `SalaryPeriodDTO` 接口定义
    - 新增 `getSalaryPeriods()` 和 `createSalaryPeriod(period)` API 函数
    - 修改现有 API 函数，增加可选 period 参数
    - _Requirements: 2.4, 5.1_
  - [x] 5.2 在 SalaryPage.vue 中实现周期选择器
    - 在工具栏中添加 el-select 周期选择器和"新建周期"按钮
    - 添加 `currentPeriod` 响应式状态和 `periods` 列表
    - 页面加载时获取周期列表，默认选中最新未归档周期
    - 切换周期时重新加载对应周期的成员数据
    - 已归档周期选项显示"已归档"标签
    - _Requirements: 3.2, 5.1, 5.2, 5.3, 5.4_
  - [x] 5.3 实现已归档周期的只读模式
    - 当选中已归档周期时，禁用"保存修改"、"计算薪资"、"归档"按钮
    - 禁用表格中的单元格编辑功能
    - 显示提示信息"该周期已归档，数据为只读"
    - _Requirements: 4.2, 6.3_
  - [x] 5.4 适配现有操作按钮传递 period 参数
    - 修改 handleBatchSave、handleCalculate、handleArchive 函数，传递 currentPeriod
    - 修改 fetchMembers 函数，传递 currentPeriod
    - _Requirements: 6.1, 6.2_
  - [x] 5.5 编写前端周期相关单元测试
    - 测试周期选择器切换逻辑
    - 测试已归档周期的 UI 禁用状态
    - 测试 API 调用中 period 参数的正确传递
    - _Requirements: 5.1, 5.2, 5.3_

- [x] 6. 数据迁移脚本
  - [x] 6.1 编写数据迁移逻辑
    - 在 schema.sql 中添加迁移注释说明
    - 实现迁移逻辑：`UPDATE salary_records SET period = DATE_FORMAT(created_at, '%Y-%m') WHERE period = '1970-01'`
    - 确保迁移后所有记录都有有效的 period 值
    - _Requirements: 7.1, 7.2_
  - [x] 6.2 编写数据迁移属性测试
    - **Property 9: 数据迁移周期赋值正确性**
    - **Validates: Requirements 7.1, 7.2**

- [x] 7. Final checkpoint - 全量测试验证
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
