# 实施计划：动态问卷报名系统

## 概述

基于现有 Spring Boot + Vue 3 项目，修复问卷设计器保存 bug，新建通用问卷渲染组件，增强注册报名和公开链接报名流程，添加报名列表分页筛选功能。

## 任务

- [ ] 1. 修复问卷设计器保存功能（QuestionnairesPage.vue）
  - [~] 1.1 重构 QuestionnairesPage.vue，添加模板列表加载、模板选择、@save/@publish/@selectVersion 事件处理
    - 添加 templateList 状态和 selectedTemplateId
    - 添加 designerRef 引用 QuestionnaireDesigner 实例
    - 实现 handleSave：从 designerRef 获取 fields/groups，构建 QuestionnaireSchema，调用 updateTemplate API
    - 实现 handlePublish：调用 publishVersion API
    - 实现 handleSelectVersion：调用 getVersion API 并加载 schema 到设计器
    - 页面加载时调用 getTemplates 获取模板列表
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [~] 1.2 编写 QuestionnairesPage.vue 单元测试
    - 测试 @save 事件触发 API 调用
    - 测试 @publish 事件触发发布 API
    - 测试保存失败时保留编辑状态
    - _Requirements: 1.1, 1.2, 1.5_

- [ ] 2. 新建 QuestionnaireRenderer 通用问卷渲染组件
  - [ ] 2.1 创建 QuestionnaireRenderer.vue 组件
    - 接收 schema（QuestionnaireSchema）和 readonly props
    - 根据 schema.groups 按 sortOrder 排序渲染字段分组
    - 未分组字段排列在最后
    - 根据 fieldType 渲染对应的 Element Plus 表单控件（el-input、el-input-number、el-radio-group、el-checkbox-group、el-select、el-date-picker）
    - 使用 evaluateConditionalLogic 实时计算字段可见性
    - 使用 validateField 在提交前校验所有可见必填字段
    - 校验失败时高亮错误字段并显示错误信息
    - 校验通过时 emit submit 事件传递 answers
    - _Requirements: 2.1, 3.3, 6.3, 6.4_

  - [ ] 2.2 编写 Property 10 属性测试：问卷字段按分组排序渲染
    - **Property 10: 问卷字段按分组排序渲染**
    - 使用 fast-check 生成随机 QuestionnaireSchema
    - 验证渲染顺序遵循分组 sortOrder，未分组字段在最后
    - **Validates: Requirements 3.3**

  - [ ] 2.3 编写 Property 12 属性测试：仅校验可见必填字段
    - **Property 12: 仅校验可见必填字段**
    - 使用 fast-check 生成随机 schema 和 answers
    - 验证校验仅对可见且必填的字段执行
    - **Validates: Requirements 6.4**

- [ ] 3. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [ ] 4. 增强注册报名入口
  - [ ] 4.1 修改 RegisterPage.vue，集成 QuestionnaireRenderer
    - 页面加载时调用 API 获取活跃问卷版本的 schema
    - 替换当前的问卷占位区域为 QuestionnaireRenderer 组件
    - 注册提交时将问卷 answers 一并发送到后端
    - _Requirements: 6.1, 6.2, 6.5_

  - [ ] 4.2 修改 RegisterRequest DTO 和 AuthService，支持问卷回答
    - RegisterRequest 新增 questionnaireAnswers 字段（Map<String, Object>）
    - AuthService.register() 增强：创建用户后获取活跃问卷版本，校验问卷回答，保存 QuestionnaireResponse，创建 Application
    - _Requirements: 6.1, 6.4_

  - [ ] 4.3 编写 Property 5 属性测试：报名创建不变量
    - **Property 5: 报名创建不变量**
    - 使用 jqwik 生成随机注册数据和问卷回答
    - 验证创建后用户角色为 APPLICANT、enabled=false、申请状态为 PENDING_INITIAL_REVIEW
    - **Validates: Requirements 6.1, 7.2**

- [ ] 5. 增强公开链接报名入口
  - [ ] 5.1 修改 PublicQuestionnairePage.vue，集成 QuestionnaireRenderer
    - 根据路由参数 linkToken 调用 getPublicQuestionnaire API 获取 schema
    - 使用 QuestionnaireRenderer 渲染问卷
    - 提交时调用 submitPublicQuestionnaire API
    - 成功后显示自动生成的用户名和密码
    - 链接无效时显示错误提示页
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 6. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [ ] 7. 前端属性测试：条件逻辑和序列化
  - [ ] 7.1 编写 Property 1 前端属性测试：QuestionnaireSchema 序列化往返一致性
    - **Property 1: QuestionnaireSchema 序列化往返一致性**
    - 使用 fast-check 生成随机 QuestionnaireSchema
    - 验证 JSON.stringify → JSON.parse 往返一致
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [ ] 7.2 编写 Property 2 属性测试：条件逻辑可见性计算
    - **Property 2: 条件逻辑可见性计算**
    - 使用 fast-check 生成随机 ConditionalLogic 和 answers
    - 验证 visible = (action == "SHOW") == conditionsMet
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4**

  - [ ] 7.3 编写 Property 3 属性测试：条件逻辑 AND/OR 运算符
    - **Property 3: 条件逻辑 AND/OR 运算符**
    - 使用 fast-check 生成随机条件集合和 answers
    - 验证 AND 要求全部满足，OR 要求至少一个满足
    - **Validates: Requirements 9.5, 9.6**

- [ ] 8. 后端属性测试：字段校验引擎
  - [ ] 8.1 编写 Property 4 属性测试：字段校验引擎正确性
    - **Property 4: 字段校验引擎正确性**
    - 使用 jqwik 生成随机字段配置和输入值
    - 验证必填校验、文本长度校验、数值范围校验、日期范围校验、多选数量校验的正确性
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

- [ ] 9. 后端报名列表分页筛选
  - [ ] 9.1 创建 ApplicationSpecification 工具类
    - 实现 withFilters 静态方法，构建动态查询 Specification
    - 支持 status、minAge、maxAge、educationStage、examFlag、examType 筛选条件
    - _Requirements: 11.2, 11.3, 11.4, 11.5, 11.7_

  - [ ] 9.2 修改 ApplicationRepository 继承 JpaSpecificationExecutor
    - _Requirements: 11.2_

  - [ ] 9.3 创建 ApplicationListItemDTO
    - 包含 id、userId、username、status、entryType、pollenUid、calculatedAge、educationStage、examFlag、examType、needsAttention、createdAt
    - _Requirements: 11.1_

  - [ ] 9.4 在 ApplicationService 中添加 listByPage 方法并实现
    - 使用 ApplicationSpecification 构建查询条件
    - 使用 PageRequest.of(page, size, Sort.by("createdAt").descending()) 分页排序
    - 将 Application 转换为 ApplicationListItemDTO（关联查询 username）
    - _Requirements: 11.1, 11.6_

  - [ ] 9.5 在 ApplicationController 中添加 /page GET 接口
    - 接收分页参数和筛选参数
    - 调用 ApplicationService.listByPage
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

  - [ ] 9.6 编写 Property 7 属性测试：报名列表筛选正确性
    - **Property 7: 报名列表筛选正确性**
    - 使用 jqwik 生成随机 Application 数据集和筛选条件
    - 验证筛选结果中每条记录满足所有指定条件
    - **Validates: Requirements 11.2, 11.3, 11.4, 11.5, 11.7**

  - [ ] 9.7 编写 Property 8 属性测试：报名列表时间排序
    - **Property 8: 报名列表时间排序**
    - 使用 jqwik 生成随机 Application 数据集
    - 验证查询结果按 createdAt 降序排列
    - **Validates: Requirements 11.1**

  - [ ] 9.8 编写 Property 13 属性测试：分页大小约束
    - **Property 13: 分页大小约束**
    - 使用 jqwik 生成随机分页参数和数据集
    - 验证返回结果数量不超过 size
    - **Validates: Requirements 11.6**

- [ ] 10. 前端报名列表分页筛选
  - [ ] 10.1 在 frontend/src/api/application.ts 中添加分页筛选 API 方法
    - 添加 getApplicationsByPage 方法，支持 page、size 和筛选参数
    - 添加 ApplicationPageResponse 类型定义
    - _Requirements: 11.1, 11.6_

  - [ ] 10.2 重构 ApplicationsPage.vue，添加筛选栏和分页组件
    - 添加状态筛选下拉框（待初审/待AI面试/待复审/已通过/已拒绝）
    - 添加年龄范围输入
    - 添加教育阶段下拉框
    - 添加中高考状态筛选
    - 替换 getApplications 为 getApplicationsByPage
    - 添加 el-pagination 分页组件
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

- [ ] 11. 后端属性测试：初审状态转换
  - [ ] 11.1 编写 Property 6 属性测试：初审状态转换一致性
    - **Property 6: 初审状态转换一致性**
    - 使用 jqwik 生成随机审核决定（通过/拒绝）
    - 验证通过时状态为 INITIAL_REVIEW_PASSED 且 enabled=true
    - 验证拒绝时状态为 REJECTED 且 enabled=false
    - **Validates: Requirements 10.1, 10.2**

- [ ] 12. Final checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

## 备注

- 标记 `*` 的任务为可选任务，可跳过以加快 MVP 进度
- 每个任务引用了具体的需求编号以确保可追溯性
- Checkpoint 任务确保增量验证
- 属性测试验证通用正确性属性，单元测试验证具体示例和边界情况
- 后端属性测试使用 jqwik，前端属性测试使用 fast-check
