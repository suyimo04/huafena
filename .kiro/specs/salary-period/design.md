# 设计文档：薪酬周期管理

## 概述

本设计基于现有花粉小组管理系统（Spring Boot 3.x + JPA 后端，Vue 3 + Element Plus 前端），为薪资模块引入"薪酬周期"概念。核心变更包括：

1. 在 `salary_records` 表新增 `period` 字段（VARCHAR，格式 "YYYY-MM"），并添加 `(user_id, period)` 联合唯一约束
2. 扩展 `SalaryRecord` 实体，增加 period 字段
3. 扩展 `SalaryRecordRepository`，增加按周期查询的方法
4. 扩展 `SalaryService` 和 `SalaryServiceImpl`，所有查询/计算/保存/归档操作增加 period 参数
5. 扩展 `SalaryController`，API 端点增加 period 参数
6. 扩展 `SalaryPage.vue`，增加周期选择器组件和新建周期功能
7. 扩展 `salary.ts` 前端 API 客户端，增加 period 参数

设计遵循现有项目架构模式：Controller → Service → Repository → Entity，最小化变更范围，保持向后兼容。

## 架构

```mermaid
graph TB
    subgraph 前端 Vue 3
        A[SalaryPage.vue] --> PS[Period_Selector 周期选择器]
        A --> C[积分维度录入表格]
        A --> D[薪酬报表展示]
    end

    subgraph API 层
        E[SalaryController - 扩展 period 参数]
    end

    subgraph 服务层
        G[SalaryService - 扩展 period 参数]
    end

    subgraph 数据层
        K[(salary_records - 新增 period 字段 + 联合唯一约束)]
    end

    PS --> A
    A --> E
    E --> G
    G --> K
end
```

### 关键设计决策

1. **周期标识格式**：使用 "YYYY-MM" 字符串格式（如 "2025-07"），存储为 VARCHAR(7)。选择字符串而非 Date 类型，因为周期是逻辑概念而非精确时间点，字符串格式便于前端展示和 URL 传参
2. **周期与归档关系**：归档操作按周期执行。一个周期内的所有记录要么全部活跃，要么全部归档。复用现有 `archived` 和 `archivedAt` 字段，不新增周期状态表
3. **唯一约束**：通过数据库层面的 `(user_id, period)` 联合唯一约束防止重复记录，而非仅在应用层校验
4. **向后兼容**：现有无 period 的记录通过数据迁移脚本赋值默认周期（基于 `created_at` 月份）。period 字段设为 NOT NULL，迁移后所有记录都有值
5. **API 设计**：现有 API 端点增加可选的 `period` 查询参数。不传 period 时默认使用最新未归档周期，保持旧客户端兼容
6. **无独立周期表**：周期信息直接从 `salary_records` 表的 `period` 字段聚合查询（`SELECT DISTINCT period`），不新建周期管理表，保持简洁

## 组件与接口

### 1. SalaryRecordRepository（扩展）

在现有接口基础上新增按周期查询的方法：

```java
@Repository
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    // ... 现有方法保留 ...

    /** 按周期查询所有薪资记录 */
    List<SalaryRecord> findByPeriod(String period);

    /** 按周期查询未归档的薪资记录 */
    List<SalaryRecord> findByPeriodAndArchivedFalse(String period);

    /** 查询某用户在某周期的记录（唯一约束保证最多一条） */
    Optional<SalaryRecord> findByUserIdAndPeriod(Long userId, String period);

    /** 查询所有不同的周期值，用于周期列表 */
    @Query("SELECT DISTINCT sr.period FROM SalaryRecord sr ORDER BY sr.period DESC")
    List<String> findDistinctPeriods();

    /** 查询所有不同的未归档周期 */
    @Query("SELECT DISTINCT sr.period FROM SalaryRecord sr WHERE sr.archived = false ORDER BY sr.period DESC")
    List<String> findDistinctActivePeriods();

    /** 按周期查询记录数量 */
    long countByPeriod(String period);

    /** 检查某周期是否已归档（任一记录已归档即视为已归档） */
    boolean existsByPeriodAndArchivedTrue(String period);
}
```

### 2. SalaryService（扩展）

在现有接口基础上增加周期相关方法：

```java
public interface SalaryService {
    // ... 现有方法签名调整，增加 period 参数 ...

    /** 获取指定周期的薪资成员列表 */
    List<SalaryMemberDTO> getSalaryMembers(String period);

    /** 创建新的薪酬周期，为所有正式成员初始化空白记录 */
    List<SalaryRecord> createPeriod(String period);

    /** 获取所有薪酬周期列表（含状态信息） */
    List<SalaryPeriodDTO> getPeriodList();

    /** 获取最新的未归档周期标识 */
    String getLatestActivePeriod();

    /** 按周期执行薪酬计算和分配 */
    List<SalaryRecord> calculateAndDistribute(String period);

    /** 按周期批量保存 */
    BatchSaveResponse batchSaveWithValidation(List<SalaryRecord> records, Long operatorId, String period);

    /** 按周期归档 */
    int archiveSalaryRecords(Long operatorId, String period);

    /** 按周期生成报表 */
    SalaryReportDTO generateSalaryReport(String period);
}
```

### 3. SalaryController（扩展）

现有端点增加 `period` 查询参数：

```java
@RestController
@RequestMapping("/api/salary")
public class SalaryController {

    /** 获取薪酬周期列表 */
    GET  /api/salary/periods                    → List<SalaryPeriodDTO>

    /** 创建新的薪酬周期 */
    POST /api/salary/periods                    → List<SalaryRecord>
         Body: { "period": "2025-07" }

    /** 获取指定周期的成员薪资数据 */
    GET  /api/salary/members?period=2025-07     → List<SalaryMemberDTO>

    /** 按周期执行计算分配 */
    POST /api/salary/calculate-distribute?period=2025-07 → List<SalaryRecord>

    /** 按周期批量保存 */
    POST /api/salary/batch-save?period=2025-07  → BatchSaveResponse

    /** 按周期归档 */
    POST /api/salary/archive?period=2025-07     → Integer

    /** 按周期生成报表 */
    GET  /api/salary/report?period=2025-07      → SalaryReportDTO
}
```

### 4. 前端 API 客户端扩展（salary.ts）

```typescript
// 新增接口
export interface SalaryPeriodDTO {
  period: string       // "2025-07"
  archived: boolean
  recordCount: number
}

// 新增 API
export function getSalaryPeriods(): Promise<ApiResponse<SalaryPeriodDTO[]>>
export function createSalaryPeriod(period: string): Promise<ApiResponse<SalaryRecord[]>>

// 现有 API 增加 period 参数
export function getSalaryMembers(period?: string): Promise<ApiResponse<SalaryMemberDTO[]>>
export function batchSaveSalary(data: BatchSaveRequest & { period: string }): Promise<ApiResponse<BatchSaveResponse>>
export function archiveSalary(operatorId: number, period: string): Promise<ApiResponse<number>>
```

### 5. 前端 Period_Selector 组件

集成在 SalaryPage.vue 工具栏中，使用 Element Plus 的 `el-select` 组件：

```vue
<!-- 周期选择器 - 嵌入工具栏 -->
<el-select v-model="currentPeriod" @change="onPeriodChange" placeholder="选择周期">
  <el-option v-for="p in periods" :key="p.period" :value="p.period">
    {{ p.period }}
    <el-tag v-if="p.archived" size="small" type="info">已归档</el-tag>
  </el-option>
</el-select>
<el-button @click="handleCreatePeriod">新建周期</el-button>
```

## 数据模型

### 1. salary_records 表变更

```sql
-- 新增 period 字段
ALTER TABLE salary_records ADD COLUMN period VARCHAR(7) NOT NULL DEFAULT '1970-01' COMMENT '薪酬周期 YYYY-MM';

-- 数据迁移：基于 created_at 赋值
UPDATE salary_records SET period = DATE_FORMAT(created_at, '%Y-%m') WHERE period = '1970-01';

-- 添加联合唯一约束
ALTER TABLE salary_records ADD UNIQUE KEY uk_user_period (user_id, period);
```

### 2. SalaryRecord 实体扩展

在现有 `SalaryRecord.java` 中新增字段：

```java
@Column(nullable = false, length = 7)
private String period;  // 格式 "YYYY-MM"
```

### 3. SalaryPeriodDTO（新增）

```java
@Data
@AllArgsConstructor
public class SalaryPeriodDTO {
    private String period;      // "2025-07"
    private boolean archived;   // 该周期是否已归档
    private long recordCount;   // 该周期的记录数量
}
```

### 4. CreatePeriodRequest（新增）

```java
@Data
public class CreatePeriodRequest {
    @NotBlank
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "周期格式必须为 YYYY-MM")
    private String period;
}
```

### 5. 周期工具类

```java
public class PeriodUtils {
    /** 获取当前月份的周期标识 */
    public static String currentPeriod() {
        return YearMonth.now().toString();  // "2025-07"
    }

    /** 校验周期格式 */
    public static boolean isValidPeriod(String period) {
        return period != null && period.matches("\\d{4}-(0[1-9]|1[0-2])");
    }
}
```


## 正确性属性

*属性（Property）是指在系统所有合法执行路径中都应成立的特征或行为——本质上是对系统应做什么的形式化陈述。属性是人类可读规格说明与机器可验证正确性保证之间的桥梁。*

### Property 1: 周期格式不变量

*For any* SalaryRecord 实体，其 period 字段的值必须匹配正则表达式 `\d{4}-(0[1-9]|1[0-2])`（即合法的 "YYYY-MM" 格式）。

**Validates: Requirements 1.1**

### Property 2: 周期创建初始化完整性

*For any* 正式成员列表和合法的周期标识，创建新周期后，该周期内的薪资记录数量应等于正式成员数量，且每条记录的 period 字段等于指定周期，各维度积分字段均为 0。

**Validates: Requirements 1.2, 2.2**

### Property 3: 用户-周期唯一性

*For any* 用户 ID 和周期组合，系统中最多存在一条对应的薪资记录。尝试创建重复的用户-周期组合应被拒绝。

**Validates: Requirements 1.3, 1.4, 2.3**

### Property 4: 周期列表排序与状态正确性

*For any* 一组包含不同归档状态的薪酬周期，getPeriodList 返回的列表应按周期标识降序排列，且每个周期的 archived 状态应与该周期内记录的实际归档状态一致。

**Validates: Requirements 2.4**

### Property 5: 周期查询隔离性

*For any* 包含多个周期数据的系统状态，按周期 P 查询薪资记录时，返回的所有记录的 period 字段均等于 P，且不包含其他周期的记录。

**Validates: Requirements 3.1, 6.1, 6.2**

### Property 6: 最新活跃周期选择正确性

*For any* 一组包含活跃和已归档周期的系统状态，getLatestActivePeriod 返回的周期应是所有未归档周期中字典序最大的那个。

**Validates: Requirements 3.2**

### Property 7: 按周期归档完整性

*For any* 包含 N 条未归档记录的周期，执行归档操作后，该周期内所有 N 条记录的 archived 字段应为 true 且 archivedAt 应非空。

**Validates: Requirements 4.1**

### Property 8: 已归档周期写保护

*For any* 已归档的周期，尝试对该周期执行计算、保存或编辑操作应被拒绝并返回错误信息。

**Validates: Requirements 4.2, 6.3**

### Property 9: 数据迁移周期赋值正确性

*For any* 具有 created_at 时间戳的薪资记录，数据迁移后其 period 字段应等于 created_at 格式化为 "YYYY-MM" 的结果。

**Validates: Requirements 7.1, 7.2**

## 错误处理

| 错误场景 | 处理方式 |
|---|---|
| 周期格式不合法（非 YYYY-MM） | 返回 400 错误码，提示合法格式 |
| 创建已存在的周期 | 返回 409 错误码，提示周期已存在 |
| 同一用户同一周期重复记录 | 返回 409 错误码，提示该用户在该周期已有记录 |
| 对已归档周期执行写操作 | 返回 400 错误码，提示该周期已归档不可修改 |
| 查询不存在的周期 | 返回空列表，前端展示空状态 |
| period 参数缺失 | 使用最新未归档周期作为默认值 |
| 无任何周期存在 | 返回空列表，前端提示创建新周期 |

## 测试策略

### 属性测试（Property-Based Testing）

使用项目已有的 **jqwik 1.9.1** 库实现属性测试，每个属性测试至少运行 100 次迭代。

每个属性测试必须以注释标注对应的设计属性编号：
- 格式：`Feature: salary-period, Property {N}: {property_text}`

属性测试覆盖：
- Property 1: PeriodUtils 格式校验
- Property 2: createPeriod 初始化逻辑
- Property 3: 用户-周期唯一约束
- Property 4: 周期列表排序与状态
- Property 5: 按周期查询隔离性
- Property 6: 最新活跃周期选择
- Property 7: 按周期归档完整性
- Property 8: 已归档周期写保护
- Property 9: 数据迁移逻辑

### 单元测试

使用 JUnit 5 + Mockito，覆盖：
- PeriodUtils 的边界值（"2025-00"、"2025-13"、null、空字符串）
- SalaryController 新增端点的请求/响应格式
- SalaryServiceImpl 中周期相关方法的正常和异常路径
- 数据迁移脚本的边界情况（created_at 为 null 的记录）

### 前端测试

使用 Vitest，覆盖：
- 周期选择器的切换逻辑
- 已归档周期的 UI 禁用状态
- API 调用中 period 参数的正确传递
