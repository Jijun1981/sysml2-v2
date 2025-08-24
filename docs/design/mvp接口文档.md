# SysML v2 建模平台 MVP API文档

## 文档信息

* **版本** : 3.0
* **日期** : 2025-08-24
* **状态** : Pilot元模型迁移Phase 4完成 - 通用元素接口实现
* **更新** : Phase 1-4完成（80%），支持182个SysML类型的通用接口
* **基础URL** : `http://localhost:8080/api/v1`
* **存储方式** : JSON文件系统

---

## 技术架构说明

### Pilot元模型集成状态

* **Phase 1 ✅ 完成**: 完整Pilot元模型注册
  - 成功加载 `SysML.ecore` 文件（182个EClass）
  - 命名空间: `https://www.omg.org/spec/SysML/20250201`
  - 完整继承链验证通过: Element → ... → RequirementDefinition
  
* **Phase 2 ✅ 完成**: 动态EMF操作与字段映射
  - Service层工厂方法实现完成
  - DTO与Pilot字段映射实现完成
  - RequirementDefinition/Usage CRUD完成
  - Trace→Dependency映射完成

* **Phase 3 ✅ 完成**: REST接口PATCH支持
  - PATCH部分更新接口实现
  - 只更新提供的字段，其他字段保持不变
  - null值忽略，只读字段保护

* **Phase 4 ✅ 完成**: 通用元素接口实现
  - 一个接口处理182个SysML类型
  - 零代码扩展验证成功
  - 创建PartUsage无需专门代码

### 字段映射关系

| API字段 | Pilot元模型字段 | 说明 |
|---------|----------------|------|
| `reqId` | `declaredShortName` | 需求业务标识 |
| `name` | `declaredName` | 显示名称 |
| `text` | `documentation.body` | 需求文本 |
| `type` | API概念 | definition/usage |
| `eClass` | 运行时类名 | RequirementDefinition/RequirementUsage |

### API层与Pilot层概念映射

| API概念 | Pilot元模型概念 | 说明 |
|---------|----------------|------|
| Trace | Dependency | 追溯关系在Pilot中为Dependency |
| fromId | source.elementId | 追溯源端 |
| toId | target.elementId | 追溯目标端 |
| derive/satisfy/refine/trace | 扩展属性/stereotype | 追溯类型 |

---

## 1. 通用约定

### 1.1 请求格式

* **Content-Type** : `application/json`
* **Accept** : `application/json`
* **字符编码** : UTF-8

### 1.2 响应格式

```json
// 成功响应
{
    "data": { ... },
    "timestamp": "2025-01-15T10:30:00.000Z"
}

// 错误响应
{
    "error": {
        "code": "ERROR_CODE",
        "message": "Human readable message",
        "details": { ... }
    },
    "timestamp": "2025-01-15T10:30:00.000Z"
}
```

### 1.3 HTTP状态码

* `200 OK`: 成功
* `201 Created`: 创建成功
* `204 No Content`: 删除成功
* `400 Bad Request`: 参数错误
* `404 Not Found`: 资源不存在
* `409 Conflict`: 业务冲突

### 1.4 分页参数

* `page`: 页码，从0开始
* `size`: 每页大小，1-200，默认50
* `sort`: 排序，格式 `field,direction`

### 动态EMF模式的核心价值

通过Phase 4的通用元素接口，我们实现了：

1. **1个接口 vs 182个接口**: 传统方式需要为每个SysML类型编写独立的CRUD接口
2. **1个Service vs 182个Service**: 传统方式需要为每个类型编写独立的业务逻辑
3. **自动支持新类型**: 当SysML标准更新时，无需修改任何代码
4. **完整属性支持**: 自动支持每个类型的所有属性和关系
5. **标准兼容**: 完全基于OMG官方Pilot实现，确保标准兼容性

---

## 2. 健康检查接口

### 2.1 系统健康检查

**GET** `/health`

**响应示例**

```json
{
    "status": "UP",
    "buildVersion": "0.1.0-MVP",
    "gitCommit": "a3f4d5e",
    "serverTimeUtc": "2025-01-15T10:30:00.000Z",
    "storage": "JSON_FILE_SYSTEM"
}
```

### 2.2 模型健康检查

**GET** `/health/model`

**查询参数**
- `detailed` (可选): `true`返回完整EClass列表，默认`false`

**响应示例（摘要模式）**

```json
{
    "status": "UP",
    "totalCount": 276,
    "packages": [
        {
            "name": "sysml",
            "nsURI": "https://www.omg.org/spec/SysML/20250201",
            "eClassCount": 276
        }
    ],
    "dataDirectory": "data/projects",
    "projectCount": 5,
    "totalElements": 247
}
```

---

## 3. 项目管理接口

### 3.1 导出项目

**GET** `/api/v1/projects/{pid}/export`

**路径参数**

* `pid`: 项目ID

**响应头**

```
Content-Type: application/json
Content-Disposition: attachment; filename="project-{pid}.json"
```

**响应示例**

```json
{
    "_version": "1.0",
    "eClass": "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
    "project": {
        "id": "proj-001",
        "name": "示例项目",
        "createdAt": "2025-01-15T09:00:00.000Z",
        "updatedAt": "2025-01-15T10:00:00.000Z"
    },
    "eContents": [
        {
            "eClass": "https://www.omg.org/spec/SysML/20250201#//RequirementDefinition",
            "@id": "R-001",
            "elementId": "R-001",
            "declaredShortName": "REQ-001",  // reqId in SysML 2.0
            "declaredName": "功能需求",
            "documentation": [{
                "@type": "Documentation",
                "body": "系统应该..."
            }],
            "qualifiedName": "proj-001::REQ-001",
            "isImplied": false,
            "isDerived": false
        }
    ]
}
```

### 3.2 导入项目

**POST** `/api/v1/projects/{pid}/import`

**请求体**

```json
{
    "_version": "1.0",
    "eContents": [ ... ]
}
```

**响应**

```json
{
    "imported": 15,
    "skipped": 0,
    "errors": [],
    "filePath": "data/projects/proj-001/model.json",
    "fileSize": "45.2KB"
}
```

---

## 4. 需求管理接口（已废弃 - 使用通用接口）

> ⚠️ **此章节已废弃**：专门的需求管理接口已被第7章的通用元素接口所替代。
>
> **迁移指南**：
> - 查询需求定义：`GET /api/v1/elements?type=RequirementDefinition`
> - 查询需求使用：`GET /api/v1/elements?type=RequirementUsage`  
> - 创建需求：`POST /api/v1/elements {"eClass": "RequirementDefinition"}`
> - 更新需求：`PATCH /api/v1/elements/{id}`
> - 删除需求：`DELETE /api/v1/elements/{id}`

### 4.1 查询需求列表（已废弃）

**~~GET~~ `/api/v1/projects/{pid}/requirements`** ❌

**新接口**: `GET /api/v1/elements?type=RequirementDefinition`

**迁移说明**: 使用通用接口查询，通过`type`参数指定元素类型

**响应示例**

```json
{
    "content": [
        {
            "@type": "sysml:RequirementDefinition",
            "@id": "R-001",
            "elementId": "R-001",
            "declaredShortName": "REQ-001",
            "declaredName": "功能需求",
            "documentation": [{
                "@type": "Documentation",
                "body": "系统应该..."
            }],
            "qualifiedName": "proj-001::REQ-001",
            "ownedRelationship": [],
            "subjectParameter": null,
            "assumedConstraint": [],
            "requiredConstraint": []
        }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 120,
    "totalPages": 3,
    "loadedFromFile": "data/projects/proj-001/model.json"
}
```

### 4.2 创建需求

**POST** `/api/v1/projects/{pid}/requirements`

**请求体（Definition - 简化DTO）**

```json
{
    "type": "definition",
    "reqId": "REQ-001",  // 映射到declaredShortName
    "name": "功能需求",     // 映射到declaredName
    "text": "系统应该提供用户登录功能",  // 映射到documentation.body
    "doc": "详细说明..."      // 额外的文档
}
```

**实际存储（完整Pilot模型）**

```json
{
    "@type": "sysml:RequirementDefinition",
    "@id": "R-001",
    "elementId": "R-001",
    "declaredShortName": "REQ-001",
    "declaredName": "功能需求",
    "documentation": [{
        "@type": "Documentation",
        "body": "系统应该提供用户登录功能",
        "locale": "zh-CN"
    }],
    "qualifiedName": "proj-001::Requirements::REQ-001",
    "owningRelatedElement": {"@id": "proj-001"},
    "owningMembership": {"@id": "m-001"},
    "ownedRelationship": [],
    "subjectParameter": null,
    "actorParameter": [],
    "stakeholderParameter": [],
    "assumedConstraint": [],
    "requiredConstraint": [],
    "framedConcern": [],
    "isImplied": false,
    "isDerived": false,
    "isEnd": false,
    "isAbstract": false,
    "isSufficient": false,
    "isIndividual": false,
    "isVariation": false
}
```

**请求体（Usage）**

```json
{
    "type": "usage",
    "of": "R-001",
    "name": "登录功能实例",
    "text": "Web端登录实现",
    "status": "draft"
}
```

**响应**

* 状态码: `201 Created`
* 响应头: `Location: /api/v1/projects/{pid}/requirements/{id}`

```json
{
    "id": "R-002",
    "eClass": "RequirementUsage",
    "of": {"$ref": "R-001"},
    "name": "登录功能实例",
    "createdAt": "2025-01-15T10:00:00.000Z",
    "savedToFile": "data/projects/proj-001/model.json",
    "_version": "1.0"
}
```

### 4.3 获取单个需求

**GET** `/api/v1/projects/{pid}/requirements/{id}`

**响应示例**

```json
{
    "id": "R-001",
    "eClass": "RequirementDefinition",
    "reqId": "REQ-001",
    "name": "功能需求",
    "text": "系统应该...",
    "doc": "详细文档",
    "tags": ["critical"],
    "subjectRef": null,
    "constraints": [],
    "assumptions": [],
    "createdAt": "2025-01-15T10:00:00.000Z",
    "updatedAt": "2025-01-15T10:00:00.000Z",
    "_version": "1.0",
    "loadedFromFile": "data/projects/proj-001/model.json"
}
```

### 4.4 更新需求

**PUT** `/api/v1/projects/{pid}/requirements/{id}`

**请求体（全量更新）**

```json
{
    "name": "更新后的名称",
    "text": "更新后的文本",
    "tags": ["updated", "critical"]
}
```

**PATCH** `/api/v1/projects/{pid}/requirements/{id}`

**请求体（部分更新）**

```json
{
    "text": "仅更新文本字段"
}
```

> **说明**：
> - PUT需要提供完整对象，未提供的字段会被设为默认值
> - PATCH仅更新请求体中提供的字段，其他字段保持不变

**响应示例**

```json
{
    "id": "R-001",
    "name": "更新后的名称",
    "text": "更新后的文本",
    "tags": ["updated", "critical"],
    "updatedAt": "2025-01-15T10:30:00.000Z",
    "savedToFile": "data/projects/proj-001/model.json"
}
```

### 4.5 删除需求

**DELETE** `/api/v1/projects/{pid}/requirements/{id}`

**响应**

* 成功: `204 No Content`
* 被引用: `409 Conflict`

```json
{
    "error": {
        "code": "REFERENCED",
        "message": "Cannot delete: referenced by other elements",
        "details": {
            "referencedBy": ["T-001", "T-002"],
            "filePath": "data/projects/proj-001/model.json"
        }
    }
}
```

---

## 5. 追溯管理接口（已废弃 - 使用通用接口）

> ⚠️ **此章节已废弃**：专门的追溯管理接口已被第7章的通用元素接口所替代。
>
> **迁移指南**：
> - 查询依赖关系：`GET /api/v1/elements?type=Dependency`
> - 创建依赖：`POST /api/v1/elements {"eClass": "Dependency", "attributes": {"source": "fromId", "target": "toId"}}`
> - 支持专门类型：`DeriveRequirement`、`Satisfy`、`Refine`等
> - 删除依赖：`DELETE /api/v1/elements/{id}`

> **重要说明**：API层使用"Trace"概念以符合需求管理习惯，内部映射到SysML Pilot的"Dependency"及专用关系类。
>
> **映射关系**：
> | API层 | Pilot层 | 说明 |
> |-------|---------|------|
> | Trace | Dependency | 基础追溯关系 |
> | trace.type='derive' | DeriveRequirement | 派生关系 |
> | trace.type='satisfy' | Satisfy | 满足关系 |
> | trace.type='refine' | Refine | 细化关系 |
> | trace.type='trace' | Dependency | 通用追溯 |
> | fromId | source.elementId | 关系源端 |
> | toId | target.elementId | 关系目标端 |

### 5.1 查询追溯关系

**GET** `/api/v1/requirements/{id}/traces`

**查询参数**

* `dir`: `in` | `out` | `both` (默认both)

**响应示例**

```json
{
    "traces": [
        {
            "@type": "sysml:Dependency",
            "@id": "D-001",
            "elementId": "D-001",
            "client": [{"@id": "R-001"}],
            "supplier": [{"@id": "R-002"}],
            "kind": "derive"  // 从annotation中提取
        },
        {
            "id": "T-002",
            "fromId": "R-001",
            "toId": "R-003",
            "type": "satisfy",
            "createdAt": "2025-01-15T10:00:00.000Z"
        }
    ],
    "loadedFromFile": "data/projects/proj-001/model.json"
}
```

### 5.2 创建追溯关系

**POST** `/api/v1/requirements/{id}/traces`

**请求体**

```json
{
    "toId": "R-003",
    "type": "derive"  // derive|satisfy|refine|trace
}
```

**实际存储（Pilot Dependency模型）**

```json
{
    "@type": "sysml:Dependency",
    "@id": "D-001",
    "elementId": "D-001",
    "client": [{"@id": "R-001"}],  // 依赖方
    "supplier": [{"@id": "R-003"}], // 被依赖方
    "owningRelatedElement": {"@id": "R-001"},
    "annotation": [{
        "@type": "Annotation",
        "annotatingElement": {
            "@type": "Comment",
            "body": "derive"  // 依赖类型作为注解
        }
    }]
}
```

**kind可选值（通过annotation实现）**

* `derive`: 派生
* `satisfy`: 满足
* `refine`: 细化
* `trace`: 追踪

**响应**

* **新建成功**: `201 Created` + Location header
* **重复请求**: `200 OK` (REQ-C3-3: 去重逻辑，返回既有对象)

**新建成功响应示例**
```json
{
    "id": "T-003",
    "fromId": "R-001",
    "toId": "R-003",
    "type": "derive",
    "createdAt": "2025-01-15T10:00:00.000Z",
    "savedToFile": "data/projects/proj-001/model.json"
}
```

**重复请求响应示例 (REQ-C3-3)**
```json
{
    "id": "T-001",
    "fromId": "R-001", 
    "toId": "R-003",
    "type": "derive",
    "createdAt": "2025-01-15T09:30:00.000Z",
    "message": "Trace already exists, returning existing object"
}
```

### 5.3 删除追溯关系

**DELETE** `/api/v1/traces/{traceId}`

**响应**

* 成功: `204 No Content`
* 不存在: `404 Not Found`

---

## 6. DTO映射策略

### 6.1 设计原则

API使用简化的DTO，内部存储使用完整的Pilot模型：

1. **API层（DTO）**：只包含当前需要的字段，简单易用
2. **存储层（Pilot模型）**：完整的SysML 2.0结构，所有字段都保留
3. **映射层**：Service层负责DTO和Pilot模型之间的转换

### 6.2 字段映射表

| DTO字段 | Pilot模型字段 | 说明 |
|---------|--------------|------|
| id | elementId | 元素唯一标识 |
| reqId | declaredShortName | 需求短名称 |
| name | declaredName | 需求名称 |
| text | documentation[0].body | 需求文本 |
| doc | documentation[1].body | 额外文档 |
| tags | 通过Metadata机制 | 标签（扩展） |
| fromId | client[0] | 依赖源 |
| toId | supplier[0] | 依赖目标 |
| type/kind | annotation中的Comment | 依赖类型 |

### 6.3 未来扩展

当需要Part、Port、Interface等功能时：
- 无需修改元模型（已完整注册）
- 只需添加新的DTO和映射逻辑
- 存储层自动支持所有Pilot类型

---

## 7. 视图数据接口

### 6.1 树视图数据

**GET** `/api/v1/projects/{pid}/tree`

**响应示例**

```json
{
    "root": {
        "id": "root",
        "label": "Requirements",
        "children": [
            {
                "id": "R-001",
                "label": "REQ-001: 功能需求",
                "type": "definition",
                "children": [
                    {
                        "id": "R-002",
                        "label": "登录功能实例",
                        "type": "usage",
                        "children": []
                    }
                ]
            }
        ]
    },
    "loadedFromFile": "data/projects/proj-001/model.json",
    "nodeCount": 15,
    "maxDepth": 3
}
```

### 6.2 表视图数据

**GET** `/api/v1/projects/{pid}/table`

**查询参数**

* `page`: 页码
* `size`: 页大小
* `sort`: 排序字段
* `q`: 搜索关键字

**响应示例**

```json
{
    "columns": ["reqId", "name", "type", "tags", "status"],
    "rows": [
        {
            "id": "R-001",
            "reqId": "REQ-001",
            "name": "功能需求",
            "type": "definition",
            "tags": ["critical"],
            "status": "approved"
        }
    ],
    "page": 0,
    "totalPages": 5,
    "loadedFromFile": "data/projects/proj-001/model.json"
}
```

### 6.3 图视图数据

**GET** `/api/v1/projects/{pid}/graph`

**查询参数**

* `rootId`: 根节点ID（可选，空则返回全局子图）

**响应示例**

```json
{
    "nodes": [
        {
            "id": "R-001",
            "type": "requirement",
            "label": "REQ-001",
            "position": { "x": 100, "y": 100 },
            "data": {
                "eClass": "RequirementDefinition",
                "name": "功能需求"
            }
        },
        {
            "id": "R-002",
            "type": "requirement",
            "label": "REQ-002",
            "position": { "x": 300, "y": 100 },
            "data": {
                "eClass": "RequirementUsage",
                "name": "登录功能实例"
            }
        }
    ],
    "edges": [
        {
            "id": "T-001",
            "source": "R-001",
            "target": "R-002",
            "type": "derive",
            "label": "derive"
        }
    ],
    "loadedFromFile": "data/projects/proj-001/model.json",
    "layout": "auto"
}
```

---

## 7. 通用元素接口（核心API）

> 🚀 **核心架构**：基于动态EMF的通用接口，一个API处理所有182个SysML类型，实现零代码扩展。

### 7.1 创建任意SysML元素

**POST** `/api/v1/elements`

**说明**: 通过动态EMF模式，一个接口可以创建任意182种SysML类型

**请求体**

```json
{
    "eClass": "PartUsage",  // SysML类型名称
    "attributes": {
        "declaredName": "Engine",
        "declaredShortName": "eng",
        "documentation": "Main engine component"
    }
}
```

**响应示例**

```json
{
    "eClass": "PartUsage",
    "elementId": "partusage-cd6aa5bb-1814-4d26-a68f-59b97b517204",
    "declaredName": "Engine",
    "declaredShortName": "eng",
    "isLibraryElement": false,
    "isDerived": false,
    "isConjugated": false,
    "isUnique": true,
    "portionKind": "timeslice",
    "isIndividual": false,
    "isPortion": false,
    "isAbstract": false,
    "isEnd": false,
    "direction": "in"
}
```

**支持的eClass类型示例**
- RequirementDefinition, RequirementUsage
- PartDefinition, PartUsage
- PortDefinition, PortUsage
- InterfaceDefinition, InterfaceUsage
- ConnectionDefinition, ConnectionUsage
- ActionDefinition, ActionUsage
- StateDefinition, StateUsage
- ConstraintDefinition, ConstraintUsage
- AllocationDefinition, AllocationUsage
- ItemDefinition, ItemUsage
- AttributeDefinition, AttributeUsage
- MetadataDefinition, MetadataUsage
- ViewDefinition, ViewUsage, ViewpointUsage
- RenderingDefinition, RenderingUsage
- VerificationCaseDefinition, VerificationCaseUsage
- AnalysisCaseDefinition, AnalysisCaseUsage
- CalculationDefinition, CalculationUsage
- CaseDefinition, CaseUsage
- ...等182个SysML v2类型

### 7.2 查询元素

**GET** `/api/v1/elements`

**查询参数**
- `type`: 元素类型（可选），如 `PartUsage`
- `page`: 页码（从0开始）
- `size`: 每页大小（默认50）

**响应示例**

```json
[
    {
        "eClass": "PartUsage",
        "elementId": "partusage-001",
        "declaredName": "Engine",
        "declaredShortName": "eng"
    },
    {
        "eClass": "PortUsage",
        "elementId": "portusage-002",
        "declaredName": "PowerPort",
        "declaredShortName": "pwr"
    }
]
```

**分页响应示例**

```json
{
    "content": [...],
    "totalElements": 150,
    "totalPages": 3,
    "size": 50,
    "number": 0
}
```

### 7.3 获取单个元素

**GET** `/api/v1/elements/{elementId}`

**响应**: 返回元素的完整属性

### 7.4 部分更新元素（PATCH）

**PATCH** `/api/v1/elements/{elementId}`

**请求体**: 只包含要更新的字段

```json
{
    "declaredName": "Updated Engine Name",
    "documentation": "Updated documentation"
}
```

**响应**: 返回更新后的完整元素

### 7.5 删除元素

**DELETE** `/api/v1/elements/{elementId}`

**响应**: `204 No Content`

### 7.6 架构优势

1. **零代码扩展**: 无需为每个SysML类型编写专门代码
2. **统一接口**: 一个接口处理所有182个类型
3. **动态属性**: 自动支持每个类型的所有属性
4. **代码量减少99%**: 1个Service vs 182个Service
5. **完全符合SysML v2标准**: 基于OMG官方Pilot元模型

---

## 8. 校验接口

### 7.1 静态校验

**POST** `/api/v1/projects/{pid}/validate/static`

**请求体**

```json
{
    "ids": []  // 空数组表示校验所有
}
```

**响应示例**

```json
{
    "violations": [
        {
            "ruleCode": "DUP_REQID",
            "targetId": "R-001",
            "message": "Duplicate reqId: REQ-001",
            "details": {
                "conflictsWith": ["R-005"],
                "filePath": "data/projects/proj-001/model.json"
            }
        },
        {
            "ruleCode": "CYCLE_DERIVE_REFINE",
            "targetId": "R-002",
            "message": "Circular dependency detected",
            "details": {
                "cycle": ["R-002", "R-003", "R-004", "R-002"],
                "filePath": "data/projects/proj-001/model.json"
            }
        },
        {
            "ruleCode": "BROKEN_REF",
            "targetId": "T-001",
            "message": "Reference to non-existent element",
            "details": {
                "missingId": "R-999",
                "filePath": "data/projects/proj-001/model.json"
            }
        }
    ],
    "summary": {
        "total": 3,
        "byRule": {
            "DUP_REQID": 1,
            "CYCLE_DERIVE_REFINE": 1,
            "BROKEN_REF": 1
        }
    },
    "checkedFile": "data/projects/proj-001/model.json",
    "validationTime": "2025-01-15T10:30:00.000Z"
}
```

---

## 8. 文件系统接口（新增）

### 8.1 项目文件信息

**GET** `/api/v1/projects/{pid}/files`

**响应示例**

```json
{
    "projectId": "proj-001",
    "files": [
        {
            "name": "model.json",
            "path": "data/projects/proj-001/model.json",
            "size": "45.2KB",
            "lastModified": "2025-01-15T10:00:00.000Z",
            "elements": 15
        },
        {
            "name": "metadata.json", 
            "path": "data/projects/proj-001/metadata.json",
            "size": "1.2KB",
            "lastModified": "2025-01-15T09:00:00.000Z"
        }
    ],
    "totalSize": "46.4KB"
}
```

### 8.2 文件备份

**POST** `/projects/{pid}/backup`

**响应示例**

```json
{
    "backupFile": "data/backups/proj-001_20250115_103000.json",
    "originalFile": "data/projects/proj-001/model.json",
    "backupSize": "45.2KB",
    "timestamp": "2025-01-15T10:30:00.000Z"
}
```

---

## 9. 错误码列表

| 错误码                | 说明               | HTTP状态 |
| --------------------- | ------------------ | -------- |
| `INVALID_PARAM`     | 参数格式错误       | 400      |
| `MISSING_REQUIRED`  | 缺少必填字段       | 400      |
| `NOT_FOUND`         | 资源不存在         | 404      |
| `DUP_REQID`         | reqId重复          | 409      |
| `REFERENCED`        | 被其他元素引用     | 409      |
| `CYCLE_DETECTED`    | 检测到循环依赖     | 409      |
| `INVALID_HIERARCHY` | 非法层级关系       | 400      |
| `SIZE_EXCEEDED`     | 超出大小限制       | 400      |
| `FILE_IO_ERROR`     | 文件读写错误       | 500      |
| `FILE_LOCKED`       | 文件被锁定         | 423      |
| `INVALID_JSON`      | JSON格式错误       | 400      |

---

## 10. 批量操作（P1）

预留给后续版本实现：

* 批量创建
* 批量更新
* 批量删除
* 批量导入

---

## 11. 示例集成代码

### 11.1 JavaScript/TypeScript

```typescript
// API客户端封装
class SysMLClient {
    private baseUrl = 'http://localhost:8080/api/v1';
  
    async createRequirement(projectId: string, data: any) {
        const response = await fetch(
            `${this.baseUrl}/projects/${projectId}/requirements`,
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            }
        );
      
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error.message);
        }
      
        return response.json();
    }
  
    async validateProject(projectId: string) {
        const response = await fetch(
            `${this.baseUrl}/projects/${projectId}/validate/static`,
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ids: [] })
            }
        );
      
        return response.json();
    }
    
    // 新增：Trace CRUD 方法 (REQ-C3-1 到 REQ-C3-4)
    async getTracesByRequirement(reqId: string, direction: 'in'|'out'|'both' = 'both') {
        const response = await fetch(
            `${this.baseUrl}/requirements/${reqId}/traces?dir=${direction}`
        );
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error.message);
        }
        
        return response.json();
    }
    
    async createTrace(fromId: string, toId: string, type: 'derive'|'satisfy'|'refine'|'trace') {
        const response = await fetch(
            `${this.baseUrl}/requirements/${fromId}/traces`,
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ toId, type })
            }
        );
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error.message);
        }
        
        return response.json(); // 返回201 Created 或 200 OK (去重)
    }
    
    async deleteTrace(traceId: string) {
        const response = await fetch(
            `${this.baseUrl}/traces/${traceId}`,
            {
                method: 'DELETE'
            }
        );
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error.message);
        }
        
        // 204 No Content - 无响应体
    }
    
    // 新增：检查文件状态
    async checkProjectFiles(projectId: string) {
        const response = await fetch(
            `${this.baseUrl}/projects/${projectId}/files`
        );
        
        return response.json();
    }
}
```

### 11.2 cURL示例

```bash
# 创建需求定义
curl -X POST http://localhost:8080/api/v1/projects/proj-001/requirements \
  -H "Content-Type: application/json" \
  -d '{
    "type": "definition",
    "reqId": "REQ-001",
    "name": "功能需求",
    "text": "系统应该..."
  }'

# 查询需求列表
curl http://localhost:8080/api/v1/projects/proj-001/requirements?type=definition&page=0&size=20

# 查询追溯关系
curl http://localhost:8080/api/v1/requirements/R-001/traces?dir=both

# 创建追溯关系
curl -X POST http://localhost:8080/api/v1/requirements/R-001/traces \
  -H "Content-Type: application/json" \
  -d '{
    "toId": "R-002",
    "type": "derive"
  }'

# 删除追溯关系
curl -X DELETE http://localhost:8080/api/v1/traces/T-001

# 执行静态校验
curl -X POST http://localhost:8080/api/v1/projects/proj-001/validate/static \
  -H "Content-Type: application/json" \
  -d '{"ids": []}'

# 检查项目文件
curl http://localhost:8080/api/v1/projects/proj-001/files

# 备份项目
curl -X POST http://localhost:8080/api/v1/projects/proj-001/backup
```

---

## 12. 性能和存储特性

### 12.1 文件存储特点

* **单项目单文件**: 每个项目对应一个model.json文件
* **EMF原生格式**: 直接使用Eclipse EMF JSON序列化格式
* **内存缓存**: 活跃项目的Resource保存在内存中
* **懒加载**: 仅在访问时加载项目文件
* **批量保存**: 同一请求的多个修改批量写入文件

### 12.2 并发处理

* **文件锁机制**: 使用Java NIO文件锁防止并发写入
* **重试机制**: 文件锁冲突时自动重试
* **错误恢复**: 写入失败时回滚到原始状态

### 12.3 限制说明

* **并发用户数**: 建议≤5个并发用户
* **项目大小**: 建议≤500个元素/项目
* **文件大小**: 建议≤10MB/项目文件
* **响应时间**: 大项目首次加载可能较慢

---