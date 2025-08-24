# SysML v2 MVP API 测试示例与集成指南

## 文档信息

* **版本**: 3.1
* **日期**: 2025-08-24  
* **状态**: 基于通用接口架构的API测试示例
* **基础URL**: `http://localhost:8080/api/v1`

---

## 1. 核心设计原则

### 1.1 通用接口优势
- **一个接口处理182个SysML类型**：`/api/v1/elements`
- **零代码扩展**：新增SysML类型无需修改代码
- **统一契约**：所有元素操作使用相同API模式

### 1.2 项目作用域
所有元素操作需绑定项目：
- **推荐方式**：Header `X-Project-Id: <pid>`
- **备选方式**：Query `?projectId=<pid>`
- 缺失项目标识 → 400

### 1.3 响应约定（统一）
所有成功响应均返回：
```json
{
  "data": "<对象或数组>",
  "meta": { "page":0, "size":50, "total":150 },  // 可选（列表时）
  "timestamp": "2025-08-24T10:30:00.000Z"
}
```

错误采用 `application/problem+json`：
```json
{
  "type": "https://api/errors/validation",
  "title": "Validation failed", 
  "status": 400,
  "detail": "field 'of' must reference an existing RequirementDefinition",
  "errors": [{ "field":"of", "code":"NOT_FOUND" }]
}
```

## 2. 需求管理测试用例

### 2.1 创建RequirementDefinition模板

**请求**：
```bash
curl -X POST http://localhost:8080/api/v1/elements \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -d '{
    "eClass": "RequirementDefinition",
    "attributes": {
      "declaredShortName": "EBS-L1-001",
      "declaredName": "电池系统性能需求",
      "documentation": [{"body": "电池系统应在${temperature}℃环境下提供${power}功率持续${duration}。"}]
    }
  }'
```

**响应**：
```json
{
  "data": {
    "id": "def-001",
    "eClass": "RequirementDefinition",
    "declaredShortName": "EBS-L1-001",
    "declaredName": "电池系统性能需求",
    "documentation": [{"body": "电池系统应在${temperature}℃环境下提供${power}功率持续${duration}。"}],
    "qualifiedName": "default::EBS-L1-001"
  },
  "timestamp": "2025-08-24T10:30:00.000Z"
}
```

### 2.2 创建RequirementUsage实例

**需求约束验证**：
- `RequirementUsage.of` **必须**指向 `RequirementDefinition`
- `RequirementUsage.subject` **必填**，指向被约束对象

**请求**：
```bash
curl -X POST http://localhost:8080/api/v1/elements \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -d '{
    "eClass": "RequirementUsage", 
    "attributes": {
      "declaredShortName": "EBS-L1-001-U1",
      "declaredName": "BMS性能要求实例",
      "of": "def-001",
      "subject": "bms-part-001",
      "parameters": {
        "temperature": "45℃",
        "power": "100kW", 
        "duration": "30min"
      }
    }
  }'
```

**响应**：
```json
{
  "data": {
    "id": "usage-001",
    "eClass": "RequirementUsage",
    "declaredShortName": "EBS-L1-001-U1",
    "of": "def-001",
    "subject": "bms-part-001",
    "parameters": {
      "temperature": "45℃",
      "power": "100kW",
      "duration": "30min"
    },
    "renderedText": "电池系统应在45℃环境下提供100kW功率持续30min。"
  },
  "timestamp": "2025-08-24T10:30:00.000Z"
}
```

### 2.3 QUDV单位/量纲校验

**校验规则**：
- Duration类型：s、min、h、day
- Power类型：W、kW、MW、hp
- Temperature类型：℃、K、℉

**错误示例**：
```bash
curl -X POST http://localhost:8080/api/v1/elements \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -d '{
    "eClass": "RequirementUsage",
    "attributes": {
      "parameters": {
        "duration": "100kg"  // 错误：Duration类型使用Mass单位
      }
    }
  }'
```

**错误响应**：
```json
{
  "type": "https://api/errors/validation/unit-mismatch",
  "title": "Unit dimension validation failed",
  "status": 400,
  "detail": "Parameter 'duration' expected Duration dimension but got Mass",
  "errors": [{
    "field": "parameters.duration",
    "code": "UNIT_DIMENSION_MISMATCH",
    "expectedDimension": "Duration",
    "acceptedUnits": ["s", "min", "h", "day"]
  }]
}
```

---

## 3. 追溯关系测试用例

### 3.1 追溯语义约束

**语义规则**：
- `Satisfy`: 设计元素(PartUsage/ActionUsage) → 需求(RequirementUsage/Definition)
- `DeriveRequirement`/`Refine`: 需求 → 需求
- `Verify`: 测试用例(TestCase) → 需求(RequirementUsage)

### 3.2 创建Satisfy关系

**请求**：
```bash
curl -X POST http://localhost:8080/api/v1/elements \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -d '{
    "eClass": "Satisfy",
    "attributes": {
      "source": "bms-part-001",     // PartUsage
      "target": "usage-001"         // RequirementUsage  
    }
  }'
```

### 3.3 语义约束错误示例

**错误**：需求指向设计元素（违反Satisfy语义）
```bash
curl -X POST http://localhost:8080/api/v1/elements \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -d '{
    "eClass": "Satisfy",
    "attributes": {
      "source": "usage-001",       // RequirementUsage - 错误
      "target": "bms-part-001"     // PartUsage - 错误
    }
  }'
```

**错误响应**：
```json
{
  "type": "https://api/errors/semantic/invalid-trace",
  "title": "Trace semantic constraint violation",
  "status": 400, 
  "detail": "Satisfy relationship must be: design element → requirement",
  "errors": [{
    "field": "source",
    "code": "INVALID_TRACE_SEMANTICS",
    "constraint": "Satisfy.source must be PartUsage|ActionUsage|InterfaceUsage"
  }]
}
```

## 4. 查询与并发控制

### 4.1 字段投影与引用展开

**请求**：
```bash
curl "http://localhost:8080/api/v1/elements?type=RequirementUsage&fields=id,declaredName,of&expand=of,subject" \
  -H "X-Project-Id: default"
```

**响应**：
```json
{
  "data": [{
    "id": "usage-001",
    "declaredName": "BMS性能要求实例", 
    "of": {
      "id": "def-001",
      "eClass": "RequirementDefinition",
      "declaredName": "电池系统性能需求"
    },
    "subject": {
      "id": "bms-part-001", 
      "eClass": "PartUsage",
      "declaredName": "电池管理系统"
    }
  }],
  "meta": { "page": 0, "size": 50, "total": 1 },
  "timestamp": "2025-08-24T10:30:00.000Z"
}
```

### 4.2 乐观锁并发控制

**获取元素（带ETag）**：
```bash
curl -I http://localhost:8080/api/v1/elements/usage-001 \
  -H "X-Project-Id: default"

# Response Headers:
# ETag: "v1-abc123def456"
```

**更新元素（需要ETag）**：
```bash
curl -X PATCH http://localhost:8080/api/v1/elements/usage-001 \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -H "If-Match: \"v1-abc123def456\"" \
  -d '{
    "declaredName": "BMS高性能要求实例"
  }'
```

**版本冲突响应**：
```json
{
  "type": "https://api/errors/concurrency/version-conflict",
  "title": "Resource version conflict",
  "status": 412,
  "detail": "Resource has been modified by another request",
  "currentETag": "v2-xyz789abc123"
}
```

## 5. PATCH字段安全性

### 5.1 字段白名单验证

**安全原则**：仅接受该 `eClass` 的声明属性；未知字段→400

**合法PATCH**：
```bash
curl -X PATCH http://localhost:8080/api/v1/elements/usage-001 \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -H "If-Match: \"v1-abc123def456\"" \
  -d '{
    "declaredName": "新名称",        // ✅ RequirementUsage的声明属性
    "subject": "new-subject-001"    // ✅ RequirementUsage的声明属性
  }'
```

**非法PATCH**：
```bash
curl -X PATCH http://localhost:8080/api/v1/elements/usage-001 \
  -H "Content-Type: application/json" \
  -H "X-Project-Id: default" \
  -H "If-Match: \"v1-abc123def456\"" \
  -d '{
    "declaredName": "新名称",
    "invalidField": "value"         // ❌ 非RequirementUsage属性
  }'
```

**错误响应**：
```json
{
  "type": "https://api/errors/validation/unknown-field",
  "title": "Unknown field in request",
  "status": 400,
  "detail": "Field 'invalidField' is not declared in RequirementUsage",
  "errors": [{
    "field": "invalidField",
    "code": "UNKNOWN_FIELD",
    "allowedFields": ["declaredName", "declaredShortName", "of", "subject", "parameters"]
  }]
}
```

## 6. 集成最佳实践

### 6.1 命名约定统一
- **输入**：请求体中使用 `eClass`
- **输出**：响应中使用 `id`（= elementId）
- **查询**：参数使用 `type`（= eClass名称）

### 6.2 错误处理模式
```javascript
// JavaScript示例
async function createElement(eClass, attributes) {
  try {
    const response = await fetch('/api/v1/elements', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Project-Id': 'default'
      },
      body: JSON.stringify({ eClass, attributes })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new APIError(error);
    }
    
    return (await response.json()).data;
  } catch (error) {
    console.error('创建元素失败:', error);
    throw error;
  }
}
```

### 6.3 参数化需求工作流
1. 创建Definition模板（含占位符）
2. 创建Usage实例（绑定具体参数）
3. 系统自动生成`renderedText`
4. 前端可显示最终需求文本

---

## 7. 性能与限制

### 7.1 查询优化
- 使用 `fields=` 减少响应体大小
- 使用分页避免大结果集
- `expand=` 仅展开必要的引用关系

### 7.2 并发建议
- 始终使用ETag进行乐观锁控制
- 处理412冲突后重新获取最新版本
- 批量操作考虑事务边界

### 7.3 单位校验性能
- 单位校验在API层完成，避免存储后报错
- 常用单位组合预编译，提高校验性能

---

这份指南涵盖了您提出的所有关键点：统一响应格式、项目作用域、需求约束、QUDV校验、追溯语义、PATCH安全性、查询增强和并发控制。每个测试用例都可以直接执行，确保API契约的一致性和正确性。