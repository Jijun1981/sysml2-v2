# SysML v2 需求建模平台 Phase 1 追加需求 v5.0

## 文档信息

- **版本**: 5.0
- **日期**: 2025-08-28
- **状态**: Phase 1 功能规格 - 基于MVP字段标准化完成版
- **基线版本**: 基于MVP v7.0（字段标准化完成），追加需求建模核心功能
- **开发方式**: 测试驱动开发（TDD），先写测试再实现

---

## 0) 基于MVP基础的架构演进

**MVP已完成基础能力**（v7.0 - 字段标准化完成）:

- ✅ EMF基座和JSON持久化（182个SysML EClass）
- ✅ 通用元素CRUD（RequirementDefinition/Usage）
- ✅ 追溯关系管理（Satisfy/Derive/Refine/Trace）
- ✅ 基础三视图联动（树/表/图）
- ✅ 静态验证和项目导入导出
- ✅ SysML 2.0字段标准化（of→requirementDefinition，删除subject）
- ✅ MetadataService接口定义（治理字段管理）

**Phase 1 目标**：在MVP基础上构建完整的需求建模平台

## 测试基线保护

**MVP测试套件**（必须保持通过）：
- ✅ **217个测试用例** - 完整覆盖现有API和业务逻辑
- ✅ **21个测试文件** - 覆盖所有层次的测试
- ✅ **99个核心测试** - Controller和Service层关键测试
- ✅ **35个字段标准化测试** - SysML 2.0字段兼容性验证
- ✅ **100%通过率** - 所有测试全部通过

**Phase 1开发约束**：
1. **禁止修改现有测试用例**，除非API签名发生必要变更
2. **新功能必须TDD开发**，先写测试再写实现
3. **每次提交必须运行完整测试套件**：`mvn test`
4. **重点回归验证**：`mvn test -Dtest="RequirementControllerTest,TraceControllerTest,ValidationControllerTest"`

---

## EPIC F｜MVP基础功能完善（P0-紧急）

### Story F1｜RequirementDefinition/Usage CRUD前后端联通

**As** 用户 **I want** 完整的需求定义和使用实例CRUD功能 **so that** 能够正常创建和管理需求。

**Background**: MVP v7.0虽然后端API完整且字段标准化完成，但前端UI可能还需要完善集成，确保用户能通过界面完成所有需求管理操作。

**Requirements**

- **REQ-F1-1** RequirementDefinition CRUD前端集成
  - **AC**: 前端主界面提供"创建需求定义"按钮；点击后弹出`CreateRequirementDialog`；填写reqId、name、text等字段后调用`requirementService.createRequirementDefinition()`；创建成功后自动刷新需求列表；**TDD**: 先写 `RequirementCRUD.integration.test.tsx` 测试用例。

- **REQ-F1-2** RequirementUsage CRUD前端集成  
  - **AC**: 前端主界面提供"创建需求使用"按钮；必须选择关联的RequirementDefinition（requirementDefinition字段）；填写实例化参数后调用`requirementService.createRequirementUsage()`；**TDD**: 先写 `RequirementUsageCRUD.test.tsx` 测试用例。

- **REQ-F1-3** 需求编辑和删除功能
  - **AC**: 表视图每行提供编辑/删除按钮；编辑使用`EditRequirementDialog`组件；删除前确认对话框；操作成功后即时更新UI；**TDD**: 先写 `RequirementEdit.test.tsx` 测试用例。

- **REQ-F1-4** 错误处理和用户反馈
  - **AC**: API调用失败时显示具体错误信息；网络错误时提示重试；409冲突时解释reqId重复；加载状态显示loading指示器；**TDD**: 先写 `ErrorHandling.test.tsx` 测试用例。

### Story F2｜三视图数据集成修复

**As** 用户 **I want** 三视图显示真实的需求数据 **so that** 能够在不同视图中查看和操作相同的需求。

**Requirements**

- **REQ-F2-1** 树视图数据加载修复
  - **AC**: 树视图调用`requirementService.getRequirements()`加载真实数据；按Definition和Usage分类显示；点击节点显示详细信息；**TDD**: 先写 `TreeView.data.test.tsx` 测试用例。

- **REQ-F2-2** 表视图数据集成
  - **AC**: 表视图显示reqId、name、text、eClass、status等列；支持分页加载；支持搜索和筛选；**TDD**: 先写 `TableView.data.test.tsx` 测试用例。

- **REQ-F2-3** 图视图关系显示
  - **AC**: 图视图显示需求节点和追溯关系；Usage节点连接到Definition节点；trace关系用不同颜色区分；**TDD**: 先写 `GraphView.relationship.test.tsx` 测试用例。

---

## EPIC G｜技术规格书项目管理（P0）

### Story G1｜技术规格书CRUD

**As** 项目管理员 **I want** 管理技术规格书作为需求项目容器 **so that** 需求有清晰的项目归属和版本管理。

**Background**: 当前MVP v7.0有通用元素接口和字段标准化，但缺少项目级的技术规格书管理。实施架构要求技术规格书作为项目容器，包含需求和关系。

**Requirements**

- **REQ-G1-1** 创建技术规格书

  - **AC**: `POST /api/v1/tech-specs {"name": "电动汽车控制系统技术规格书", "version": "1.0.0", "author": "张三", "description": "..."}` → `201` 返回 `TechSpecDTO`；`name` 必填，重复项目名 → `409`；**TDD**: 先写 `TechSpecControllerTest.createTechSpec_Success()` 测试用例。
- **REQ-G1-2** 查询技术规格书

  - **AC**: `GET /api/v1/tech-specs` 返回所有技术规格书列表；`GET /api/v1/tech-specs/{id}` 返回特定技术规格书详情；包含需求数量、关系数量等统计信息；**TDD**: 先写 `TechSpecControllerTest.getTechSpecs_Success()` 测试用例。
- **REQ-G1-3** 更新技术规格书

  - **AC**: `PATCH /api/v1/tech-specs/{id} {"version": "1.1.0", "status": "review"}` 部分更新；只更新提供的字段；版本号必须符合 SemVer 格式；**TDD**: 先写 `TechSpecControllerTest.updateTechSpec_Success()` 测试用例。
- **REQ-G1-4** 删除技术规格书

  - **AC**: `DELETE /api/v1/tech-specs/{id}` 删除技术规格书；包含需求时 → `409`，提示"请先清空需求"；删除成功 → `204`；**TDD**: 先写 `TechSpecControllerTest.deleteTechSpec_WithRequirements_Conflict()` 测试用例。

### Story G2｜技术规格书内容管理

**As** 需求分析师 **I want** 在技术规格书中管理需求和关系 **so that** 需求有明确的项目归属。

**Requirements**

- **REQ-G2-1** 技术规格书需求关联

  - **AC**: 创建需求时必须指定 `techSpecId`；`POST /api/v1/tech-specs/{techSpecId}/requirements {"name": "系统性能需求", "text": "..."}` → 需求自动关联到技术规格书；**TDD**: 先写 `TechSpecServiceTest.addRequirementToTechSpec()` 测试用例。
- **REQ-G2-2** 技术规格书内容查询

  - **AC**: `GET /api/v1/tech-specs/{id}/requirements` 返回技术规格书包含的所有需求；`GET /api/v1/tech-specs/{id}/relationships` 返回所有关系；支持分页；**TDD**: 先写 `TechSpecControllerTest.getTechSpecContent()` 测试用例。
- **REQ-G2-3** 技术规格书完整性校验

  - **AC**: 关系的源和目标必须都在同一技术规格书内；跨技术规格书引用 → `400`；提供 `POST /api/v1/tech-specs/{id}/validate` 校验接口；**TDD**: 先写 `TechSpecValidationTest.validateCrossReference()` 测试用例。

---

## EPIC H｜需求库和模板管理（P0）

### Story H1｜RequirementDefinition 库管理

**As** 需求工程师 **I want** 管理可复用的需求定义模板库 **so that** 提高需求编写效率和一致性。

**Background**: MVP v7.0支持RequirementDefinition CRUD和字段标准化，但缺少库的概念、模板机制和批量导入功能。这与之前讨论的模板化需求（isAbstract标记）需要对齐。

**Requirements**

- **REQ-H1-1** 需求定义库查询

  - **AC**: `GET /api/v1/requirement-definitions` 返回全部定义库；支持分页、搜索、分类筛选；`GET /api/v1/requirement-definitions/categories` 返回分类列表（如：性能、安全、接口）；**TDD**: 先写 `RequirementDefinitionControllerTest.getDefinitionLibrary()` 测试用例。
- **REQ-H1-2** 需求定义批量导入

  - **AC**: `POST /api/v1/requirement-definitions/bulk-import` 支持JSON/CSV批量导入；CSV格式：`reqId,name,text,category,priority`；导入失败返回具体行号和错误信息；**TDD**: 先写 `RequirementDefinitionServiceTest.bulkImport_CSV()` 测试用例。
- **REQ-H1-3** 需求定义使用统计

  - **AC**: `GET /api/v1/requirement-definitions/{id}/usage-stats` 返回定义被引用次数、最后使用时间等；支持批量查询多个定义的使用情况；**TDD**: 先写 `RequirementDefinitionServiceTest.getUsageStatistics()` 测试用例。

### Story H2｜需求包管理

**As** 需求工程师 **I want** 将相关需求定义打包管理 **so that** 可以批量复用领域知识。

**Requirements**

- **REQ-H2-1** 需求包定义

  ```typescript
  interface RequirementPackage {
    id: string;
    name: string;              // "ISO 26262 安全需求包"
    description: string;       // 包描述
    domain: string;           // 应用领域（汽车、航空等）
    version: string;          // 包版本
    content: {
      definitions: RequirementDefinition[];  // 包含的定义
      relationships: Relationship[];          // 定义间关系
    };
  }
  ```

  - **AC**: 需求包包含多个相关的需求定义和它们之间的关系；支持版本管理；**TDD**: 先写 `RequirementPackageTest.createPackage()` 测试用例。
- **REQ-H2-2** 需求包CRUD操作

  - **AC**: `POST /api/v1/requirement-packages` 创建需求包；`GET /api/v1/requirement-packages` 查询需求包库；`PATCH /api/v1/requirement-packages/{id}` 更新包信息；包被使用时删除 → `409`；**TDD**: 先写 `RequirementPackageControllerTest.packageCRUD()` 测试用例。
- **REQ-H2-3** 需求包导入到技术规格书

  - **AC**: `POST /api/v1/tech-specs/{techSpecId}/import-package {"packageId": "pkg-iso26262", "options": {"createUsages": true}}` → 将包中的定义复制到技术规格书，并可选择性创建使用实例；ID重映射保证唯一性；**TDD**: 先写 `RequirementPackageServiceTest.importToTechSpec()` 测试用例。

---

## EPIC I｜基于关系的需求层级管理（P0）

### Story I1｜关系驱动的树形结构

**As** 需求分析师 **I want** 通过derive/refine关系构建需求层级 **so that** 符合SysML 2.0标准的关系驱动模型。

**Background**: MVP的树视图是简单的列表展示，需要基于关系构建真正的层级结构。

**Requirements**

- **REQ-I1-1** 关系驱动的树构建算法

  - **AC**: `GET /api/v1/tech-specs/{id}/requirements-tree` 返回基于derive/refine关系的树形结构；没有被derive/refine指向的需求为根节点；支持多根树；**TDD**: 先写 `RequirementTreeServiceTest.buildTreeFromRelationships()` 测试用例。
- **REQ-I1-2** 孤立需求识别

  - **AC**: 树结构中标识出没有任何关系的孤立需求；`GET /api/v1/tech-specs/{id}/orphan-requirements` 返回孤立需求列表；在树视图中用特殊样式标记；**TDD**: 先写 `RequirementTreeServiceTest.identifyOrphanRequirements()` 测试用例。
- **REQ-I1-3** 循环依赖检测

  - **AC**: 创建derive/refine关系时检测循环依赖；`POST /api/v1/relationships` 如果形成循环 → `400`，返回循环路径；使用深度优先搜索算法；**TDD**: 先写 `RelationshipValidationTest.detectCircularDependency()` 测试用例。

### Story I2｜拖拽建模交互

**As** 需求分析师 **I want** 通过拖拽方式创建需求和关系 **so that** 直观高效地建模。

**Requirements**

- **REQ-I2-1** Definition到Usage的拖拽创建

  - **AC**: 从需求库拖拽Definition到技术规格书画布 → 自动创建RequirementUsage并设置requirementDefinition字段关联；画布支持多选拖拽批量创建；**TDD**: 先写 `DragDropServiceTest.createUsageFromDefinition()` 测试用例。
- **REQ-I2-2** 需求间关系拖拽创建

  - **AC**: 从源需求拖拽到目标需求 → 弹出关系类型选择框（derive/refine/trace/satisfy）；创建对应的Relationship对象；自动更新树视图结构；**TDD**: 先写 `DragDropServiceTest.createRelationshipByDrag()` 测试用例。
- **REQ-I2-3** 需求包整体拖拽导入

  - **AC**: 拖拽需求包到技术规格书 → 批量导入包内的所有定义和关系；自动处理ID重映射；生成导入报告；**TDD**: 先写 `DragDropServiceTest.importPackageByDrag()` 测试用例。

---

## EPIC J｜多视图建模界面（P0）

### Story J1｜双树建模界面

**As** 需求分析师 **I want** 上下分屏的双树界面 **so that** 同时浏览技术规格书和需求库。

**Background**: 实施架构定义了双树界面：上部显示技术规格书需求，下部显示需求库。

**Requirements**

- **REQ-J1-1** 双树界面布局

  - **AC**: 界面分为上下两部分：上部显示当前技术规格书的需求树（基于关系构建），下部显示需求库（Definition分类树）；支持拖拽调节分割线位置；**TDD**: 先写前端组件测试 `RequirementExplorer.test.tsx`。
- **REQ-J1-2** 需求库分类展示

  - **AC**: 下部树按分类展示需求定义（性能、安全、接口等）；支持搜索过滤；显示定义使用次数；支持收藏夹功能；**TDD**: 先写 `DefinitionLibraryTree.test.tsx` 测试。
- **REQ-J1-3** 跨树拖拽操作

  - **AC**: 支持从下部需求库拖拽Definition到上部技术规格书创建Usage；支持上部需求间拖拽创建关系；提供拖拽预览和合法性校验；**TDD**: 先写 `TreeDragDrop.test.tsx` 测试。

### Story J2｜图视图增强

**As** 需求分析师 **I want** 专业的需求依赖图 **so that** 直观查看需求间关系。

**Requirements**

- **REQ-J2-1** SysML标准节点样式

  - **AC**: 需求节点使用SysML标准样式：矩形框、«requirement»构造型、分段显示（header/text/constraints）；Definition用虚线边框，Usage用实线边框；**TDD**: 先写 `RequirementNode.test.tsx` 测试。
- **REQ-J2-2** 关系连线标准样式

  - **AC**: 不同关系类型使用不同样式：derive（蓝色虚线）、satisfy（绿色实线）、refine（橙色细线）、trace（灰色点线）；连线显示关系标签；**TDD**: 先写 `RelationshipEdge.test.tsx` 测试。
- **REQ-J2-3** 图布局算法

  - **AC**: 使用分层布局算法（hierarchical layout）自动排列节点；根需求在顶部，派生需求分层向下；支持手动调节节点位置并保存；**TDD**: 先写 `GraphLayoutTest.ts` 测试。

### Story J3｜表视图功能完善

**As** 需求分析师 **I want** 功能完整的需求表格 **so that** 批量编辑和分析需求。

**Requirements**

- **REQ-J3-1** 需求表格列定义

  - **AC**: 表格列包括：reqId、declaredName、documentation、requirementDefinition（对于Usage）、status、priority、verificationMethod、createdAt、updatedAt等；支持列的显示/隐藏、排序、筛选；**TDD**: 先写 `RequirementTable.test.tsx` 测试。
- **REQ-J3-2** 行内编辑功能

  - **AC**: 双击单元格进入编辑模式；支持下拉选择（status）、文本编辑（name/text）、只读显示（创建时间等）；编辑后自动保存；**TDD**: 先写 `InlineEditCell.test.tsx` 测试。
- **REQ-J3-3** 批量操作

  - **AC**: 支持多选行进行批量状态更新、批量删除；批量删除前确认对话框；操作失败提供详细错误信息；**TDD**: 先写 `BulkOperations.test.tsx` 测试。

---

## EPIC K｜数据状态管理和同步（P0）

### Story K1｜单一数据源架构

**As** 系统 **I want** 严格的SSOT数据管理 **so that** 确保多视图数据一致性。

**Background**: MVP虽然实现了基础的数据同步，但需要更严格的SSOT架构来支持复杂的建模操作。

**Requirements**

- **REQ-K1-1** 中央数据Store设计

  ```typescript
  interface RequirementDataStore {
    // 当前技术规格书
    currentTechSpec: TechnicalSpecification | null;

    // 核心数据映射
    requirements: Map<string, RequirementUsage>;
    definitions: Map<string, RequirementDefinition>;
    relationships: Map<string, Relationship>;
    packages: Map<string, RequirementPackage>;

    // 计算属性（缓存）
    requirementTree: TreeNode[];
    orphanRequirements: RequirementUsage[];

    // UI状态
    selectedIds: Set<string>;
    viewFilters: ViewFilters;
  }
  ```

  - **AC**: 所有数据修改必须通过Store的dispatch方法；任何数据变更自动通知所有视图更新；**TDD**: 先写 `RequirementDataStore.test.ts` 测试。
- **REQ-K1-2** 乐观更新机制

  - **AC**: UI操作立即更新本地状态；后台异步提交到服务器；提交失败时回滚本地状态并提示用户；避免UI阻塞；**TDD**: 先写 `OptimisticUpdate.test.ts` 测试。
- **REQ-K1-3** 变更事件系统

  - **AC**: 数据变更时发送类型化事件（RequirementCreated、RelationshipDeleted等）；视图组件订阅相关事件进行局部更新；支持事件的撤销/重做；**TDD**: 先写 `ChangeEventSystem.test.ts` 测试。

### Story K2｜多视图同步机制

**As** 用户 **I want** 所有视图实时同步 **so that** 操作一个视图其他视图立即反映。

**Requirements**

- **REQ-K2-1** 选择状态同步

  - **AC**: 任一视图选中需求时，其他视图高亮同一需求；支持多选状态同步；选择状态变更时间<100ms；**TDD**: 先写 `SelectionSync.test.ts` 测试。
- **REQ-K2-2** 结构变更同步

  - **AC**: 创建/删除需求或关系时，所有视图立即更新结构；树视图重新计算层级；图视图重新布局；表视图更新行数据；**TDD**: 先写 `StructureSync.test.ts` 测试。
- **REQ-K2-3** 编辑状态管理

  - **AC**: 任一视图编辑需求时，其他视图显示编辑状态指示；防止并发编辑冲突；编辑完成时同步最新数据；**TDD**: 先写 `EditStateManagement.test.ts` 测试。

---

## 验收测试清单（Phase 1核心）

### 架构验收（基于实施文档）

- **T-TECHSPEC-01**: 技术规格书CRUD操作完整，包含版本管理
- **T-DOUBLETREE-01**: 双树界面布局正确，上下分屏，支持拖拽调节
- **T-RELATIONSHIP-TREE-01**: 基于关系的树构建算法正确，支持多根
- **T-ORPHAN-DETECTION-01**: 孤立需求识别准确，在UI中特殊标识
- **T-SSOT-SYNC-01**: 多视图数据同步时间<100ms，选择状态一致

### 建模功能验收

- **T-DEFINITION-USAGE-01**: Definition拖拽创建Usage功能正常
- **T-PACKAGE-IMPORT-01**: 需求包整体导入功能，ID重映射正确
- **T-DRAG-RELATIONSHIP-01**: 拖拽创建关系，弹框选择类型
- **T-BULK-OPERATIONS-01**: 表视图批量操作功能（状态更新、删除）
- **T-INLINE-EDIT-01**: 表视图行内编辑保存正确

### 性能验收

- **T-LARGE-DATA-01**: 1000个需求时UI响应时间<2秒
- **T-CIRCULAR-DEPENDENCY-01**: 循环依赖检测阻止非法关系创建

---

## TDD开发流程规范

### 测试优先原则

1. **Red**: 先写失败的测试用例，明确需求规格
2. **Green**: 写最少代码让测试通过
3. **Refactor**: 重构代码提高质量，保持测试通过

### 测试分层策略

```
Unit Tests (单元测试)
├── Service Layer Tests (业务逻辑)
│   ├── TechSpecService.test.ts
│   ├── RequirementPackageService.test.ts  
│   └── RequirementTreeService.test.ts
├── Controller Tests (API接口)
│   ├── TechSpecController.test.ts
│   └── RequirementPackageController.test.ts
└── Utility Tests (工具函数)
    ├── TreeBuilder.test.ts
    └── DragDropService.test.ts

Integration Tests (集成测试)  
├── API Integration Tests
└── Database Integration Tests

E2E Tests (端到端测试)
├── RequirementModeling.e2e.ts
├── DragDropModeling.e2e.ts  
└── MultiViewSync.e2e.ts
```

### 测试覆盖率目标

- **单元测试覆盖率**: ≥90%
- **API接口覆盖率**: 100%（所有端点和状态码）
- **业务规则覆盖率**: 100%（所有验证规则）
- **关键路径覆盖率**: 100%（创建、编辑、关系建立）

---

## Phase 1 开发里程碑

### Milestone 0: MVP基础功能完善（1周）**【紧急优先】**

- ✅ RequirementDefinition/Usage CRUD前后端联通（字段标准化已完成）
- ✅ 三视图真实数据集成
- ✅ 错误处理和用户反馈完善
- ✅ 前端UI组件完全集成（确保requirementDefinition字段正确）

### Milestone 1: 技术规格书管理（2周）

- ✅ TechSpec CRUD API
- ✅ 项目级数据隔离
- ✅ 需求与技术规格书关联

### Milestone 2: 需求库和包管理（2周）

- ✅ Definition库分类管理
- ✅ RequirementPackage CRUD
- ✅ 批量导入导出功能

### Milestone 3: 关系驱动建模（3周）

- ✅ 基于关系的树构建
- ✅ 拖拽建模交互
- ✅ 循环依赖检测

### Milestone 4: 多视图界面（3周）

- ✅ 双树建模界面
- ✅ 增强的图视图和表视图
- ✅ SSOT数据同步机制

### Milestone 5: 验证和优化（2周）

- ✅ 扩展验证规则
- ✅ 性能优化
- ✅ 用户体验提升

**总计开发周期**: 13周（含1周紧急完善），计划完成时间: 2025年11月下旬

---

## 成功标准

**功能完整性**:

- 完整支持SysML 2.0需求建模流程
- Definition-Usage模式完全实现
- 基于关系的层级管理

**性能指标**:

- 支持2000个需求的大规模模型
- UI交互响应时间<100ms
- 数据同步延迟<50ms

**质量保证**:

- 测试覆盖率>90%
- 零关键缺陷
- API稳定性100%
- 保持MVP基线217个测试全部通过

**用户体验**:

- 界面操作符合常见建模工具习惯
- 提供完整的操作文档和示例
- 拖拽和双击等交互响应及时

Phase 1 完成后，平台将具备完整的企业级需求建模能力，为后续Phase 2的参数计算和约束验证功能奠定坚实基础。
