# SysML v2 建模平台 MVP 回归测试套件

## 文档信息

- **版本**: 1.0
- **日期**: 2025-08-28
- **状态**: MVP完成，字段标准化验证通过
- **目的**: 定义MVP阶段的完整回归测试套件，确保Phase 1开发的基线稳定性

---

## 1. 测试套件概述

### 1.1 测试统计

| 指标 | 数值 | 说明 |
|------|------|------|
| **测试文件总数** | 21个 | 包含所有层次的测试 |
| **测试用例总数** | 217个 | @Test注解方法 |
| **核心测试用例** | 99个 | Controller+Service层 |
| **通过率** | 100% | 所有测试全部通过 |
| **执行时间** | ~45秒 | 完整套件运行时间 |

### 1.2 测试覆盖范围

- ✅ **API层**: 所有REST端点的请求/响应验证
- ✅ **业务层**: 业务逻辑和验证规则
- ✅ **数据层**: EMF模型操作和序列化
- ✅ **集成测试**: 端到端功能验证
- ✅ **字段标准化**: SysML 2.0字段兼容性

---

## 2. 核心回归测试清单

### 2.1 需求管理测试组

#### RequirementControllerTest (11个测试)

```bash
mvn test -Dtest=RequirementControllerTest
```

**测试内容**：
- ✅ REQ-C1-1: 创建需求定义（reqId唯一性验证）
- ✅ REQ-C1-1: reqId重复时返回409
- ✅ REQ-C1-2: 查询所有需求定义
- ✅ REQ-C1-2: 根据ID查询需求
- ✅ REQ-C1-2: 查询不存在的需求返回404
- ✅ REQ-C1-3: 更新需求定义（PATCH语义）
- ✅ REQ-C2-1: 创建需求使用
- ✅ REQ-C2-2: 查询所有需求使用
- ✅ REQ-C1-4: 参数化文本渲染
- ✅ REQ-C2-4: 删除被引用的需求返回409
- ✅ REQ-C2-4: 删除未被引用的需求成功

#### RequirementServiceTest (13个测试)

```bash
mvn test -Dtest=RequirementServiceTest
```

**测试内容**：
- ✅ reqId唯一性业务验证
- ✅ reqId重复异常处理
- ✅ reqId必填验证
- ✅ 需求定义PATCH更新
- ✅ requirementDefinition字段验证（标准化后）
- ✅ requirementDefinition必填验证
- ✅ 删除前引用检查
- ✅ 参数化文本渲染逻辑
- ✅ 委托给UniversalElementService

### 2.2 追溯关系测试组

#### TraceControllerTest (9个测试)

```bash
mvn test -Dtest=TraceControllerTest
```

**测试内容**：
- ✅ REQ-C3-1: 创建追溯关系（4种类型）
- ✅ REQ-C3-2: 查询所有追溯关系
- ✅ REQ-C3-2: 按类型过滤追溯关系
- ✅ REQ-C3-3: 依赖关系自动去重
- ✅ REQ-C3-4: 追溯语义约束验证
- ✅ 删除追溯关系
- ✅ 自引用检测（400错误）
- ✅ 引用不存在元素（404错误）

#### TraceServiceTest (12个测试)

```bash
mvn test -Dtest=TraceServiceTest
```

**测试内容**：
- ✅ 四种追溯类型创建（derive/satisfy/refine/trace）
- ✅ 双向查询（getIncoming/getOutgoing）
- ✅ 循环依赖检测
- ✅ 关系去重逻辑
- ✅ Dependency统一实现验证

### 2.3 验证规则测试组

#### ValidationControllerTest (10个测试)

```bash
mvn test -Dtest=ValidationControllerTest
```

**测试内容**：
- ✅ REQ-E1-1: reqId唯一性校验
- ✅ REQ-E1-2: 循环依赖检测
- ✅ REQ-E1-3: 悬挂引用检测
- ✅ 批量验证执行
- ✅ 验证结果查询
- ✅ 清除验证结果

#### ValidationServiceTest (12个测试)

```bash
mvn test -Dtest=ValidationServiceTest
```

**测试内容**：
- ✅ 三条核心规则实现
- ✅ 循环检测算法（DFS）
- ✅ 悬挂引用扫描
- ✅ 验证性能（<100ms）

### 2.4 通用元素接口测试组

#### UniversalElementServiceTest (11个测试)

```bash
mvn test -Dtest=UniversalElementServiceTest
```

**测试内容**：
- ✅ REQ-B5-1: 创建任意SysML类型
- ✅ REQ-B5-2: 按类型查询元素
- ✅ REQ-B5-3: PATCH更新元素
- ✅ 动态EMF操作
- ✅ 182个EClass支持验证

### 2.5 项目管理测试组

#### ProjectControllerTest (7个测试)

```bash
mvn test -Dtest=ProjectControllerTest
```

**测试内容**：
- ✅ REQ-F2-1: 项目导出（JSON格式）
- ✅ REQ-F2-2: 项目导入
- ✅ REQ-F2-3: ID稳定性验证
- ✅ 树视图数据构建
- ✅ 图视图数据格式

### 2.6 高级查询测试组

#### AdvancedQueryControllerTest (14个测试)

```bash
mvn test -Dtest=AdvancedQueryControllerTest
```

**测试内容**：
- ✅ 分页查询
- ✅ 多条件过滤
- ✅ 关系路径查询
- ✅ 影响分析
- ✅ 性能验证（<500ms）

### 2.7 字段标准化测试组（新增）

#### FieldStandardizationTest (35个测试)

```bash
mvn test -Dtest=FieldStandardizationTest
```

**测试内容**：
- ✅ REQ-FS-1: M2核心字段设置测试（6个）
- ✅ REQ-FS-2: 标准字段映射测试（7个）
- ✅ REQ-FS-3: 动态EMF兼容性测试（6个）
- ✅ REQ-FS-4: Metadata机制测试（8个）
- ✅ REQ-FS-5: 回归完整性测试（8个）

---

## 3. 集成测试

### 3.1 SystemIntegrationTest (10个测试)

```bash
mvn test -Dtest=SystemIntegrationTest
```

**测试内容**：
- ✅ 端到端CRUD流程
- ✅ 三视图数据一致性
- ✅ 导入导出完整性
- ✅ 588个元素性能测试

### 3.2 SimpleIntegrationTest (10个测试)

```bash
mvn test -Dtest=SimpleIntegrationTest
```

**测试内容**：
- ✅ 基础集成场景
- ✅ 数据流验证
- ✅ 错误处理流程

---

## 4. EMF核心测试

### 4.1 EMFCoreTest (8个测试)

```bash
mvn test -Dtest=EMFCoreTest
```

**测试内容**：
- ✅ 182个EClass注册验证
- ✅ 继承层次验证
- ✅ 动态对象创建
- ✅ 属性设置验证

### 4.2 EMFCoreSimpleTest (7个测试)

```bash
mvn test -Dtest=EMFCoreSimpleTest
```

**测试内容**：
- ✅ JSON序列化/反序列化
- ✅ 循环引用处理
- ✅ ID稳定性
- ✅ 未知字段保留

---

## 5. 回归测试执行策略

### 5.1 完整回归测试

```bash
# 运行所有测试（约45秒）
mvn test

# 生成测试报告
mvn surefire-report:report
```

### 5.2 快速核心验证（30秒内）

```bash
# 核心功能快速验证
mvn test -Dtest="RequirementControllerTest,RequirementServiceTest,TraceControllerTest,TraceServiceTest" -q
```

### 5.3 分层测试

```bash
# Controller层测试
mvn test -Dtest="*ControllerTest"

# Service层测试
mvn test -Dtest="*ServiceTest"

# 集成测试
mvn test -Dtest="*IntegrationTest"
```

### 5.4 字段标准化专项测试

```bash
# 字段标准化验证
mvn test -Dtest="FieldStandardizationTest"

# of→requirementDefinition映射验证
mvn test -Dtest="RequirementServiceTest#*RequirementUsage*"
```

---

## 6. 测试环境配置

### 6.1 测试配置文件

```yaml
# src/test/resources/application-test.yml
spring:
  profiles:
    active: test

logging:
  level:
    com.sysml.mvp: DEBUG
    
# 测试数据路径
data:
  path: target/test-data
```

### 6.2 测试数据准备

```java
@TestConfiguration
public class TestDataConfig {
    
    @Bean
    public DemoDataInitializer demoDataInitializer() {
        return new DemoDataInitializer();
    }
    
    @PostConstruct
    public void initTestData() {
        // 加载电池系统演示数据
        demoDataInitializer.loadBatterySystemData();
    }
}
```

---

## 7. 持续集成配置

### 7.1 Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test -Dtest="*ServiceTest"'
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh 'mvn test -Dtest="*ControllerTest,*IntegrationTest"'
            }
        }
        
        stage('Field Standardization Tests') {
            steps {
                sh 'mvn test -Dtest="FieldStandardizationTest"'
            }
        }
        
        stage('Report') {
            steps {
                junit 'target/surefire-reports/*.xml'
            }
        }
    }
}
```

### 7.2 GitHub Actions

```yaml
name: MVP Regression Tests

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
        
    - name: Run MVP Regression Tests
      run: mvn test
      
    - name: Upload Test Results
      uses: actions/upload-artifact@v2
      with:
        name: test-results
        path: target/surefire-reports
```

---

## 8. 测试结果基线

### 8.1 当前基线（2025-08-28）

| 测试类别 | 测试数 | 通过 | 失败 | 跳过 |
|----------|--------|------|------|------|
| Controller层 | 51 | 51 | 0 | 0 |
| Service层 | 48 | 48 | 0 | 0 |
| 集成测试 | 20 | 20 | 0 | 0 |
| EMF核心 | 15 | 15 | 0 | 0 |
| 字段标准化 | 35 | 35 | 0 | 0 |
| 其他 | 48 | 48 | 0 | 0 |
| **总计** | **217** | **217** | **0** | **0** |

### 8.2 关键性能指标

- API响应时间: < 100ms (平均)
- 500节点模型操作: < 500ms
- 验证规则执行: < 100ms
- 导入导出(10MB): < 2s

---

## 9. 问题处理指南

### 9.1 常见测试失败原因

| 问题 | 原因 | 解决方法 |
|------|------|----------|
| reqId重复 | 测试数据未清理 | 使用@DirtiesContext |
| 字段找不到 | of→requirementDefinition | 确认字段映射正确 |
| 超时 | 性能问题 | 检查数据量，优化查询 |
| 序列化失败 | 循环引用 | 使用$ref处理 |

### 9.2 调试技巧

```bash
# 单个测试调试
mvn test -Dtest=RequirementServiceTest#testCreateRequirement_ShouldValidateReqIdUniqueness -Dmaven.surefire.debug

# 查看详细日志
mvn test -Dtest=RequirementServiceTest -Dlogging.level.com.sysml.mvp=TRACE

# 生成覆盖率报告
mvn jacoco:report
```

---

## 10. Phase 1 开发注意事项

### 10.1 测试先行原则

1. **不允许破坏现有测试**：所有217个测试必须保持通过
2. **新功能先写测试**：TDD原则，测试驱动开发
3. **提交前回归测试**：运行完整测试套件

### 10.2 基线保护

```bash
# Phase 1开发前建立基线
git tag mvp-baseline-v1.0
mvn test > mvp-baseline-test-results.txt

# 每次提交前验证
mvn test
git diff mvp-baseline-test-results.txt current-test-results.txt
```

### 10.3 测试扩展规范

- 新测试类命名：`*Test.java`
- 测试方法命名：`test<功能>_Should<预期行为>`
- 使用@DisplayName注解
- 关联需求ID注释

---

## 11. 总结

本回归测试套件包含**217个测试用例**，覆盖了MVP的所有核心功能，包括最新的字段标准化变更。所有测试**100%通过**，为Phase 1开发提供了稳定的基线。

**关键成果**：
- ✅ 完整的测试覆盖
- ✅ 字段标准化验证
- ✅ 性能基线建立
- ✅ CI/CD就绪

此测试套件将作为MVP阶段的最终交付物，确保产品质量和后续开发的稳定性。