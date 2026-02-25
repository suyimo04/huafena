# 设计文档：工资计算规则系统

## 概述

本设计基于现有花粉小组管理系统（Spring Boot 3.x + JPA 后端，Vue 3 + Element Plus 前端），对薪资计算模块进行增强。核心变更包括：

1. 新增 `salary_config` 数据库表存储可配置参数，替代现有硬编码常量
2. 扩展 `salary_records` 表，增加各积分维度明细字段
3. 重构 `SalaryServiceImpl` 计算引擎，实现基于维度的自动汇总逻辑
4. 重构 `SalaryPage.vue`，增加维度录入列和配置管理面板

设计遵循现有项目架构模式：Controller → Service → Repository → Entity，使用 Lombok、JPA、AES 加密等已有技术栈。

## 架构

```mermaid
graph TB
    subgraph 前端 Vue 3
        A[SalaryPage.vue] --> B[配置面板 ConfigPanel]
        A --> C[积分维度录入表格]
        A --> D[薪酬报表展示]
    end

    subgraph API 层
        E[SalaryController]
        F[SalaryConfigController]
    end

    subgraph 服务层
        G[SalaryService - 计算引擎]
        H[SalaryConfigService - 配置管理]
        I[PointsService - 积分服务]
        J[MemberRotationService - 流转服务]
    end

    subgraph 数据层
        K[(salary_records - 扩展)]
        L[(salary_config - 新增)]
        M[(points_records)]
        N[(users)]
    end

    A --> E
    B --> F
    E --> G
    F --> H
    G --> H
    G --> I
    J --> H
    G --> K
    H --> L
    I --> M
    G --> N
end
```

### 关键设计决策

1. **配置存储方案**：使用数据库表 `salary_config` 存储键值对配置，而非 application.yml，因为需要前端动态修改且无需重启服务
2. **积分维度扩展方案**：在 `salary_records` 表中新增各维度字段（如 `community_activity_points`、`checkin_count` 等），而非依赖 `points_records` 聚合查询，因为薪资记录需要快照当期数据
3. **计算触发方式**：前端录入时实时计算展示（前端本地计算），保存时后端重新计算验证（后端权威计算）
4. **向后兼容**：保留现有 `basePoints`、`bonusPoints`、`totalPoints`、`miniCoins` 字段，新增维度字段作为明细来源

## 组件与接口

### 1. SalaryConfigService（新增）

```java
public interface SalaryConfigService {
    /** 获取所有配置项 */
    Map<String, String> getAllConfig();

    /** 获取单个配置值，不存在时返回默认值 */
    String getConfigValue(String key, String defaultValue);

    /** 获取整数配置值 */
    int getIntConfig(String key, int defaultValue);

    /** 批量保存配置（含校验） */
    void saveConfig(Map<String, String> configMap);

    /** 获取薪酬池总额 */
    int getSalaryPoolTotal();

    /** 获取正式成员数量要求 */
    int getFormalMemberCount();

    /** 获取个人迷你币范围 [min, max] */
    int[] getMiniCoinsRange();

    /** 获取积分转迷你币比例 */
    int getPointsToCoinsRatio();

    /** 获取签到奖惩表配置 */
    List<CheckinTier> getCheckinTiers();

    /** 获取流转阈值配置 */
    RotationThresholds getRotationThresholds();
}
```

### 2. SalaryService（增强）

在现有接口基础上增加：

```java
public interface SalaryService {
    // ... 现有方法保留 ...

    /** 基于维度明细计算单个成员的积分汇总 */
    SalaryCalculationResult calculateMemberPoints(SalaryDimensionInput input);

    /** 执行薪酬池分配（含调剂） */
    List<SalaryRecord> calculateAndDistribute();
}
```

### 3. SalaryConfigController（新增）

```java
@RestController
@RequestMapping("/api/salary-config")
public class SalaryConfigController {
    GET  /api/salary-config          → 获取所有配置
    PUT  /api/salary-config          → 批量更新配置
    GET  /api/salary-config/checkin-tiers → 获取签到奖惩表
    PUT  /api/salary-config/checkin-tiers → 更新签到奖惩表
}
```

### 4. 前端 API 客户端（新增）

```typescript
// api/salaryConfig.ts
export function getSalaryConfig(): Promise<ApiResponse<Record<string, string>>>
export function updateSalaryConfig(config: Record<string, string>): Promise<ApiResponse<void>>
export function getCheckinTiers(): Promise<ApiResponse<CheckinTier[]>>
export function updateCheckinTiers(tiers: CheckinTier[]): Promise<ApiResponse<void>>
```

## 数据模型

### 1. salary_config 表（新增）

```sql
CREATE TABLE salary_config (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    config_key  VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at  DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资配置表';
```

默认配置项：

| config_key | config_value | description |
|---|---|---|
| salary_pool_total | 2000 | 薪酬池总额（迷你币） |
| formal_member_count | 5 | 正式成员数量 |
| base_allocation | 400 | 基准分配额（迷你币/人） |
| mini_coins_min | 200 | 个人最低迷你币 |
| mini_coins_max | 400 | 个人最高迷你币 |
| points_to_coins_ratio | 2 | 积分转迷你币比例 |
| promotion_points_threshold | 100 | 转正积分阈值 |
| demotion_salary_threshold | 150 | 降级薪酬阈值（积分） |
| demotion_consecutive_months | 2 | 降级检测连续月数 |
| dismissal_points_threshold | 100 | 开除积分阈值 |
| dismissal_consecutive_months | 2 | 开除检测连续月数 |
| checkin_tiers | JSON | 签到奖惩分级表 |

签到奖惩表 JSON 格式：
```json
[
  {"minCount": 0, "maxCount": 19, "points": -20, "label": "不合格"},
  {"minCount": 20, "maxCount": 29, "points": -10, "label": "需改进"},
  {"minCount": 30, "maxCount": 39, "points": 0, "label": "合格"},
  {"minCount": 40, "maxCount": 49, "points": 30, "label": "良好"},
  {"minCount": 50, "maxCount": 999, "points": 50, "label": "优秀"}
]
```

### 2. salary_records 表扩展字段

```sql
ALTER TABLE salary_records ADD COLUMN community_activity_points INT NOT NULL DEFAULT 0 COMMENT '社群活跃度积分 0-100';
ALTER TABLE salary_records ADD COLUMN checkin_count INT NOT NULL DEFAULT 0 COMMENT '月度签到次数';
ALTER TABLE salary_records ADD COLUMN checkin_points INT NOT NULL DEFAULT 0 COMMENT '签到计算积分';
ALTER TABLE salary_records ADD COLUMN violation_handling_count INT NOT NULL DEFAULT 0 COMMENT '违规处理次数';
ALTER TABLE salary_records ADD COLUMN violation_handling_points INT NOT NULL DEFAULT 0 COMMENT '违规处理积分';
ALTER TABLE salary_records ADD COLUMN task_completion_points INT NOT NULL DEFAULT 0 COMMENT '任务完成积分';
ALTER TABLE salary_records ADD COLUMN announcement_count INT NOT NULL DEFAULT 0 COMMENT '公告发布次数';
ALTER TABLE salary_records ADD COLUMN announcement_points INT NOT NULL DEFAULT 0 COMMENT '公告发布积分';
ALTER TABLE salary_records ADD COLUMN event_hosting_points INT NOT NULL DEFAULT 0 COMMENT '活动举办积分';
ALTER TABLE salary_records ADD COLUMN birthday_bonus_points INT NOT NULL DEFAULT 0 COMMENT '生日福利积分';
ALTER TABLE salary_records ADD COLUMN monthly_excellent_points INT NOT NULL DEFAULT 0 COMMENT '月度优秀评议积分';
```

### 3. SalaryConfig 实体（新增）

```java
@Entity
@Table(name = "salary_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(nullable = false, length = 500)
    private String configValue;

    private String description;
    private LocalDateTime updatedAt;
}
```

### 4. SalaryRecord 实体扩展

在现有 `SalaryRecord.java` 中新增字段：

```java
// 基础职责维度明细
private Integer communityActivityPoints = 0;  // 社群活跃度 0-100
private Integer checkinCount = 0;              // 签到次数
private Integer checkinPoints = 0;             // 签到计算积分
private Integer violationHandlingCount = 0;    // 违规处理次数
private Integer violationHandlingPoints = 0;   // 违规处理积分（count × 3）
private Integer taskCompletionPoints = 0;      // 任务完成积分
private Integer announcementCount = 0;         // 公告次数
private Integer announcementPoints = 0;        // 公告积分（count × 5）
private Integer eventHostingPoints = 0;        // 活动举办积分
private Integer birthdayBonusPoints = 0;       // 生日福利积分
private Integer monthlyExcellentPoints = 0;    // 月度优秀评议积分
```

### 5. 核心 DTO

```java
/** 签到奖惩分级 */
@Data
public class CheckinTier {
    private int minCount;
    private int maxCount;
    private int points;
    private String label;
}

/** 流转阈值配置 */
@Data
public class RotationThresholds {
    private int promotionPointsThreshold;
    private int demotionSalaryThreshold;
    private int demotionConsecutiveMonths;
    private int dismissalPointsThreshold;
    private int dismissalConsecutiveMonths;
}

/** 维度录入输入 */
@Data
public class SalaryDimensionInput {
    private Long userId;
    private int communityActivityPoints;
    private int checkinCount;
    private int violationHandlingCount;
    private int taskCompletionPoints;
    private int announcementCount;
    private int eventHostingPoints;
    private int birthdayBonusPoints;
    private int monthlyExcellentPoints;
}

/** 计算结果 */
@Data
public class SalaryCalculationResult {
    private int basePoints;       // 基础积分汇总
    private int bonusPoints;      // 奖励积分汇总
    private int totalPoints;      // 总积分
    private int miniCoins;        // 迷你币
    private int checkinPoints;    // 签到计算积分
    private int violationHandlingPoints; // 违规处理积分
    private int announcementPoints;      // 公告积分
    private String checkinLevel;  // 签到等级标记
}
```

### 6. 计算引擎核心逻辑

```
function calculateMemberPoints(input, config):
    // 基础职责积分
    checkinPoints = lookupCheckinTier(input.checkinCount, config.checkinTiers)
    violationPoints = input.violationHandlingCount × 3
    announcementPoints = input.announcementCount × 5

    basePoints = input.communityActivityPoints
               + checkinPoints
               + violationPoints
               + input.taskCompletionPoints
               + announcementPoints

    // 卓越贡献积分
    bonusPoints = input.eventHostingPoints
                + input.birthdayBonusPoints
                + input.monthlyExcellentPoints

    totalPoints = basePoints + bonusPoints
    miniCoins = totalPoints × config.pointsToCoinsRatio

    return { basePoints, bonusPoints, totalPoints, miniCoins, checkinPoints, ... }

function distributePool(memberResults, config):
    rawCoins = [each.miniCoins for each in memberResults]
    totalRaw = sum(rawCoins)

    if totalRaw > config.salaryPoolTotal:
        // 等比例缩减
        adjustedCoins = [coin × config.salaryPoolTotal / totalRaw for coin in rawCoins]
    else:
        adjustedCoins = rawCoins

    // 范围裁剪 + 调剂
    finalCoins = clipAndRedistribute(adjustedCoins, config.miniCoinsMin, config.miniCoinsMax)

    // 确保总额不超过薪酬池
    assert sum(finalCoins) <= config.salaryPoolTotal
    return finalCoins

function lookupCheckinTier(count, tiers):
    for tier in tiers:
        if tier.minCount <= count <= tier.maxCount:
            return tier.points
    return 0
```



## 正确性属性

*属性（Property）是指在系统所有合法执行路径中都应成立的特征或行为——本质上是对系统应做什么的形式化陈述。属性是人类可读规格说明与机器可验证正确性保证之间的桥梁。*

### Property 1: 配置读写一致性（Round-Trip）

*For any* 合法的配置键值对，保存到 Config_Manager 后再读取，应返回与保存时相同的值。

**Validates: Requirements 1.2**

### Property 2: 配置参数校验拒绝非法组合

*For any* 配置参数组合，若个人最低迷你币大于个人最高迷你币，或基准分配额乘以正式成员数量超过薪酬池总额，或流转阈值为负数，则 Config_Manager 应拒绝保存。

**Validates: Requirements 1.4, 1.5, 6.3**

### Property 3: 积分维度汇总计算正确性

*For any* 合法的积分维度输入，Calculation_Engine 计算的 basePoints 应等于社群活跃度 + 签到积分 + 违规处理积分 + 任务完成积分 + 公告积分之和，bonusPoints 应等于活动举办积分 + 生日福利积分 + 月度优秀评议积分之和，totalPoints 应等于 basePoints + bonusPoints。

**Validates: Requirements 2.3, 2.4, 2.5**

### Property 4: 积分维度输入范围校验

*For any* 积分维度输入值超出该维度允许范围（如社群活跃度超出 0-100），Calculation_Engine 应拒绝该输入。

**Validates: Requirements 2.6**

### Property 5: 签到奖惩分级查表正确性

*For any* 非负整数签到次数和合法的签到奖惩分级表，lookupCheckinTier 返回的积分值应与签到次数所落入的分级区间对应的积分值一致。

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

### Property 6: 积分转迷你币换算正确性

*For any* 总积分值和配置的换算比例，计算得到的迷你币应等于总积分乘以换算比例。

**Validates: Requirements 4.1, 5.1, 5.2**

### Property 7: 薪酬池分配比例保持性

*For any* 一组正式成员的原始迷你币列表，若总和超过薪酬池总额，则调整后各成员迷你币的相对比例应与原始比例一致（在整数舍入误差范围内）；若总和未超过薪酬池总额，则各成员迷你币应保持不变。

**Validates: Requirements 4.2, 4.3**

### Property 8: 薪酬分配范围与总额不变量

*For any* 薪酬池分配结果，每位正式成员的最终迷你币应在配置的 [最低值, 最高值] 范围内，且所有成员最终迷你币总和不超过薪酬池总额。

**Validates: Requirements 4.4, 4.6**

### Property 9: 薪酬报表完整性

*For any* 一组薪资记录，生成的薪酬报表应包含每位成员的所有积分维度明细字段，且报表中的已分配总额应等于所有成员迷你币之和，已分配总额加剩余额度应等于薪酬池总额。

**Validates: Requirements 7.1, 7.2, 7.3**

### Property 10: 归档操作完整性

*For any* 一组未归档的薪资记录，执行归档操作后，所有记录的 archived 字段应为 true 且 archivedAt 应非空。

**Validates: Requirements 7.4**

### Property 11: 流转阈值配置生效性

*For any* 新的流转阈值配置值，保存后 MemberRotationService 的检测逻辑应使用新配置的阈值而非硬编码值。

**Validates: Requirements 6.2**

## 错误处理

| 错误场景 | 处理方式 |
|---|---|
| 配置参数校验失败（min > max, allocation × count > pool） | 返回 400 错误码和具体校验失败信息，不保存任何配置 |
| 积分维度输入超出范围 | 返回 400 错误码，指明具体维度和合法范围 |
| 正式成员数量不符 | 返回 400 错误码，提示当前数量和要求数量 |
| 薪酬池分配后总额超限 | 内部重新调剂，确保不超限；若无法满足（如 min × count > pool），返回 400 错误 |
| 配置表不存在或为空 | 使用预定义默认值，不抛出异常 |
| 并发修改冲突（乐观锁） | 返回 409 错误码，提示刷新后重试 |
| 签到次数为负数 | 视为 0 次处理，归入最低分级 |

## 测试策略

### 属性测试（Property-Based Testing）

使用项目已有的 **jqwik 1.9.1** 库实现属性测试，每个属性测试至少运行 100 次迭代。

每个属性测试必须以注释标注对应的设计属性编号：
- 格式：`Feature: salary-calculation-rules, Property {N}: {property_text}`

属性测试覆盖：
- Property 1-2: SalaryConfigService 配置读写与校验
- Property 3-6: 计算引擎核心逻辑（维度汇总、签到查表、积分转换）
- Property 7-8: 薪酬池分配算法（比例保持、范围不变量）
- Property 9-10: 报表生成与归档
- Property 11: 流转阈值配置生效

### 单元测试

使用 JUnit 5 + Mockito，覆盖：
- 签到奖惩表各分级边界值（0, 19, 20, 29, 30, 39, 40, 49, 50）
- 配置默认值回退
- 薪酬池分配边界情况（所有人积分为 0、单人积分极高）
- 前端 API 接口的请求/响应格式验证

### 前端测试

使用 Vitest，覆盖：
- 积分维度录入后的本地计算逻辑
- 配置面板的表单校验
- API 调用的请求参数构造
