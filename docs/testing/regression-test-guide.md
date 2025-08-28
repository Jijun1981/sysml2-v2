# 回归测试指南

## 🎯 目标
确保新功能开发不会破坏现有功能，保持系统稳定性。

## 📋 日常开发流程

### 1. **开发前**（5分钟）
```bash
# 确保当前代码状态良好
cd backend
./quick-test.sh
```

### 2. **开发中**（边开发边测试）
```bash
# 只运行相关的测试
mvn test -Dtest="RequirementControllerTest"  # 测试需求相关
mvn test -Dtest="TraceControllerTest"        # 测试追溯相关
```

### 3. **开发完成后**（10分钟）
```bash
# 运行完整回归测试
mvn test
```

## 🚨 关键检查点

### A. **核心功能不能破坏**
- ✅ 需求CRUD操作 (`RequirementControllerTest`)
- ✅ 追溯关系管理 (`TraceControllerTest`) 
- ✅ 数据验证规则 (`ValidationControllerTest`)
- ✅ 项目导入导出 (`ProjectControllerTest`)

### B. **API接口不能变更**
- ✅ `/api/v1/requirements` 路径保持不变
- ✅ 请求/响应格式保持兼容
- ✅ 错误码和消息格式一致

### C. **数据格式不能破坏**
- ✅ JSON序列化格式兼容
- ✅ 文件存储结构兼容
- ✅ EMF模型结构兼容

## ⚡ 快速检查命令

```bash
# 1分钟快检（只跑Controller层）
mvn test -Dtest="*ControllerTest" --batch-mode --quiet

# 5分钟全检（所有测试）
mvn test --batch-mode

# 检查编译（30秒）
mvn clean compile
```

## 🔄 Git工作流集成

### 提交前检查
```bash
# 自动化git hook示例
cd .git/hooks
cat > pre-commit << 'EOF'
#!/bin/bash
echo "🧪 运行回归测试..."
cd backend
mvn test -Dtest="*ControllerTest" --batch-mode --quiet
if [ $? -ne 0 ]; then
    echo "❌ 测试失败，提交被阻止！"
    exit 1
fi
echo "✅ 测试通过，继续提交"
EOF
chmod +x pre-commit
```

### 分支合并前检查
```bash
# 合并到develop前的完整检查
git checkout develop
git pull origin develop
git checkout feature/your-feature
git rebase develop
mvn clean test  # 确保在最新develop基础上测试通过
```

## 📊 测试分级策略

### 🔥 Level 1: 冒烟测试（1分钟）
```bash
# 最关键的功能快速检查
mvn test -Dtest="RequirementControllerTest#testCreateRequirement,TraceControllerTest#testCreateTrace"
```

### 🟡 Level 2: 回归测试（5分钟）
```bash
# 所有Controller层测试
mvn test -Dtest="*ControllerTest"
```

### 🟢 Level 3: 完整测试（10分钟）
```bash
# 所有测试，包括集成测试
mvn clean test
```

## 🚀 自动化建议

### IDE集成
- **IntelliJ IDEA**: 配置Run Configuration运行特定测试
- **VS Code**: 使用Java Extension Pack的测试功能

### GitHub Actions（未来）
```yaml
# .github/workflows/regression-test.yml
name: Regression Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: mvn test
```

## 🎭 测试失败时的处理

### 1. **确定影响范围**
```bash
# 查看哪些测试失败
mvn test | grep "FAILED"
```

### 2. **隔离问题**
```bash
# 只运行失败的测试类
mvn test -Dtest="RequirementControllerTest"
```

### 3. **对比差异**
```bash
# 与上一个工作版本对比
git diff HEAD~1 -- backend/src/main/java/
```

## 📋 检查清单

开发完成后，请确认：
- [ ] 所有原有测试通过 (`mvn test`)
- [ ] 新功能有对应测试用例
- [ ] API文档更新（如果有接口变更）
- [ ] 数据兼容性检查（如果有模型变更）
- [ ] 错误处理正常工作

## ⚠️ 危险操作警告

这些操作可能破坏现有功能，需要特别小心：
- 修改已存在的Controller方法签名
- 变更DTO字段名称或类型
- 修改EMF模型结构
- 更改API路径或HTTP方法
- 修改异常处理逻辑

---

**记住**: 每次代码变更后运行回归测试是最佳实践，短期的几分钟投入能避免长期的bug修复成本！