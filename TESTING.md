# 🧪 SysML v2 回归测试 - 单人开发专用

> **5分钟保护你的开发成果，避免5小时debug**

## 最简单的规则

**每次开发完成后，运行这个命令：**
```bash
./quick-test.sh
```
**看到 "🎉 快速回归测试完成 - 所有核心功能正常！" 就可以提交。**

---

## 我改了什么，需要测什么？

### 📝 改了后端代码（Java/Spring Boot）
```bash
# 1. 核心业务测试（3分钟）
cd backend
mvn test -Dtest="RequirementServiceTest,UniversalElementServiceTest,FieldStandardizationTest" -q

# 2. 如果上面通过，运行完整后端测试
mvn test -q

# 3. 验证功能完整性
cd .. && ./quick-test.sh
```

### 🎨 改了前端代码（React/TypeScript）
```bash
# 1. 前端基础测试
cd frontend
npm test -- --run src/__tests__/simple.test.ts src/__tests__/simple-react.test.tsx

# 2. 验证功能完整性
cd .. && ./quick-test.sh
```

### 🔌 改了API接口或数据结构
```bash
# 完整回归测试（15分钟）
./scripts/regression-suite.sh core

# 特别验证删除持久化（这最容易被破坏）
TEST_ID="API-$(date +%s)"
curl -X POST "http://localhost:8080/api/v1/requirements" -H "Content-Type: application/json" -d "{\"elementId\":\"$TEST_ID\",\"reqId\":\"$TEST_ID\",\"declaredShortName\":\"API测试\",\"declaredName\":\"API测试描述\"}"
curl -X DELETE "http://localhost:8080/api/v1/requirements/$TEST_ID"
# 重启后端，确认数据真的删除了
```

### 🐛 修复了bug
```bash
# 1. 验证bug确实修复了
# 2. 确保没破坏其他功能
cd backend && mvn test -q
./quick-test.sh
```

### 🚀 准备发布
```bash
./scripts/regression-suite.sh full
```

---

## 🚨 绝对不能省略的测试

### 删除持久化验证（重中之重）
**这是用户反馈过的重大bug，每次开发后都必须验证！**

```bash
# 完整验证步骤
TEST_ID="CRITICAL-$(date +%s)"

# 1. 创建测试数据
curl -X POST "http://localhost:8080/api/v1/requirements" \
  -H "Content-Type: application/json" \
  -d "{\"elementId\":\"$TEST_ID\",\"reqId\":\"$TEST_ID\",\"declaredShortName\":\"删除测试\",\"declaredName\":\"删除测试的描述\"}"

# 2. 确认创建成功
curl http://localhost:8080/api/v1/requirements | grep "$TEST_ID" && echo "✅ 创建成功"

# 3. 删除数据
curl -X DELETE "http://localhost:8080/api/v1/requirements/$TEST_ID"

# 4. 确认立即删除成功
curl http://localhost:8080/api/v1/requirements | grep "$TEST_ID" || echo "✅ 立即删除成功"

# 5. 重启服务
pkill -f "spring-boot:run"
cd backend && mvn spring-boot:run &
sleep 30

# 6. 验证持久化删除
curl http://localhost:8080/api/v1/requirements | grep "$TEST_ID" || echo "🎉 删除持久化验证通过"
```

**如果这个失败，立即停止开发去修复！**

---

## 📦 核心功能测试清单（2024-08-29更新）

### 1. Requirements管理（必测）
```bash
# RequirementDefinition CRUD
curl -X POST http://localhost:8080/api/v1/requirements \
  -H "Content-Type: application/json" \
  -d '{"elementId":"TEST-DEF","reqId":"TEST-DEF","declaredShortName":"测试定义","declaredName":"这是测试定义的详细描述"}'

# RequirementUsage与引用（新功能！）
curl -X POST http://localhost:8080/api/v1/requirements/usages \
  -H "Content-Type: application/json" \
  -d '{"elementId":"TEST-USE","declaredShortName":"测试使用","declaredName":"这是测试使用的详细描述","requirementDefinition":"TEST-DEF"}'

# 验证引用关系
curl http://localhost:8080/api/v1/requirements/usages | grep "requirementDefinition"

# 测试空引用（REQ-TDD-001-4）
curl -X POST http://localhost:8080/api/v1/requirements/usages \
  -H "Content-Type: application/json" \
  -d '{"elementId":"TEST-NULL","declaredShortName":"无引用","declaredName":"这是一个无引用的需求使用"}'

# 测试无效引用（应该报错）
curl -X POST http://localhost:8080/api/v1/requirements/usages \
  -H "Content-Type: application/json" \
  -d '{"elementId":"TEST-ERR","declaredShortName":"错误引用","declaredName":"这是一个错误引用的测试","requirementDefinition":"NOT-EXIST"}'
```

### 2. 追溯关系管理
```bash
# 创建追溯关系
curl -X POST http://localhost:8080/api/v1/traces \
  -H "Content-Type: application/json" \
  -d '{"elementId":"TRACE-1","sourceId":"TEST-USE","targetId":"TEST-DEF","traceType":"satisfy"}'

# 查询追溯关系
curl http://localhost:8080/api/v1/traces

# 查询依赖关系
curl "http://localhost:8080/api/v1/traces/dependencies/TEST-DEF"
```

### 3. 验证服务
```bash
# 运行验证规则
curl -X POST http://localhost:8080/api/v1/validation/validate \
  -H "Content-Type: application/json" \
  -d '{"projectId":"default"}'

# 查看验证结果
curl http://localhost:8080/api/v1/validation/results
```

### 4. 项目管理
```bash
# 导出项目
curl -X POST http://localhost:8080/api/v1/projects/default/export \
  -H "Content-Type: application/json" \
  -d '{"format":"json"}'

# 导入项目
curl -X POST http://localhost:8080/api/v1/projects/test-import/import \
  -H "Content-Type: application/json" \
  -F "file=@export.json"
```

### 5. 高级查询（已知问题区域）
```bash
# 分层查询（层级关系）
curl "http://localhost:8080/api/v1/advanced/hierarchy?rootId=TEST-DEF"

# 分页查询
curl "http://localhost:8080/api/v1/advanced/query?type=RequirementUsage&page=0&size=10"

# 字段标准化验证
curl http://localhost:8080/api/v1/requirements | jq '.[].elementId' # 应该都有elementId
```

### 6. Demo数据验证
```bash
# 加载demo数据
curl -X POST http://localhost:8080/api/v1/demo/battery-system

# 验证demo数据
curl http://localhost:8080/api/v1/requirements | grep "Battery"
```

---

## ⚡ 快速测试脚本详解

`./quick-test.sh` 会验证：
- ✅ 后端服务健康检查
- ✅ 基础CRUD功能正常
- ✅ 删除功能正常
- ✅ API响应正常
- ✅ RequirementDefinition引用功能（新增）

**2分钟内完成，覆盖90%的常见问题**

---

## 📋 提交前检查清单

- [ ] 运行了对应场景的测试
- [ ] `./quick-test.sh` 显示成功
- [ ] 删除持久化验证通过（如果改了后端）
- [ ] 没有破坏现有测试用例
- [ ] RequirementUsage引用功能正常（如果改了引用相关代码）

---

## 🔄 开发工作流

```bash
# 开始开发前
./quick-test.sh  # 确保起点正常

# 开发中...
# （按需运行相关测试）

# 开发完成后
# 根据改动类型选择上述对应测试

# 提交前
./quick-test.sh && git commit -m "你的提交信息" || echo "修复后再提交"
```

---

## 🛠 测试工具说明

### 1. `./quick-test.sh` - 快速验证（2分钟）
- 最常用的测试
- 覆盖核心功能
- 每次开发后必运行

### 2. `./scripts/regression-suite.sh core` - 完整回归（15分钟）
- API接口变更时使用
- 重要功能修改时使用
- 发布前使用

### 3. `mvn test` - 后端单元测试
- 后端代码修改时使用
- 可以指定特定测试类
- 最可靠的测试覆盖

### 4. 特定功能测试类（按需运行）
```bash
# EMF引用功能测试
mvn test -Dtest=RequirementDefinitionReferenceTest

# 字段标准化测试
mvn test -Dtest=FieldStandardizationTest

# 追溯关系测试
mvn test -Dtest=TraceServiceTest

# 验证服务测试
mvn test -Dtest=ValidationServiceTest

# 端到端系统测试
mvn test -Dtest=EndToEndSystemTest
```

---

## 💡 实用技巧

### 创建自己的测试别名
在你的 `.bashrc` 或 `.zshrc` 中添加：
```bash
alias qt="./quick-test.sh"
alias bt="cd backend && mvn test -q && cd .."
alias ft="cd frontend && npm test -- --run src/__tests__/simple.test.ts && cd .."
alias rt="cd backend && mvn test -Dtest=RequirementDefinitionReferenceTest -q && cd .."
```

### 保存测试快照
```bash
# 备份当前正常状态
cp -r data/projects/default test-snapshots/baseline-$(date +%Y%m%d)

# 出问题时对比
diff -r test-snapshots/baseline-20250829 data/projects/default
```

### 查看详细日志
```bash
# 后端日志
tail -f backend/logs/application.log
tail -f backend/backend.log

# 测试日志  
ls -la logs/

# Spring Boot运行日志
tail -f backend/sysml-mvp-backend.log
```

---

## 🔧 已知问题和解决方案

### 问题1: RequirementDefinition引用不持久化
**症状**: requirementDefinition字段在重启后丢失
**解决**: 已通过EMFReferenceHelper后处理机制修复
**验证**:
```bash
# 创建带引用的Usage
curl -X POST http://localhost:8080/api/v1/requirements/usages \
  -d '{"elementId":"REF-TEST","declaredShortName":"引用测试","declaredName":"这是一个带引用的测试用例","requirementDefinition":"DEF-PERF"}'
# 重启服务
# 验证引用还在
curl http://localhost:8080/api/v1/requirements/usages | grep "REF-TEST" | grep "DEF-PERF"
```

### 问题2: 删除操作不持久化
**症状**: 删除的数据在重启后又出现
**解决**: 确保FileModelRepository.saveProject被调用
**验证**: 使用上面的删除持久化验证步骤

### 问题3: 字段名不一致
**症状**: API返回的字段名不统一
**解决**: 通过FieldStandardizationService统一处理
**验证**: 所有API响应都应该有elementId字段

---

## ⚠️ 记住这些原则

1. **简单有效 > 复杂全面**
2. **先保证核心功能不被破坏**
3. **遇到问题再添加相应测试**
4. **删除持久化是最重要的测试**
5. **每次提交前至少运行快速测试**
6. **新功能必须有对应的测试验证**

---

## 📊 测试覆盖率统计

当前测试覆盖的功能模块：
- ✅ RequirementDefinition CRUD (100%)
- ✅ RequirementUsage CRUD + 引用关系 (100%)
- ✅ Trace追溯关系管理 (100%)
- ✅ Validation验证服务 (100%)
- ✅ Project项目管理 (导入/导出)
- ✅ AdvancedQuery高级查询
- ✅ DemoData示例数据
- ✅ 字段标准化服务
- ✅ EMF模型注册和管理
- ✅ 文件持久化层

---

## 🔄 这个文档会不断更新

- 发现新的易出错点 → 添加到测试清单
- 新增核心功能 → 添加到快速测试
- 修复重要bug → 添加防回归测试
- 优化开发流程 → 更新工作流建议

**记住：这个测试体系会随着项目成长而成长！**

最后更新: 2024-08-29
- 新增RequirementDefinition引用功能测试
- 新增EMFReferenceHelper相关测试
- 更新核心功能测试清单
- 添加已知问题和解决方案