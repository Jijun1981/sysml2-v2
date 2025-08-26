# SysML v2 MVP 自下而上开发路线图

## 开发理念
**自下而上（Bottom-Up）构建**：从数据模型和核心服务开始，逐层向上构建，确保每层都有坚实的基础。

## 当前基础（已完成）✅
```
Layer 0: EMF核心层
├── EMFModelRegistry (182个EClass)     // REQ-B1-1: 完整Pilot元模型注册
├── FileModelRepository (JSON持久化)   // REQ-B1-2: JSON工厂
├── PilotEMFService (EMF工具方法)      // REQ-B2-1: 创建API基础
└── Demo数据 (21个对象)               // REQ-B1-4: Demo数据
```

---

## 📦 第一阶段：通用元素服务层 (Week 1)
> 构建内部通用服务基础

### 1.1 通用DTO定义
```java
backend/src/main/java/com/sysml/mvp/dto/
├── ElementDTO.java                // ✅【REQ-D0-1】通用元素数据API - eClass字段标识类型
├── RequirementDTO.java            // 【REQ-B2-4】DTO选择性映射 - 需求特定字段
├── TraceDTO.java                  // 【REQ-C3-1】创建追溯关系 - 追溯DTO
└── ValidationResultDTO.java      // 【REQ-E1-3】接口返回 - 验证结果格式
```

### 1.2 通用元素服务（内部工具）
```java
backend/src/main/java/com/sysml/mvp/service/
└── UniversalElementService.java   // ✅【REQ-B5-3】内部EMF工具层 - 仅供领域层使用
    ├── createElement(eClassName, attributes)  // 【REQ-B2-1】创建API
    ├── queryElements(type)                   // 【REQ-D0-1】通用查询 - 支持type过滤
    ├── patchElement(id, updates)            // 【REQ-B2-4】DTO选择性映射 - PATCH语义
    └── deleteElement(id)                    // 内部删除方法
```

### 1.3 数据映射工具
```java
backend/src/main/java/com/sysml/mvp/mapper/
└── ElementMapper.java       // ✅【REQ-B2-4】DTO选择性映射 - EMF↔DTO转换
    ├── toDTO(EObject)      // REQ-D0-1: 动态提取所有属性到ElementDTO
    └── toMap(ElementDTO)   // REQ-B2-4: DTO转属性Map，用于PATCH更新
```

**交付标准（对应需求验收）**：
- ✅【REQ-D0-1】ElementDTO包含eClass字段，支持任意SysML类型
- ✅【REQ-B5-3】UniversalElementService仅供内部使用，完整CRUD实现
- ✅【REQ-B2-4】ElementMapper支持选择性映射，未映射字段保持默认值
- ✅【REQ-B2-1】与PilotEMFService正确集成，createElement工作正常
- ✅ 单元测试覆盖所有方法，29个测试全部通过

---

## 🔧 第二阶段：领域服务层 (Week 2)
> 实现业务验证和领域逻辑

### 2.1 领域服务实现（委托通用服务）
```java
backend/src/main/java/com/sysml/mvp/service/
├── RequirementService.java     // 【REQ-B5-1】需求领域接口 - 业务验证后委托
│   ├── createRequirement()            // 【REQ-C1-1】创建需求定义 - reqId唯一性验证
│   ├── updateRequirement()            // 【REQ-C1-3】更新需求定义 - PATCH语义
│   ├── deleteRequirement()            // 【REQ-C2-4】删除前检查被引用保护
│   ├── createRequirementUsage()       // 【REQ-C2-1】创建需求使用 - of字段验证
│   ├── renderParametricText()         // 【REQ-C1-4】参数化文本渲染 - ${placeholder}语法
│   ├── getRequirements()              // 【REQ-C1-2】查询需求定义 - 委托通用服务
│   └── getRequirementById()           // 【REQ-B5-1】根据ID查找 - 委托通用服务
│
├── TraceService.java           // 【REQ-C3-1】追溯领域服务 - type映射到EClass
│   ├── createTrace()                  // 【REQ-C3-1】创建追溯关系 - derive/satisfy映射
│   ├── validateTraceSemantics()       // 【REQ-C3-4】追溯语义约束 - Satisfy/DeriveRequirement规则
│   ├── mapTraceTypeToEClass()         // 【REQ-C3-1】type→EClass映射 - API简化
│   ├── findTracesByElement()          // 【REQ-C3-2】查询元素相关追溯
│   └── preventDuplicateTraces()       // 【REQ-C3-3】依赖去重 - 相同(source,target,eClass)
│
└── ValidationService.java      // 【REQ-E1-1】MVP规则集 - 3条核心规则
    ├── validateReqIdUniqueness()      // 【REQ-E1-1】规则1: reqId唯一性检查
    ├── checkCyclicDependency()        // 【REQ-E1-1】规则2: derive/refine循环依赖
    ├── checkDanglingReferences()      // 【REQ-E1-1】规则3: 悬挂引用检查
    └── validateStaticRules()          // 【REQ-E1-3】接口返回 - ValidationResultDTO格式
```

### 2.2 业务规则实现（对应具体需求条目）
- **RequirementService**（对应EPIC C1-C2）
  - 【REQ-C1-1】创建需求定义：验证reqId唯一性，重复→409冲突
  - 【REQ-C1-3】更新需求定义：PATCH语义，只更新提供字段
  - 【REQ-C1-4】参数化文本渲染：Definition支持${placeholder}语法
  - 【REQ-C2-1】创建需求使用：验证of字段引用存在，不存在→404
  - 【REQ-C2-3】约束对象必填：RequirementUsage必须填写subject字段
  - 【REQ-C2-4】删除保护：Definition被Usage引用时→409，阻止删除
  - 【REQ-B5-5】QUDV单位量纲校验：数值参数单位与量纲匹配验证
  - 委托模式：所有CRUD通过UniversalElementService.createElement()执行

- **TraceService**（对应EPIC C3）
  - 【REQ-C3-1】创建追溯关系：API layer的type映射为具体EClass实例
  - 【REQ-C3-2】支持专门追溯类型：derive/satisfy/refine/trace四种type
  - 【REQ-C3-3】依赖去重：相同(source,target,eClass)不重复创建，返回200
  - 【REQ-C3-4】追溯语义约束：
    * Satisfy: source∈{PartUsage,ActionUsage}, target∈{RequirementUsage,RequirementDefinition}
    * DeriveRequirement: source/target∈{RequirementDefinition,RequirementUsage}
    * 违反约束→400，确保追溯图语义正确性
  - 委托模式：通过UniversalElementService.createElement()创建Dependency实例

- **ValidationService**（对应EPIC E1）
  - 【REQ-E1-1】MVP规则集：仅检测3条核心规则，不扩展
  - 【REQ-E1-2】规则码枚举：DUP_REQID、CYCLE_DERIVE_REFINE、BROKEN_REF
  - 【REQ-E1-3】接口返回：POST /api/v1/validate/static格式规范
  - 性能要求：≤500元素处理<2s

**交付标准（对应需求验收）**：
- ✅【REQ-B5-1】领域服务正确委托给UniversalElementService，不直接操作EMF
- ✅【REQ-C1-1】reqId唯一性验证通过，重复创建返回409
- ✅【REQ-C2-4】删除保护验证通过，被引用Definition无法删除
- ✅【REQ-C3-4】追溯语义约束验证通过，违反类型约束返回400
- ✅【REQ-E1-1】3条静态验证规则完整实现并通过测试
- ✅ 服务层单元测试覆盖率>80%，所有业务规则有测试覆盖

---

## 🌐 第三阶段：领域API层 (Week 3)
> 暴露领域特定HTTP接口

### 3.1 Controller实现（调用领域服务）
```java
backend/src/main/java/com/sysml/mvp/controller/
├── RequirementController.java   // 【REQ-B5-1】需求领域接口 - HTTP暴露
│   ├── POST /api/v1/requirements         // 【REQ-C1-1】创建需求定义 - 201/409响应
│   ├── GET /api/v1/requirements          // 【REQ-C1-2】查询需求定义 - 支持分页
│   ├── GET /api/v1/requirements/{id}     // 【REQ-B5-1】根据ID查找需求
│   ├── PATCH /api/v1/requirements/{id}   // 【REQ-C1-3】更新需求定义 - PATCH语义
│   ├── DELETE /api/v1/requirements/{id}  // 【REQ-C2-4】删除需求 - 引用保护
│   ├── POST /api/v1/requirements/usage   // 【REQ-C2-1】创建需求使用
│   └── GET /api/v1/requirements/tree     // 【REQ-D1-1】树视图数据 - Definition-Usage层级
│
├── TraceController.java        // 【REQ-C3-1】追溯领域服务 - HTTP暴露
│   ├── POST /api/v1/traces              // 【REQ-C3-1】创建追溯关系 - type映射
│   ├── GET /api/v1/traces               // 【REQ-C3-2】查询追溯关系 - 支持type过滤
│   ├── GET /api/v1/traces?element={id}  // 【REQ-C3-2】查询元素相关追溯
│   └── DELETE /api/v1/traces/{id}       // 删除追溯关系
│
├── ElementController.java      // 【REQ-B5-3】内部EMF工具层 - 只读对外接口
│   ├── GET /api/v1/elements             // 【REQ-D0-1】通用元素数据API - 查询所有
│   ├── GET /api/v1/elements?type={eClassName} // 【REQ-D0-2】依赖关系数据API - 按类型
│   ├── GET /api/v1/elements?page&size   // 【REQ-D2-1】表视图数据 - 分页支持
│   └── POST/PUT/DELETE → 405            // 【REQ-B5-3】写操作返回405 - "请使用领域端点"
│
├── ValidationController.java   // 【REQ-E1-3】接口返回 - 静态验证API
│   └── POST /api/v1/validate/static     // 【REQ-E1-3】执行静态验证 - ValidationResultDTO
│
└── ProjectController.java      // 【REQ-B3-1,B3-2】项目导入导出
    ├── GET /api/v1/projects/{pid}/export  // 【REQ-B3-1】导出JSON - 规范文件名
    └── POST /api/v1/projects/{pid}/import // 【REQ-B3-2】导入JSON - 一致性保证
```

### 3.2 API响应规范（对应具体需求条目）
```
需求验证响应（EPIC C1-C2）：
【REQ-C1-1】创建需求成功 → 201 Created + RequirementDTO
【REQ-C1-1】reqId重复 → 409 Conflict "reqId already exists"
【REQ-C1-2】查询需求 → 200 OK + RequirementDTO[]，支持分页
【REQ-C1-3】更新成功 → 200 OK + RequirementDTO，PATCH语义
【REQ-C2-1】创建Usage → 201 Created，验证of引用存在
【REQ-C2-3】缺少subject → 400 Bad Request "subject is required"
【REQ-C2-4】删除保护 → 409 Conflict "referenced by N usages"

追溯关系响应（EPIC C3）：
【REQ-C3-1】创建追溯 → 201 Created + TraceDTO，type映射到EClass
【REQ-C3-2】查询追溯 → 200 OK + TraceDTO[]，支持type和element过滤
【REQ-C3-3】重复追溯 → 200 OK（返回既有对象，不重复创建）
【REQ-C3-4】语义约束违反 → 400 Bad Request "invalid trace type"

架构约束响应（EPIC B5）：
【REQ-B5-3】写操作/api/v1/elements → 405 Method Not Allowed "请使用领域端点"
【REQ-D0-1】元素查询 → 200 OK + ElementDTO[]，包含eClass字段
【REQ-D0-2】依赖查询 → 200 OK，支持type=Dependency等过滤

验证和导入导出响应（EPIC E1, B3）：
【REQ-E1-3】静态验证 → 200 OK + ValidationResultDTO，≤500元素<2s
【REQ-B3-1】导出JSON → 200 OK + application/json，标准文件名
【REQ-B3-2】导入JSON → 200 OK，一致性保证，格式错误返回行/列/原因

通用约定：
- 分页：page从0开始，size∈(1..200]，默认50，超出范围→400
- 时间戳：ISO-8601 UTC格式，服务器生成并维护
- ID稳定性：导出/导入后ID保持不变
- 错误码：400（参数错误）、404（不存在）、409（冲突）、405（方法不允许）
```

**交付标准（对应需求验收）**：
- ✅【REQ-B5-1】需求领域接口完整实现，HTTP暴露所有CRUD操作
- ✅【REQ-B5-3】/api/v1/elements仅支持GET，写操作返回405提示使用领域端点
- ✅【REQ-C1-1】reqId唯一性验证HTTP返回409，错误信息明确
- ✅【REQ-C2-4】删除保护HTTP返回409，提示被引用情况
- ✅【REQ-C3-4】追溯语义约束违反HTTP返回400，错误类型明确（基于Dependency实现）
- ✅【REQ-E1-3】静态验证API返回标准ValidationResultDTO格式
- ✅【REQ-A1-1,A2-1,A3-1】所有Controller层功能经过端到端集成测试验证
- ✅ Controller到EMF持久化层完整数据流验证通过
- ✅ 30个Controller测试全部通过，HTTP状态码和响应格式正确
- ✅ 发现并修复ElementMapper缺少elementId处理的集成问题

---

## 💾 第四阶段：数据增强层 (Week 4) ⬅️ **当前阶段**
> 完善数据管理能力

### 4.1 高级查询功能
- 【通用约定】分页支持：page从0开始，size∈(1..200]，默认50
- 排序支持 (sort参数)
- 过滤支持 (filter参数)
- 全文搜索 (search参数)

### 4.2 导入导出功能
- 【REQ-B3-1】JSON项目导出：GET /api/v1/projects/{pid}/export
- 【REQ-B3-2】JSON项目导入：POST /api/v1/projects/{pid}/import
- 【REQ-B3-3】导入导出一致性：ID保持不变
- 【通用约定】导出文件名：project-{pid}.json

### 4.3 高质量Demo数据
- 【REQ-B1-4】电池系统Demo数据：
  - 15-20个RequirementDefinition模板（L1/L2/L3层次）
  - 50个RequirementUsage实例
  - 15-20个追溯关系
  - reqId层次编码（EBS-L1-001格式）
- 【REQ-B1-4】测试数据集：small(10)/medium(100)/large(500)

**交付标准**：
- ✅ 查询性能<500ms (500个元素)
- ✅ 导入导出往返一致性100%
- ✅ Demo数据覆盖所有元素类型

---

## 🎨 第五阶段：前端基础层 (Week 5)
> 构建前端架构

### 5.1 项目结构
```
frontend/
├── src/
│   ├── api/          // API客户端
│   ├── types/        // TypeScript类型
│   ├── store/        // 状态管理
│   ├── hooks/        // 自定义Hooks
│   └── utils/        // 工具函数
```

### 5.2 核心模块
- **API客户端**: 【REQ-D0-3】前端自动加载数据
- **类型定义**: 与后端DTO对应，支持eClass字段
- **状态管理**: 【REQ-A1-1】SSOT - Context API统一状态
- **错误处理**: 统一错误边界

**交付标准**：
- ✅ TypeScript严格模式无错误
- ✅ API客户端类型安全
- ✅ 状态管理响应式更新

---

## 🖼️ 第六阶段：视图组件层 (Week 6-7)
> 实现三视图展示

### 6.1 基础组件
```
frontend/src/components/
├── common/           // 通用组件
├── tree/            // 树视图
│   ├── TreeView.tsx
│   └── TreeNode.tsx
├── table/           // 表视图
│   ├── TableView.tsx
│   └── TableColumns.tsx
└── graph/           // 图视图
    ├── GraphView.tsx
    └── GraphNode.tsx
```

### 6.2 视图功能
- **TreeView**: 【REQ-D1-1】层级展示，包含/引用关系
- **TableView**: 【REQ-D2-1】分页表格，内联编辑
- **GraphView**: 【REQ-D3-1】React Flow图形化，【REQ-D3-3】自动布局≤500节点<3s

### 6.3 视图联动
- 【REQ-A1-1】选中同步：任一视图选中，其他视图高亮
- 【REQ-A1-2】视图为投影：不持久化副本
- 【REQ-A1-3】性能底线：500节点联动<500ms

**交付标准**：
- ✅ 三视图独立功能完整
- ✅ 视图间联动响应<100ms
- ✅ 支持500节点流畅渲染

---

## 🚀 第七阶段：集成优化层 (Week 8)
> 系统集成和优化

### 7.1 端到端测试
- Cypress E2E测试套件
- 用户场景覆盖
- 性能基准测试

### 7.2 性能优化
- 【REQ-A1-3】虚拟滚动：超过500节点自动启用
- 【REQ-D2-3】懒加载：表格数据分页加载
- 缓存策略：减少重复请求

### 7.3 部署准备
- Docker镜像构建
- 环境配置分离
- 部署文档编写

**交付标准**：
- ✅ 【REQ-A1-3】性能达标：500节点/1000边，响应<500ms
- ✅ 【REQ-B2-3】ID稳定性：导出导入后ID不变
- ✅ 【REQ-E1-1】3条核心验证规则全部通过
- ✅ E2E测试通过率100%
- ✅ 一键部署脚本就绪

---

## 📊 开发优先级矩阵

| 阶段 | 层次 | 优先级 | 依赖 | 关键任务 |
|------|------|--------|------|----------|
| 0 | EMF核心层 | ✅完成 | - | PilotEMFService已实现 |
| 1 | 通用服务层 | P0 | EMF核心 | UniversalElementService |
| 2 | 领域服务层 | P0 | 通用服务 | RequirementService委托调用 |
| 3 | 领域API层 | P0 | 领域服务 | 领域Controller |
| 4 | 数据增强层 | P1 | API层 | 分页/排序/过滤 |
| 5 | 前端基础层 | P0 | - | 可并行开发 |
| 6 | 视图组件层 | P0 | 前端基础 | 三视图实现 |
| 7 | 集成优化层 | P1 | 全部 | 性能优化 |

## 🎯 下一步行动

**立即开始第一阶段：通用元素服务层**

1. 创建ElementDTO通用数据传输对象
2. 实现UniversalElementService（内部工具）
3. 创建ElementMapper映射工具
4. 测试与PilotEMFService集成

**架构调用链**：
```
RequirementController（领域API）
    ↓ 调用
RequirementService（业务验证）
    ↓ 委托
UniversalElementService（通用CRUD）
    ↓ 委托
PilotEMFService（EMF操作）✅已有
    ↓
EMF核心（182个EClass）✅已有
```

这种自下而上的方式确保：
- ✅ 符合需求文档REQ-B5要求
- ✅ 领域服务专注业务验证
- ✅ 通用服务提供复用能力
- ✅ 写操作必须走领域端点

## 📋 需求对齐验证矩阵

| 阶段 | 已实现组件 | 对应需求条目 | 验收状态 |
|------|------------|--------------|----------|
| **第一阶段** | ElementDTO | REQ-D0-1 | ✅ 9个测试通过 |
| | UniversalElementService | REQ-B5-3 | ✅ 11个测试通过 |
| | ElementMapper | REQ-B2-4 | ✅ 9个测试通过 |
| **待完成** | RequirementDTO | REQ-B2-4 | ❌ 未创建 |
| | TraceDTO | REQ-C3-1 | ❌ 未创建 |
| | ValidationResultDTO | REQ-E1-3 | ❌ 未创建 |

**第一阶段完成度**: 50% (3/6 组件完成)

## 🎯 当前任务优先级

**立即任务**（完成第一阶段）：
1. ❌ RequirementDTO + 测试用例
2. ❌ TraceDTO + 测试用例  
3. ❌ ValidationResultDTO + 测试用例

**严格遵循TDD原则**：
- 先写测试用例，与需求条目严格对齐
- 测试用例必须覆盖具体的REQ-XXX验收条件
- 实现代码只为通过测试，不添加额外功能
- 每个DTO完成后立即验证与需求文档的一致性

**禁止野代码**：
- ✅ 每行代码必须对应明确的需求条目
- ✅ 不实现需求文档之外的功能
- ✅ 测试用例标注对应的REQ-XXX编号
- ✅ 交付标准与需求验收条件完全匹配