# SysML v2 建模平台 MVP API文档

## 文档信息

* **版本** : 1.3
* **日期** : 2025-08-23
* **状态** : MVP接口定稿（含Trace CRUD + 健康检查实现）
* **基础URL** : `http://localhost:8080/api/v1`
* **存储方式** : JSON文件系统

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

**响应示例**

```json
{
    "status": "UP",
    "packages": [
        {
            "nsUri": "urn:your:sysml2",
            "name": "sysml",
            "source": "local",
            "classCount": 3
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

**GET** `/projects/{pid}/export`

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
            "eClass": "RequirementDefinition",
            "$id": "R-001",
            "reqId": "REQ-001",
            "name": "功能需求",
            "text": "系统应该...",
            "tags": ["critical"],
            "createdAt": "2025-01-15T10:00:00.000Z",
            "updatedAt": "2025-01-15T10:00:00.000Z",
            "_version": "1.0"
        }
    ]
}
```

### 3.2 导入项目

**POST** `/projects/{pid}/import`

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

## 4. 需求管理接口

### 4.1 查询需求列表

**GET** `/projects/{pid}/requirements`

**查询参数**

* `type`: `definition` | `usage`
* `page`: 页码（0开始）
* `size`: 页大小（1-200）
* `q`: 搜索关键字

**响应示例**

```json
{
    "content": [
        {
            "id": "R-001",
            "eClass": "RequirementDefinition",
            "reqId": "REQ-001",
            "name": "功能需求",
            "text": "系统应该...",
            "tags": ["critical", "safety"],
            "createdAt": "2025-01-15T10:00:00.000Z",
            "updatedAt": "2025-01-15T10:00:00.000Z",
            "_version": "1.0"
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

**POST** `/projects/{pid}/requirements`

**请求体（Definition）**

```json
{
    "type": "definition",
    "reqId": "REQ-001",
    "name": "功能需求",
    "text": "系统应该提供用户登录功能",
    "tags": ["security"],
    "doc": "详细说明..."
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
* 响应头: `Location: /projects/{pid}/requirements/{id}`

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

**GET** `/projects/{pid}/requirements/{id}`

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

**PUT** `/projects/{pid}/requirements/{id}`

**请求体**

```json
{
    "name": "更新后的名称",
    "text": "更新后的文本",
    "tags": ["updated", "critical"]
}
```

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

**DELETE** `/projects/{pid}/requirements/{id}`

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

## 5. 追溯管理接口

### 5.1 查询追溯关系

**GET** `/requirements/{id}/traces`

**查询参数**

* `dir`: `in` | `out` | `both` (默认both)

**响应示例**

```json
{
    "traces": [
        {
            "id": "T-001",
            "fromId": "R-001",
            "toId": "R-002",
            "type": "derive",
            "createdAt": "2025-01-15T10:00:00.000Z"
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

**POST** `/requirements/{id}/traces`

**请求体**

```json
{
    "toId": "R-003",
    "type": "derive"
}
```

**type可选值**

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

**DELETE** `/traces/{traceId}`

**响应**

* 成功: `204 No Content`
* 不存在: `404 Not Found`

---

## 6. 视图数据接口

### 6.1 树视图数据

**GET** `/projects/{pid}/tree`

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

**GET** `/projects/{pid}/table`

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

**GET** `/projects/{pid}/graph`

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

## 7. 校验接口

### 7.1 静态校验

**POST** `/projects/{pid}/validate/static`

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

**GET** `/projects/{pid}/files`

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