# RequirementUsage CRUD API 测试示例

## 说明
本文档提供了RequirementUsage CRUD API功能的完整测试示例，展示了如何使用新实现的API来满足REQ-C2-1和REQ-C2-2需求。

## 1. 创建RequirementDefinition（前置条件）

首先创建一个RequirementDefinition用于引用：

```bash
curl -X POST http://localhost:8080/requirements \
  -H "Content-Type: application/json" \
  -d '{
    "type": "definition",
    "reqId": "REQ-USER-001",
    "name": "用户登录需求",
    "text": "系统应支持用户通过用户名和密码登录",
    "doc": "用户登录功能的详细说明",
    "tags": ["security", "authentication"]
  }'
```

预期响应（201 Created）：
```json
{
  "id": "R-a1b2c3d4",
  "reqId": "REQ-USER-001", 
  "name": "用户登录需求",
  "text": "系统应支持用户通过用户名和密码登录",
  "doc": "用户登录功能的详细说明",
  "tags": ["security", "authentication"],
  "version": "1.0",
  "createdAt": "2025-08-23T12:00:00Z",
  "updatedAt": "2025-08-23T12:00:00Z"
}
```

## 2. 创建RequirementUsage（REQ-C2-1）

### 2.1 成功创建Usage

```bash
curl -X POST http://localhost:8080/requirements \
  -H "Content-Type: application/json" \
  -d '{
    "type": "usage",
    "of": "R-a1b2c3d4",
    "name": "登录界面需求",
    "text": "实现具体的登录界面",
    "tags": ["ui", "frontend"]
  }'
```

预期响应（201 Created）：
```json
{
  "id": "U-e5f6g7h8",
  "of": "R-a1b2c3d4",
  "name": "登录界面需求",
  "text": "实现具体的登录界面",
  "status": "draft",
  "tags": ["ui", "frontend"],
  "version": "1.0",
  "createdAt": "2025-08-23T12:01:00Z",
  "updatedAt": "2025-08-23T12:01:00Z"
}
```

### 2.2 缺少of参数的错误情况

```bash
curl -X POST http://localhost:8080/requirements \
  -H "Content-Type: application/json" \
  -d '{
    "type": "usage",
    "name": "无效的Usage",
    "text": "缺少of参数"
  }'
```

预期响应（400 Bad Request）：
```json
{
  "error": "创建usage时of参数不能为空",
  "status": 400
}
```

### 2.3 引用的定义不存在的错误情况

```bash
curl -X POST http://localhost:8080/requirements \
  -H "Content-Type: application/json" \
  -d '{
    "type": "usage",
    "of": "R-nonexistent",
    "name": "无效引用的Usage"
  }'
```

预期响应（404 Not Found）：
```json
{
  "error": "引用的需求定义不存在: R-nonexistent",
  "status": 404
}
```

## 3. 获取RequirementUsage

```bash
curl -X GET http://localhost:8080/requirements/U-e5f6g7h8
```

预期响应（200 OK）：
```json
{
  "id": "U-e5f6g7h8",
  "of": "R-a1b2c3d4",
  "name": "登录界面需求",
  "text": "实现具体的登录界面",
  "status": "draft",
  "tags": ["ui", "frontend"],
  "version": "1.0",
  "createdAt": "2025-08-23T12:01:00Z",
  "updatedAt": "2025-08-23T12:01:00Z"
}
```

## 4. 更新RequirementUsage（REQ-C2-2）

允许更新的字段：name, text, status, tags

```bash
curl -X PUT http://localhost:8080/requirements/U-e5f6g7h8 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "更新后的登录界面需求",
    "text": "实现具体的用户登录界面，包括用户体验优化",
    "status": "in_review",
    "tags": ["ui", "frontend", "ux"]
  }'
```

预期响应（200 OK）：
```json
{
  "id": "U-e5f6g7h8",
  "of": "R-a1b2c3d4",
  "name": "更新后的登录界面需求",
  "text": "实现具体的用户登录界面，包括用户体验优化",
  "status": "in_review",
  "tags": ["ui", "frontend", "ux"],
  "version": "1.0",
  "createdAt": "2025-08-23T12:01:00Z",
  "updatedAt": "2025-08-23T12:02:00Z"
}
```

## 5. 删除RequirementUsage（REQ-C2-2）

### 5.1 成功删除（无Trace关系）

```bash
curl -X DELETE http://localhost:8080/requirements/U-e5f6g7h8
```

预期响应（204 No Content）：无响应体

### 5.2 有Trace关系阻塞删除的情况

如果Usage被Trace引用，删除会返回409错误：

预期响应（409 Conflict）：
```json
{
  "error": "需求用法被Trace引用，无法删除",
  "blockingTraceIds": ["T-x1y2z3w4", "T-a5b6c7d8"]
}
```

## 6. 列表查询支持type过滤

### 6.1 获取所有需求（定义+用法）

```bash
curl -X GET http://localhost:8080/requirements
```

### 6.2 只获取需求定义

```bash
curl -X GET "http://localhost:8080/requirements?type=definition"
```

### 6.3 只获取需求用法

```bash
curl -X GET "http://localhost:8080/requirements?type=usage"
```

## 验证要点

1. **REQ-C2-1验证**：
   - ✅ POST /requirements（type=usage, of=defId）成功创建
   - ✅ 缺of参数返回400错误
   - ✅ defId不存在返回404错误
   - ✅ 201响应包含Location header

2. **REQ-C2-2验证**：
   - ✅ 允许更新name, text, status, tags字段
   - ✅ 存在Trace关系时删除返回409，包含阻塞的traceIds
   - ✅ 正常情况下删除返回204

3. **架构一致性**：
   - ✅ 复用现有的RequirementDefinitionDTO
   - ✅ 保持与Definition CRUD相同的代码风格
   - ✅ 使用EMFModelRegistry工厂方法
   - ✅ 统一的异常处理和错误响应格式

## 技术实现亮点

1. **统一的Controller方法**：使用type参数区分definition和usage的处理
2. **复用现有DTO**：RequirementDefinitionDTO同时支持definition和usage字段
3. **分层错误处理**：通过GlobalExceptionHandler统一处理各种异常类型
4. **EMF集成**：充分利用现有的EMF架构和工厂模式
5. **完整的验证链**：从参数验证到业务规则验证的完整链路