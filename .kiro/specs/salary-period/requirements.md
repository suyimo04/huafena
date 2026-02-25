# 需求文档：薪酬周期管理

## 简介

本需求文档定义花粉小组管理系统中薪酬周期（Salary Period）功能的完整方案。现有薪资系统已具备积分维度录入、自动计算、薪酬池分配和归档功能，但缺少"月度周期"概念——所有薪资记录混在一起，无法按月区分和管理。本次增强将引入薪酬周期字段（格式如 "2025-01"），使每月的薪资数据独立管理，支持周期切换、历史查看、新周期创建，并与现有归档功能整合。

## 术语表

- **薪酬周期（Salary_Period）**：以"年-月"格式（如 "2025-07"）标识的月度薪资统计周期
- **当前周期（Current_Period）**：系统当前正在编辑和录入数据的薪酬周期
- **历史周期（Historical_Period）**：已归档的过往薪酬周期，数据为只读状态
- **周期选择器（Period_Selector）**：前端用于切换和选择薪酬周期的 UI 组件
- **薪资管理页面（SalaryPage）**：现有的薪资管理前端页面
- **薪资记录（Salary_Record）**：存储在 salary_records 表中的单条成员薪资数据
- **计算引擎（Calculation_Engine）**：后端自动计算积分和迷你币的服务模块

## 需求

### 需求 1：薪资记录关联薪酬周期

**用户故事：** 作为管理员，我希望每条薪资记录都关联一个薪酬周期，以便按月份区分和管理薪资数据。

#### 验收标准

1. THE Salary_Record SHALL 包含一个 period 字段，格式为 "YYYY-MM"（如 "2025-07"），用于标识该记录所属的薪酬周期
2. WHEN 创建新的薪资记录时，THE Calculation_Engine SHALL 自动将当前周期值赋给 period 字段
3. THE Salary_Record SHALL 对同一用户在同一薪酬周期内仅允许存在一条记录，通过 user_id 和 period 的联合唯一约束保证
4. IF 尝试为同一用户在同一周期内创建重复记录，THEN THE Calculation_Engine SHALL 拒绝创建并返回重复记录错误信息

### 需求 2：薪酬周期的创建与管理

**用户故事：** 作为管理员，我希望能够创建新的薪酬周期并管理周期列表，以便按月组织薪资录入工作。

#### 验收标准

1. WHEN 管理员请求创建新周期时，THE Calculation_Engine SHALL 基于当前日期自动生成当月的周期标识（格式 "YYYY-MM"）
2. WHEN 管理员创建新周期时，THE Calculation_Engine SHALL 为所有当前正式成员自动初始化该周期的空白薪资记录（各维度积分默认为 0）
3. IF 管理员尝试创建已存在的薪酬周期，THEN THE Calculation_Engine SHALL 拒绝创建并返回周期已存在的错误信息
4. WHEN 管理员请求周期列表时，THE Calculation_Engine SHALL 返回所有已存在的薪酬周期，按时间倒序排列，并标注每个周期的状态（活跃或已归档）

### 需求 3：按周期查询薪资数据

**用户故事：** 作为管理员，我希望按薪酬周期查询和查看薪资数据，以便分别管理每个月的薪资信息。

#### 验收标准

1. WHEN 管理员选择某个薪酬周期时，THE SalaryPage SHALL 仅展示该周期内的薪资记录
2. WHEN 管理员打开薪资管理页面时，THE SalaryPage SHALL 默认选中最新的未归档周期
3. WHEN 查询的周期不存在任何薪资记录时，THE SalaryPage SHALL 展示空状态提示并提供创建新周期的入口

### 需求 4：薪酬周期与归档功能整合

**用户故事：** 作为管理员，我希望归档操作以薪酬周期为单位执行，以便完整地归档一个月的薪资数据。

#### 验收标准

1. WHEN 管理员执行归档操作时，THE Calculation_Engine SHALL 将当前选中周期内的所有薪资记录标记为已归档
2. WHILE 某个薪酬周期处于已归档状态时，THE SalaryPage SHALL 将该周期的薪资数据设为只读，禁止编辑和重新计算
3. WHILE 某个薪酬周期处于已归档状态时，THE Period_Selector SHALL 以视觉标记（如灰色或归档图标）区分已归档周期和活跃周期

### 需求 5：前端周期选择器

**用户故事：** 作为管理员，我希望在薪资管理页面通过周期选择器快速切换不同月份的薪资数据，以便高效地进行薪资管理。

#### 验收标准

1. THE Period_Selector SHALL 以下拉选择器形式展示在薪资管理页面工具栏中，显示所有可用的薪酬周期
2. WHEN 管理员切换周期时，THE SalaryPage SHALL 重新加载对应周期的薪资数据，并更新表格展示
3. THE Period_Selector SHALL 在每个周期选项旁展示该周期的状态标签（活跃/已归档）
4. WHEN 管理员点击"新建周期"按钮时，THE Period_Selector SHALL 触发新周期创建流程并在创建成功后自动切换到新周期

### 需求 6：薪酬计算与批量保存的周期隔离

**用户故事：** 作为管理员，我希望薪酬计算和批量保存操作仅作用于当前选中的周期，以便避免跨周期数据混淆。

#### 验收标准

1. WHEN 执行薪酬计算时，THE Calculation_Engine SHALL 仅对当前选中周期内的薪资记录执行计算和薪酬池分配
2. WHEN 执行批量保存时，THE Calculation_Engine SHALL 仅保存当前选中周期内的薪资记录修改
3. IF 当前选中的周期已归档，THEN THE SalaryPage SHALL 禁用计算、保存和编辑操作按钮，并展示提示信息

### 需求 7：历史周期数据迁移

**用户故事：** 作为管理员，我希望现有的无周期薪资记录能够平滑迁移到周期体系中，以便保持数据完整性。

#### 验收标准

1. WHEN 系统升级部署时，THE Calculation_Engine SHALL 将所有现有无 period 字段的薪资记录自动赋值为一个默认周期标识（基于记录的 created_at 月份）
2. WHEN 数据迁移完成后，THE Salary_Record SHALL 确保所有记录都具有有效的 period 值，不存在空值
