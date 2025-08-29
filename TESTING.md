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
curl -X POST "http://localhost:8080/api/v1/requirements" -H "Content-Type: application/json" -d "{\"elementId\":\"$TEST_ID\",\"reqId\":\"$TEST_ID\",\"name\":\"API测试\"}"
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
  -d "{\"elementId\":\"$TEST_ID\",\"reqId\":\"$TEST_ID\",\"name\":\"删除测试\"}"

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

## ⚡ 快速测试脚本详解

`./quick-test.sh` 会验证：
- ✅ 后端服务健康检查
- ✅ 基础CRUD功能正常
- ✅ 删除功能正常
- ✅ API响应正常

**2分钟内完成，覆盖90%的常见问题**

---

## 📋 提交前检查清单

- [ ] 运行了对应场景的测试
- [ ] `./quick-test.sh` 显示成功
- [ ] 删除持久化验证通过（如果改了后端）
- [ ] 没有破坏现有测试用例

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

---

## 💡 实用技巧

### 创建自己的测试别名
在你的 `.bashrc` 或 `.zshrc` 中添加：
```bash
alias qt="./quick-test.sh"
alias bt="cd backend && mvn test -q && cd .."
alias ft="cd frontend && npm test -- --run src/__tests__/simple.test.ts && cd .."
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

# 测试日志  
ls -la logs/
```

---

## ⚠️ 记住这些原则

1. **简单有效 > 复杂全面**
2. **先保证核心功能不被破坏**
3. **遇到问题再添加相应测试**
4. **删除持久化是最重要的测试**
5. **每次提交前至少运行快速测试**

---

## 🔄 这个文档会不断更新

- 发现新的易出错点 → 添加到测试清单
- 新增核心功能 → 添加到快速测试
- 修复重要bug → 添加防回归测试
- 优化开发流程 → 更新工作流建议

**记住：这个测试体系会随着项目成长而成长！**