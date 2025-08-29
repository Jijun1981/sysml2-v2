# 需求建模架构设计 v2

## 一、核心概念：技术规格书与需求结构

### 1.1 技术规格书（TechSpec）- 项目容器

```typescript
// 技术规格书 - 项目级容器（不是树的根）
interface TechnicalSpecification {
  id: string;
  name: string;              // 如："电动汽车控制系统技术规格书"
  version: string;            // "1.0.0"
  author: string;             // 创建人
  description: string;        // 描述
  createdAt: Date;
  modifiedAt: Date;
  status: 'draft' | 'review' | 'approved' | 'released';
  
  // 包含的需求（需求自己形成树）
  requirements: RequirementUsage[];
  // 关系
  relationships: Relationship[];
}

// RequirementUsage - 需求实例（无parent属性）
interface RequirementUsage {
  id: string;
  name: string;
  text: string;
  
  // Definition关联 - 知道从哪个模板创建
  definitionId?: string;     // 关联的Definition ID（可为null表示独立需求）
  definitionName?: string;   // Definition名称（冗余便于显示）
  
  // 生命周期状态管理（工程实践需要）
  status?: RequirementStatus;
  
  // 注意：没有parentId！层级通过关系表达
  // 符合SysML 2.0标准设计
  
  // SysML标准属性
  reqId?: string;           // 需求标识符
  subject?: any;            // 需求主体
  stakeholder?: string[];   // 利益相关者
  
  // 元数据
  createdAt?: Date;
  modifiedAt?: Date;
  createdBy?: string;
}

// 需求状态枚举（符合工程实践）
enum RequirementStatus {
  DRAFT = 'draft',           // 草稿
  REVIEW = 'review',          // 评审中
  APPROVED = 'approved',      // 已批准
  IMPLEMENTED = 'implemented', // 已实现
  VERIFIED = 'verified',      // 已验证
  DEPRECATED = 'deprecated'   // 已废弃
}
```

**设计理念**：
- 技术规格书是**项目容器**，不是需求树的根
- 需求自己形成树结构，可以有多个根
- Definition是Usage的"模板来源"属性
- 允许孤立需求存在，但要识别并标记

### 1.2 基于关系的层级识别

```typescript
// 通过关系判断根需求（没有被derive或refine的需求）
function isRootRequirement(req: RequirementUsage, relationships: Relationship[]): boolean {
  return !relationships.some(rel => 
    rel.targetId === req.id && 
    (rel.type === 'derive' || rel.type === 'refine')
  );
}

// 判断是否为孤立需求（没有任何关系）
function isOrphanRequirement(req: RequirementUsage, relationships: Relationship[]): boolean {
  return !relationships.some(rel => 
    rel.sourceId === req.id || rel.targetId === req.id
  );
}

// 从关系构建树形结构
function buildRequirementTree(
  requirements: RequirementUsage[], 
  relationships: Relationship[]
): TreeNode[] {
  // 找层级关系（derive和refine）
  const hierarchyRels = relationships.filter(r => 
    r.type === 'derive' || r.type === 'refine'
  );
  
  // 构建父子映射
  const childrenMap = new Map<string, string[]>();
  hierarchyRels.forEach(rel => {
    if (!childrenMap.has(rel.sourceId)) {
      childrenMap.set(rel.sourceId, []);
    }
    childrenMap.get(rel.sourceId)!.push(rel.targetId);
  });
  
  // 找根节点
  const roots = requirements.filter(req => 
    isRootRequirement(req, relationships)
  );
  
  // 递归构建树
  function buildNode(req: RequirementUsage): TreeNode {
    return {
      ...req,
      children: (childrenMap.get(req.id) || [])
        .map(childId => requirements.find(r => r.id === childId))
        .filter(Boolean)
        .map(buildNode)
    };
  }
  
  return roots.map(buildNode);
}
```

### 1.3 Definition与Usage的关系

```typescript
// Definition是Usage的模板来源
interface RequirementDefinition {
  id: string;
  name: string;
  text: string;           // 模板文本（可含占位符）
  category: string;       // 分类
  parameters?: Parameter[];  // 可选参数定义
}

// Usage创建时记录来源
function createUsageFromDefinition(def: RequirementDefinition): RequirementUsage {
  return {
    id: generateId(),
    name: def.name + "_instance",
    text: def.text,
    
    // 记录来源Definition
    definitionId: def.id,
    definitionName: def.name
    
    // 注意：没有parentId，层级通过derive/refine关系建立
  };
}
```

## 二、界面布局：双树结构

### 2.1 左侧面板设计

```
┌─────────────────────────────┐
│         左侧面板             │
├─────────────────────────────┤
│  [技术规格书：xxx项目] ▼    │
│  ├─ 🏷️[根] 系统需求        │
│  │  ├─ REQ-001 登录需求     │
│  │  ├─ REQ-002 性能需求     │
│  │  │  ├─ REQ-002.1 响应   │
│  │  │  └─ REQ-002.2 吞吐   │
│  │  └─ REQ-003 安全需求     │
│  ├─ 🏷️[根] 子系统需求      │
│  │  └─ REQ-100 接口需求     │
│  └─ ⚠️[孤立] REQ-999       │
├─────────────────────────────┤
│  [需求库/模板] ▼            │
│  ├─ 标准模板                │
│  │  ├─ 功能需求模板         │
│  │  ├─ 性能需求模板         │
│  │  └─ 安全需求模板         │
│  └─ 需求包                  │
│     ├─ ISO26262包(10个)     │
│     └─ 通用Web包(15个)      │
└─────────────────────────────┘
```

### 2.2 两树的本质区别

| 属性 | Usage树（上） | Definition树（下） |
|------|--------------|-------------------|
| **内容** | 实际需求实例 | 需求模板/包 |
| **状态** | 项目特定 | 可复用资源 |
| **操作** | 编辑/删除/状态管理 | 拖拽使用 |
| **数据** | RequirementUsage | RequirementDefinition |
| **显示** | 树形层级（derive关系） | 分类列表 |

## 三、需求包（Requirement Package）设计

### 3.1 需求包概念（包含关系）

```typescript
// 需求包 - 批量复用的单位（包含节点和关系）
interface RequirementPackage {
  id: string;
  name: string;           // "ISO 26262 ASIL-D安全需求包"
  description: string;    
  category: string;       // "automotive-safety"
  sourceProject?: string; // 来源项目
  
  // 包含的内容（完整导出）
  content: {
    // 可选：包含的Definition模板
    definitions?: RequirementDefinition[];
    
    // 主要：需求节点
    usages: RequirementUsage[];
    
    // 重要：保留所有关系
    relationships: Relationship[];  // derive, refine, trace等
    
    // 结构信息
    structure: {
      rootNodes: string[];  // 根节点ID列表
      hierarchy: any;       // 层级结构
    }
  };
  
  // 元数据
  metadata: {
    version: string;
    author: string;
    createdAt: Date;
    tags: string[];
  };
}

// 关系定义（增强可追溯性）
interface Relationship {
  id: string;
  type: 'derive' | 'refine' | 'trace' | 'satisfy';
  sourceId: string;      // 源需求ID
  targetId: string;      // 目标需求ID
  
  // 增强的关系元数据（工程价值）
  rationale?: string;    // 关系理由/依据（为什么建立这个关系）
  confidence?: number;   // 置信度（0-1，satisfy关系特别有用）
  status?: RelationStatus; // 关系状态
  
  // 审计信息
  createdAt?: Date;
  createdBy?: string;
  verifiedBy?: string;   // 验证人（特别是satisfy关系）
  verifiedAt?: Date;
}

// 关系状态
enum RelationStatus {
  PROPOSED = 'proposed',     // 提议的
  CONFIRMED = 'confirmed',   // 已确认
  VERIFIED = 'verified',     // 已验证（特别是satisfy）
  INVALID = 'invalid'        // 无效的
}
```

### 3.2 需求包 vs 单个Definition

| 对比项 | 单个Definition | 需求包Package |
|--------|---------------|---------------|
| **粒度** | 单个需求模板 | 5-20个相关需求 |
| **关系** | 无 | **包含完整关系** |
| **使用** | 创建单个Usage | 创建需求子树+关系 |
| **场景** | 零散需求 | 标准合规/模块化需求 |

### 3.3 创建需求包的交互流程

```typescript
// 创建需求包对话框
const CreatePackageDialog = ({ project, onConfirm }) => {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [packageName, setPackageName] = useState("");
  
  // 树形选择组件
  const handleNodeSelect = (nodeId: string, checked: boolean) => {
    if (checked) {
      // 选中节点
      selectedIds.add(nodeId);
      // 选中根节点=全选子树
      if (isRootNode(nodeId)) {
        selectAllDescendants(nodeId);
      }
    } else {
      selectedIds.delete(nodeId);
      if (isRootNode(nodeId)) {
        deselectAllDescendants(nodeId);
      }
    }
  };
  
  // 创建需求包
  const createPackage = () => {
    const selectedReqs = Array.from(selectedIds)
      .map(id => findRequirement(id, project.requirements));
    
    // 提取相关关系（只包含选中节点间的关系）
    const relevantRelationships = project.relationships.filter(rel => 
      selectedIds.has(rel.sourceId) && selectedIds.has(rel.targetId)
    );
    
    const reqPackage: RequirementPackage = {
      id: generateId(),
      name: packageName,
      sourceProject: project.id,
      content: {
        usages: selectedReqs,
        relationships: relevantRelationships,  // 保留关系！
        structure: {
          rootNodes: findRoots(selectedReqs),
          hierarchy: buildHierarchy(selectedReqs)
        }
      },
      metadata: {
        author: currentUser,
        createdAt: new Date(),
        version: "1.0",
        tags: []
      }
    };
    
    onConfirm(reqPackage);
  };
  
  return (
    <Modal title="创建需求包">
      <Input 
        placeholder="需求包名称"
        value={packageName}
        onChange={e => setPackageName(e.target.value)}
      />
      
      {/* 树形多选列表 */}
      <Tree
        checkable
        defaultExpandAll
        treeData={buildTreeData(project.requirements)}
        onCheck={handleNodeSelect}
      />
      
      <div className="summary">
        <p>已选择 {selectedIds.size} 个需求</p>
        <p>包含 {relevantRelationships.length} 个关系</p>
      </div>
      
      <Button onClick={createPackage}>
        创建需求包
      </Button>
    </Modal>
  );
};
```

## 四、建模交互流程

### 4.1 需求创建的三种方式

```
1. 从Definition创建（基于模板）
   Definition → 拖拽/选择 → 创建Usage(definitionId=xxx)

2. 创建独立需求（SysML 2.0原生支持）
   空白创建 → 创建Usage(definitionId=null) → 可后续通过关系挂载

3. 从需求包导入（批量）
   Package → 拖拽 → 创建Usage子树 + 关系 → 自动挂载到根
```

#### 独立需求的支持

```typescript
// 创建独立需求（不基于Definition）
function createStandaloneRequirement(data: {
  name: string;
  text: string;
}): RequirementUsage {
  return {
    id: generateId(),
    name: data.name,
    text: data.text,
    
    // 关键：没有Definition关联
    definitionId: null,
    definitionName: null
  };
}

// 通过关系建立层级
function establishHierarchy(
  parentReq: RequirementUsage, 
  childReq: RequirementUsage,
  relationType: 'derive' | 'refine' = 'derive'
) {
  createRelationship({
    type: relationType,
    sourceId: parentReq.id,  // source是父
    targetId: childReq.id    // target是子
  });
}
```

### 4.2 需求包导入处理

```typescript
// 拖拽需求包的处理（保留关系）
function handlePackageDrop(pkg: RequirementPackage, dropPosition: Point) {
  // 1. ID映射表（旧ID -> 新ID）
  const idMapping = new Map<string, string>();
  
  // 2. 创建所有需求的副本（新ID）
  const copiedRequirements = pkg.content.usages.map(req => {
    const newId = generateNewId();
    idMapping.set(req.id, newId);
    
    return {
      ...req,
      id: newId,
      originalId: req.id,
      // 保留definitionId关联
      definitionId: req.definitionId,
      definitionName: req.definitionName
    };
  });
  
  // 3. 重建关系（使用新ID）
  const copiedRelationships = pkg.content.relationships.map(rel => ({
    ...rel,
    id: generateNewId(),
    sourceId: idMapping.get(rel.sourceId),
    targetId: idMapping.get(rel.targetId)
  }));
  
  // 4. 找到包内的根节点
  const packageRootIds = copiedRequirements
    .filter(req => !copiedRelationships.some(rel => 
      rel.targetId === req.id && 
      (rel.type === 'derive' || rel.type === 'refine')
    ))
    .map(r => r.id);
  
  // 5. 自动挂载到项目根节点（通过创建derive关系）
  const projectRoots = getRootRequirements(currentProject.currentModel.usages, currentProject.currentModel.relationships);
  
  if (projectRoots.length > 0 && packageRootIds.length > 0) {
    // 为每个包根节点创建到项目根的derive关系
    packageRootIds.forEach(pkgRootId => {
      copiedRelationships.push({
        id: generateNewId(),
        type: 'derive',
        sourceId: projectRoots[0].id,  // 项目根作为source
        targetId: pkgRootId            // 包根作为target
      });
    });
    message.info('需求包已通过derive关系挂载到项目根节点');
  }
  
  // 6. 添加到项目
  currentProject.currentModel.usages.push(...copiedRequirements);
  currentProject.currentModel.relationships.push(...copiedRelationships);
  
  // 7. 更新所有视图
  updateAllViews();
}
```

### 4.3 数据流设计

```typescript
// 核心数据模型
interface ProjectModel {
  // 技术规格书（容器，不是根）
  techSpec: TechnicalSpecification;
  
  // 需求库（资源）
  library: {
    definitions: RequirementDefinition[];
    packages: RequirementPackage[];
  };
  
  // 当前模型（工作区）
  currentModel: {
    usages: RequirementUsage[];      // 扁平列表
    relationships: Relationship[];    // 关系列表
    tree: TreeNode[];                // 树形结构（计算属性）
  };
}

// 拖拽处理
function handleDrop(source: Definition | Package, position: Point) {
  if (isDefinition(source)) {
    // 创建单个Usage
    const usage = createUsageFromDefinition(source);
    currentProject.currentModel.usages.push(usage);
  } else if (isPackage(source)) {
    // 导入需求包
    handlePackageDrop(source, position);
  }
  
  // 更新所有视图
  updateViews();
}
```

## 五、需求来源追溯

### 5.1 需求来源管理

每个需求的来源通过definitionId追溯：

```typescript
// 注意：RequirementUsage接口已在1.1节定义，这里不重复定义
// 来源通过definitionId字段追踪：
// - definitionId有值：从Definition创建
// - definitionId为null：独立创建的需求

// 获取需求来源信息
function getRequirementSource(req: RequirementUsage): string {
  if (req.definitionId) {
    return `来自模板: ${req.definitionName || req.definitionId}`;
  } else {
    return '独立创建的需求';
  }
}
```

### 5.2 根节点判定规则

```typescript
// 判断是否为根需求（基于关系，不是parent属性）
function isRootRequirement(
  req: RequirementUsage, 
  relationships: Relationship[]
): boolean {
  // 没有被derive或refine的需求就是根
  return !relationships.some(rel => 
    rel.targetId === req.id && 
    (rel.type === 'derive' || rel.type === 'refine')
  );
}

// 获取需求树（使用已定义的buildRequirementTree函数）
function getRequirementTree(
  currentModel: { usages: RequirementUsage[]; relationships: Relationship[]; }
): TreeNode[] {
  return buildRequirementTree(
    currentModel.usages, 
    currentModel.relationships
  );
}
```

## 六、实施方案

### 6.1 Phase 1 核心功能

```typescript
// 1. 技术规格书管理
class TechSpecManager {
  create(name: string, author: string): TechnicalSpecification;
  load(id: string): TechnicalSpecification;
  save(spec: TechnicalSpecification): void;
}

// 2. 双树视图组件
const RequirementExplorer = () => {
  return (
    <div className="requirement-explorer">
      {/* 上部：Usage树 */}
      <div className="usage-tree">
        <TreeView 
          title="技术规格书"
          data={currentProject.currentModel.usages}
          relationships={currentProject.currentModel.relationships}
          onSelect={handleUsageSelect}
          onDrop={handleDropToUsage}
        />
      </div>
      
      {/* 下部：Definition库 */}
      <div className="definition-library">
        <Tabs>
          <TabPane tab="需求模板" key="definitions">
            <DefinitionList 
              definitions={library.definitions}
              onDragStart={handleDragDefinition}
            />
          </TabPane>
          <TabPane tab="需求包" key="packages">
            <PackageList 
              packages={library.packages}
              onDragStart={handleDragPackage}
            />
          </TabPane>
        </Tabs>
      </div>
    </div>
  );
};

// 3. 建模画布
const ModelingCanvas = () => {
  return (
    <ReactFlow
      nodes={usageNodes}
      edges={relationships}
      onDrop={handleCanvasDrop}
      onConnect={handleConnect}
    />
  );
};
```

### 6.2 数据存储结构

```json
{
  "techSpec": {
    "id": "ts-001",
    "name": "电动汽车控制系统技术规格书",
    "version": "1.0.0",
    "author": "张三"
  },
  "currentModel": {
    "usages": [
      {
        "id": "req-001",
        "type": "RequirementUsage",
        "name": "系统性能需求",
        "definitionId": "def-performance",
        "status": "approved"
      }
    ],
    "relationships": [
      {
        "id": "rel-001",
        "type": "derive",
        "sourceId": "req-root",
        "targetId": "req-001",
        "rationale": "基于系统架构需求导出"
      }
    ],
    "tree": []
  },
  "library": {
    "definitions": [...],
    "packages": [
      {
        "id": "pkg-iso26262",
        "name": "ISO 26262安全需求包",
        "content": {
          "usages": [...],
          "relationships": [...]
        }
      }
    ]
  }
}
```

## 七、关键设计决策

### 7.1 为什么需要TechSpec？
- **需求必须有归属**：避免孤立需求
- **项目级管理**：版本、状态、作者等元数据
- **符合工程习惯**：对应传统的需求规格说明书

### 7.2 为什么是双树结构？
- **上树（Usage）**：当前项目的需求实例，实际工作内容
- **下树（Definition）**：可复用资源库，提高效率

### 7.3 为什么需要需求包？
- **批量复用**：一次导入相关的10-20个需求
- **保持关系**：需求间的derive关系一并导入
- **领域知识**：封装行业最佳实践

## 八、单一数据源与多视图同步（SSOT）

### 8.1 单一数据源设计

```typescript
// 核心数据模型 - 单一真相源
class RequirementDataStore {
  private state = {
    techSpec: TechnicalSpecification;
    usages: Map<string, RequirementUsage>;
    definitions: Map<string, RequirementDefinition>;
    relationships: Map<string, Relationship>;
    packages: Map<string, RequirementPackage>;
  };
  
  // 所有修改通过统一接口
  dispatch(action: Action): void {
    switch(action.type) {
      case 'CREATE_USAGE':
        this.createUsage(action.payload);
        this.notifyAllViews();
        break;
      case 'CREATE_RELATIONSHIP':
        this.createRelationship(action.payload);
        this.notifyAllViews();
        break;
      // ... 其他操作
    }
  }
  
  // 通知所有视图更新
  private notifyAllViews() {
    this.listeners.forEach(listener => listener.update(this.state));
  }
}
```

### 8.2 多视图操作映射

| 操作 | 树视图 | 图视图 | 表视图 |
|------|--------|--------|--------|
| **创建derive关系** | 拖拽节点到另一节点 | 连线（箭头） | 下拉选择源需求 |
| **删除需求** | 右键删除 | Delete键/右键 | 行删除按钮 |
| **修改属性** | 属性面板 | 双击节点编辑 | 单元格编辑 |
| **批量操作** | Shift多选 | 框选 | Checkbox多选 |

### 8.3 视图同步机制

```typescript
// 树视图
class TreeView implements ViewListener {
  update(state: DataState) {
    // 重建树结构
    this.tree = buildTreeFromUsages(state.usages, state.relationships);
    this.render();
  }
  
  // 创建derive关系
  onNodeDrop(sourceId: string, targetId: string) {
    dataStore.dispatch({
      type: 'CREATE_RELATIONSHIP',
      payload: { type: 'derive', source: sourceId, target: targetId }
    });
  }
}

// 图视图
class GraphView implements ViewListener {
  update(state: DataState) {
    // 更新节点和边
    this.nodes = convertUsagesToNodes(state.usages);
    this.edges = convertRelationshipsToEdges(state.relationships);
    this.reactFlow.setNodes(this.nodes);
    this.reactFlow.setEdges(this.edges);
  }
  
  // 连线创建关系
  onConnect(connection: Connection) {
    dataStore.dispatch({
      type: 'CREATE_RELATIONSHIP',
      payload: { 
        type: getRelationType(connection),
        source: connection.source,
        target: connection.target
      }
    });
  }
}

// 表视图
class TableView implements ViewListener {
  update(state: DataState) {
    // 更新表格数据
    this.dataSource = Array.from(state.usages.values());
    this.table.setDataSource(this.dataSource);
  }
  
  // 下拉选择创建关系
  onDeriveFromSelect(targetId: string, sourceId: string) {
    dataStore.dispatch({
      type: 'CREATE_RELATIONSHIP',
      payload: { type: 'derive', source: sourceId, target: targetId }
    });
  }
}
```

## 九、SysML 2.0标准图形表示

### 9.1 需求节点表示（参考Syson实现）

```typescript
// 需求节点的标准展现形式
interface RequirementNodeDisplay {
  // 节点样式
  style: {
    shape: 'rectangle';  // 矩形
    borderStyle: 'solid' | 'dashed';  // Definition虚线，Usage实线
    backgroundColor: '#f0f8ff';  // 浅蓝色背景
  };
  
  // 节点内容 - 格子式展开
  compartments: {
    header: {
      stereotype: '«requirement»';  // 构造型
      name: string;  // 需求名称
      id: string;    // 需求ID
    };
    
    // 可展开的格子区域
    body: {
      text?: CompartmentSection;      // 需求文本
      subject?: CompartmentSection;    // 主体
      stakeholder?: CompartmentSection; // 利益相关者
      constraint?: CompartmentSection;  // 约束
      // 可扩展更多section
    };
  };
}

// 格子区域定义
interface CompartmentSection {
  label: string;      // 区域标签
  content: any;       // 内容
  expanded: boolean;  // 是否展开
  visible: boolean;   // 是否显示
}
```

### 9.2 React Flow中的实现

```tsx
// 自定义需求节点组件
const RequirementNode = ({ data }: NodeProps) => {
  const [expandedSections, setExpandedSections] = useState({
    text: true,
    subject: false,
    stakeholder: false,
    constraint: false
  });
  
  return (
    <div className={`requirement-node ${data.type}`}>
      {/* 头部 */}
      <div className="node-header">
        <span className="stereotype">«requirement»</span>
        <span className="name">{data.name}</span>
        <span className="id">{data.reqId}</span>
      </div>
      
      {/* 可展开的格子区域 */}
      <div className="node-compartments">
        {/* 需求文本区 */}
        <Compartment
          label="text"
          expanded={expandedSections.text}
          onToggle={() => toggleSection('text')}
        >
          <div className="text-content">{data.text}</div>
        </Compartment>
        
        {/* 主体区 */}
        {data.subject && (
          <Compartment
            label="subject"
            expanded={expandedSections.subject}
            onToggle={() => toggleSection('subject')}
          >
            <div className="subject-list">
              {data.subject.map(s => <div key={s.id}>{s.name}</div>)}
            </div>
          </Compartment>
        )}
        
        {/* 约束区 */}
        {data.constraints && (
          <Compartment
            label="constraints"
            expanded={expandedSections.constraint}
            onToggle={() => toggleSection('constraint')}
          >
            <div className="constraint-list">
              {data.constraints.map(c => <div key={c.id}>{c.expression}</div>)}
            </div>
          </Compartment>
        )}
      </div>
      
      {/* 连接点 */}
      <Handle type="source" position={Position.Right} />
      <Handle type="target" position={Position.Left} />
    </div>
  );
};

// 格子组件
const Compartment = ({ label, expanded, onToggle, children }) => (
  <div className={`compartment ${expanded ? 'expanded' : 'collapsed'}`}>
    <div className="compartment-header" onClick={onToggle}>
      <span className="toggle-icon">{expanded ? '▼' : '▶'}</span>
      <span className="label">{label}</span>
    </div>
    {expanded && (
      <div className="compartment-body">
        {children}
      </div>
    )}
  </div>
);
```

### 9.3 关系的图形表示

```typescript
// SysML 2.0标准关系样式
const relationshipStyles = {
  derive: {
    type: 'smoothstep',
    style: { 
      stroke: '#2196F3',      // 蓝色
      strokeDasharray: '5,5'  // 虚线
    },
    markerEnd: 'arrowclosed',
    label: '«derive»'
  },
  
  satisfy: {
    type: 'straight',
    style: { 
      stroke: '#4CAF50',      // 绿色
      strokeWidth: 2          // 实线
    },
    markerEnd: 'arrowclosed',
    label: '«satisfy»'
  },
  
  refine: {
    type: 'smoothstep',
    style: { 
      stroke: '#FF9800',      // 橙色
      strokeWidth: 1          // 细线
    },
    markerEnd: 'arrowopen',
    label: '«refine»'
  },
  
  trace: {
    type: 'straight',
    style: { 
      stroke: '#9E9E9E',      // 灰色
      strokeDasharray: '2,2'  // 点线
    },
    markerEnd: 'arrowopen',
    label: '«trace»'
  }
};
```

## 十、设计决策的理由

### 10.1 为什么添加status属性

**理由**：
1. **工程需要**：需求有生命周期，需要跟踪状态
2. **SysML兼容**：SysML 2.0支持元数据扩展
3. **实用价值**：帮助团队管理需求成熟度

**使用场景**：
```typescript
// 筛选已批准的需求进行实现
const approvedReqs = requirements.filter(r => r.status === 'approved');

// 验证时更新状态
function verifyRequirement(req: RequirementUsage) {
  req.status = 'verified';
  req.modifiedAt = new Date();
}
```

### 10.2 为什么添加rationale属性

**理由**：
1. **可追溯性**：理解为什么建立关系
2. **知识保留**：记录决策依据
3. **合规需要**：某些标准要求记录理由

**使用场景**：
```typescript
// 创建derive关系时说明理由
createRelationship({
  type: 'derive',
  sourceId: 'sys-req-1',
  targetId: 'module-req-1',
  rationale: '系统响应时间需求分解到各模块，模块A负责数据处理部分',
  confidence: 0.9
});

// satisfy关系记录验证信息
createRelationship({
  type: 'satisfy',
  sourceId: 'design-1',
  targetId: 'req-1',
  rationale: '通过仿真测试验证满足响应时间要求',
  confidence: 0.95,
  status: 'verified',
  verifiedBy: 'QA Team',
  verifiedAt: new Date()
});
```

## 十一、与SysML 2.0标准的对应

| 我们的概念 | SysML 2.0对应 | 说明 |
|-----------|--------------|------|
| TechSpec | Package | 顶层包含器 |
| Usage树 | Requirement hierarchy | 需求层级 |
| Definition库 | Requirement library | 可复用库 |
| 需求包 | Package of requirements | 需求集合 |
| derive关系 | DeriveRequirement | 需求派生 |
| status属性 | 元数据扩展 | 工程实践扩展 |
| rationale | Documentation | 关系文档化 |
| 节点展现 | Compartment notation | 格子式表示法 |
| SSOT | Model repository | 模型仓库 |

## 十二、关键实现要点

1. **单一数据源**：所有视图操作都通过中央Store，保证数据一致性
2. **关系驱动层级**：没有parentId，完全通过derive/refine关系构建树
3. **状态管理**：需求和关系都有状态，支持生命周期管理
4. **可追溯性**：通过rationale记录决策依据
5. **操作等价性**：同一操作在不同视图有不同表现形式但效果相同
6. **格子式展现**：节点信息分区显示，可折叠展开
7. **标准图形符号**：遵循SysML 2.0规范的图形表示

这样的设计既符合SysML 2.0标准，又满足工程实践需要！