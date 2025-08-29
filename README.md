# SysML v2 建模平台 MVP

基于Spring Boot和React的SysML v2建模平台最小可行产品（MVP）实现。

## 项目概述

本项目实现了一个轻量级的SysML v2建模平台，支持需求定义（RequirementDefinition）、需求使用（RequirementUsage）和追溯关系（Trace）的管理，提供树视图、表视图和图视图三种联动的可视化方式。

## 技术栈

### 后端
- **Java 17** + **Spring Boot 3.2**
- **Eclipse EMF 2.35** - 元模型框架
- **JSON文件存储** - 无需数据库依赖
- **Maven** - 项目构建

### 前端
- **React 18** + **TypeScript 5**
- **React Flow 11** - 图形可视化
- **Ant Design 5** - UI组件库
- **Vite 5** - 构建工具

## 快速开始

### 前置要求

- Java 17+
- Maven 3.6+
- Node.js 16+
- npm 8+

### 安装依赖

```bash
# Windows
scripts\dev.bat --install

# Linux/Mac
chmod +x scripts/dev.sh
./scripts/dev.sh --install
```

### 启动开发环境

```bash
# Windows
scripts\dev.bat

# Linux/Mac
./scripts/dev.sh
```

服务启动后访问：
- 前端界面：http://localhost:3000
- 后端API：http://localhost:8080/api/v1
- API文档：http://localhost:8080/api/v1/swagger-ui.html

### 手动启动

```bash
# 启动后端
cd backend
mvn spring-boot:run

# 启动前端（新终端）
cd frontend
npm run dev
```

## 项目结构

```
sysml2-v2/
├── backend/                 # 后端Spring Boot项目
│   ├── src/main/java/      # Java源码
│   ├── src/main/resources/ # 配置文件
│   └── pom.xml             # Maven配置
├── frontend/               # 前端React项目
│   ├── src/                # TypeScript源码
│   ├── public/             # 静态资源
│   └── package.json        # npm配置
├── data/                   # 数据存储目录
│   ├── projects/           # 项目文件
│   ├── backups/            # 备份文件
│   └── demo/               # 演示数据
├── docs/                   # 项目文档
├── opensource/             # 外部参考资源
└── scripts/                # 开发脚本

```

## 核心功能

### 需求管理
- 创建、编辑、删除需求定义和需求使用
- 支持标签、状态管理
- reqId唯一性校验

### 追溯关系
- 支持derive、satisfy、refine、trace四种关系类型
- 自动去重和循环依赖检测
- 双向关系查询

### 三视图联动
- **树视图**：层级结构展示
- **表视图**：批量数据管理
- **图视图**：关系可视化（基于React Flow）

### 数据导入导出
- JSON格式导入导出
- EMF原生格式支持
- 项目备份恢复

## API接口

完整API文档请参考：[MVP接口文档](docs/mvp接口文档.md)

主要接口：
- `GET /health` - 健康检查
- `GET/POST /projects/{pid}/requirements` - 需求管理
- `POST /projects/{pid}/requirements/{id}/traces` - 追溯关系
- `GET /projects/{pid}/tree|table|graph` - 视图数据

## 开发指南

### 后端开发

```bash
cd backend
mvn test                    # 运行完整测试套件
mvn test -Dtest=RequirementControllerTest  # 运行特定测试类
mvn package                 # 打包
java -jar target/*.jar      # 运行JAR包
```

**测试套件覆盖范围**：
- ✅ **控制器层测试**（5个测试类）
  - `RequirementControllerTest` - 需求CRUD操作
  - `TraceControllerTest` - 追溯关系管理
  - `ValidationControllerTest` - 数据验证规则
  - `ProjectControllerTest` - 项目导入导出
  - `AdvancedQueryControllerTest` - 高级查询功能

- ✅ **服务层测试**（4个测试类）
  - `RequirementServiceTest` - 需求业务逻辑
  - `TraceServiceTest` - 追溯关系业务逻辑
  - `ValidationServiceTest` - 验证服务
  - `UniversalElementServiceTest` - 通用元素服务

- ✅ **集成测试**（2个测试类）
  - `SystemIntegrationTest` - 系统级集成测试
  - `SimpleIntegrationTest` - 基础集成测试

- ✅ **数据与模型测试**（6个测试类）
  - `EMFCoreTest` / `EMFCoreSimpleTest` - EMF基座测试
  - `BatterySystemDemoDataTest` - 演示数据验证
  - DTO映射和数据转换测试

**⚠️ 回归测试要求**：
```bash
# Phase 1开发前必须运行，确保基线功能正常
mvn test

# Phase 1开发过程中，每次提交前必须运行
mvn test -Dtest="*Test"

# 重点回归测试（核心功能不可破坏）
mvn test -Dtest="RequirementControllerTest,TraceControllerTest,ValidationControllerTest"
```

**📊 MVP基线测试状态**（v3.3）：
- ✅ **51个测试用例全部通过**
- ✅ **API接口100%覆盖**（所有Controller端点）
- ✅ **业务逻辑验证完整**（CRUD + Validation）
- ✅ **集成测试通过**（端到端数据流）

**⚠️ Phase 1开发注意事项**：
1. **不允许修改现有测试用例**，除非需求明确变更
2. **所有新功能必须先写测试**（TDD原则）
3. **提交代码前必须确保`mvn test`全部通过**
4. **破坏现有功能的PR将被拒绝**

**🔍 快速验证命令**：
```bash
# 验证核心API功能（30秒内完成）
mvn test -Dtest="RequirementControllerTest" -q

# 验证EMF基座功能
mvn test -Dtest="EMFCoreTest" -q

# 验证数据验证功能
mvn test -Dtest="ValidationServiceTest" -q
```

### 前端开发

```bash
cd frontend
npm run dev                 # 开发模式
npm run build              # 生产构建
npm run test               # 运行测试
npm run lint               # 代码检查
```

## 配置说明

### 后端配置
配置文件：`backend/src/main/resources/application.yml`
- 端口：8080
- 数据目录：./data
- 最大节点数：500
- 并发用户数：5

### 前端配置
配置文件：`frontend/vite.config.ts`
- 端口：3000
- API代理：http://localhost:8080

## 性能限制

- 模型规模：≤500节点
- 文件大小：≤10MB
- 并发用户：≤5
- API响应：<500ms

## 许可证

本项目采用私有许可，仅供学习和研究使用。

## 回归测试

> **重要：单人开发必读** - 确保新功能不破坏现有功能

### 🚀 开发完成后必须运行的测试

#### 改了后端代码（Java/Spring Boot）
```bash
cd backend
mvn test -Dtest="RequirementServiceTest,UniversalElementServiceTest,FieldStandardizationTest" -q
mvn test -q  # 如果上面通过
./quick-test.sh
```

#### 改了前端代码（React/TypeScript）
```bash
cd frontend  
npm test -- --run src/__tests__/simple.test.ts src/__tests__/simple-react.test.tsx
./quick-test.sh
```

#### 改了API接口或数据结构
```bash
./scripts/regression-suite.sh core  # 完整回归测试
# 额外验证删除持久化功能
```

#### 最快验证（2分钟）
```bash
./quick-test.sh
# 必须显示："🎉 快速回归测试完成 - 所有核心功能正常！"
```

### 🚨 绝对不能省略的测试

**删除持久化验证**（最重要 - 曾经是用户报告的重大bug）：
```bash
# 创建->删除->重启服务->验证数据真的删除了
TEST_ID="DEL-$(date +%s)"
curl -X POST "http://localhost:8080/api/v1/requirements" -H "Content-Type: application/json" -d "{\"elementId\":\"$TEST_ID\",\"reqId\":\"$TEST_ID\",\"name\":\"删除测试\"}"
curl -X DELETE "http://localhost:8080/api/v1/requirements/$TEST_ID"
# 重启后端，确认数据真的删除了
```

📖 **完整测试指南**: [TESTING.md](TESTING.md)

## 相关文档

- [需求文档](docs/需求文档.md)
- [架构设计文档](docs/mvp架构设计文档.md)
- [API接口文档](docs/mvp接口文档.md)
- [回归测试指南](TESTING.md)
- [参考资源清单](opensource/README-参考资源.md)