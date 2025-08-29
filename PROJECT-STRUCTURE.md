# SysML v2建模平台 - 项目架构文档

## 概述

SysML v2建模平台是一个**单体架构**的Web应用，采用Spring Boot后端 + React前端，专注于需求建模与追溯关系管理。本文档详细描述了MVP版本的完整项目结构、核心组件和开发指南。

## 🏗️ 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     SysML v2建模平台                        │
├─────────────────────────────────────────────────────────────┤
│  Frontend (React 18 + TypeScript)                          │
│  ├─ TreeView      ├─ TableView      ├─ GraphView           │
│  ├─ Ant Design 5  ├─ React Flow 11  ├─ Context API         │
│  └─ 三视图联动同步机制                                        │
├─────────────────────────────────────────────────────────────┤
│  Backend (Spring Boot 3.2 + Java 17)                      │
│  ├─ RESTful API   ├─ EMF Integration ├─ File Repository     │
│  ├─ Validation    ├─ Exception      ├─ Test Coverage        │
│  └─ JSON序列化                                               │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                 │
│  ├─ EMF Resource (Single Source of Truth)                  │
│  ├─ JSON File Storage                                       │
│  └─ SysML Pilot Metamodel Registry                         │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 MVP功能验证完成

### 测试结果汇总
- **总测试数**: 51个
- **成功率**: 100% (51/51) 
- **Controller层覆盖**: 完整覆盖所有REST接口
- **核心功能**: 需求CRUD、追溯关系、验证规则全部通过测试

### 已验证功能列表
✅ RequirementDefinition CRUD完整实现  
✅ RequirementUsage支持  
✅ Trace追溯关系管理  
✅ 数据验证规则（reqId唯一性、循环依赖、悬挂引用）  
✅ 项目导入导出功能  
✅ 高级查询接口  
✅ 演示数据生成  
✅ 错误处理与异常管理

## 📁 后端架构 (Spring Boot)

### 代码结构
```
backend/src/main/java/com/sysml/mvp/
├── Application.java                    # Spring Boot应用入口
├── config/                            # 配置类
│   └── CorsConfig.java               # 跨域配置
├── controller/                        # REST控制器层 ✅完成
│   ├── RequirementController.java    # 需求CRUD接口 (11个测试通过)
│   ├── TraceController.java          # 追溯关系接口 (9个测试通过)
│   ├── ValidationController.java     # 验证规则接口 (10个测试通过)
│   ├── ProjectController.java        # 项目管理接口 (7个测试通过)
│   ├── AdvancedQueryController.java  # 高级查询接口 (14个测试通过)
│   └── DemoDataController.java       # 演示数据接口
├── service/                          # 业务逻辑层 ✅完成
│   └── impl/
│       └── ProjectServiceImpl.java   # 项目服务实现
├── repository/                       # 数据访问层 ✅完成
│   └── FileModelRepository.java      # 文件系统仓库
├── model/                           # EMF模型定义 ✅完成
│   └── EMFModelRegistry.java        # EMF注册表
├── dto/                            # 数据传输对象 ✅完成
│   ├── RequirementDTO.java         # 需求DTO
│   ├── TraceDTO.java               # 追溯关系DTO
│   ├── ValidationResultDTO.java    # 验证结果DTO
│   └── ElementDTO.java             # 元素DTO
├── mapper/                         # 对象映射器 ✅完成
│   └── ElementMapper.java          # 元素映射器
├── exception/                      # 异常处理 ✅完成
│   └── GlobalExceptionHandler.java # 全局异常处理器
└── data/                          # 演示数据生成 ✅完成
    ├── TestDataSetGenerator.java  # 测试数据生成器
    └── BatterySystemDemoDataGenerator.java # 电池系统演示数据
```

### 核心组件说明

#### 1. **EMF集成** (`EMFModelRegistry.java`)
- 从SysML Pilot项目注册标准元模型
- 命名空间：`https://www.omg.org/spec/SysML/20240201`
- 支持182个SysML类的完整继承结构
- 自定义ResourceFactory处理URI协议

#### 2. **文件存储** (`FileModelRepository.java`)
- JSON文件存储：`data/projects/{projectId}/model.json`
- 使用EMF原生JSON格式序列化
- 支持循环引用的$ref机制

#### 3. **核心数据模型**
```java
// RequirementDefinition - 标准化需求定义
{
  "reqId": "REQ-001",        // 唯一标识符
  "name": "System Power",     // 需求名称
  "text": "System shall...",  // 需求描述
  "tags": ["critical"],      // 标签
  "isAbstract": false        // 是否抽象
}

// RequirementUsage - 需求使用实例
{
  "reqId": "REQ-001-IMPL",   // 实例标识符
  "definition": "REQ-001",   // 关联的定义
  "context": "Battery"       // 使用上下文
}

// Trace - 追溯关系
{
  "traceType": "derive",     // 关系类型：derive/satisfy/refine/trace
  "sourceId": "REQ-001",     // 源需求ID
  "targetId": "REQ-002"      // 目标需求ID
}
```

#### 4. **验证规则**
- **REQ-ID唯一性**：确保reqId在项目内唯一
- **循环依赖检测**：防止追溯关系形成环路
- **悬挂引用检测**：确保所有引用的需求存在

### API接口设计

**基础路径**: `/api/v1`

#### 需求管理接口
```
POST   /api/v1/requirements                     # 创建需求
GET    /api/v1/requirements                     # 获取需求列表
GET    /api/v1/requirements/{id}                # 获取单个需求
PUT    /api/v1/requirements/{id}                # 更新需求
DELETE /api/v1/requirements/{id}                # 删除需求
GET    /api/v1/requirements/definition/{reqId}  # 根据reqId获取需求
```

#### 追溯关系接口
```
POST   /api/v1/traces          # 创建追溯关系
GET    /api/v1/traces          # 获取追溯关系列表
DELETE /api/v1/traces/{id}     # 删除追溯关系
```

#### 验证接口
```
GET    /api/v1/validation/rules     # 获取验证规则
POST   /api/v1/validation/validate  # 执行验证
```

## 🎨 前端架构 (React)

### 代码结构
```
frontend/src/
├── main.tsx                    # 应用入口
├── App.tsx                     # 主应用组件
├── components/                 # UI组件库 ✅已实现
│   ├── tree/
│   │   ├── TreeView.tsx       # 树视图组件
│   │   └── TreeViewSimple.tsx # 简化树视图
│   ├── table/
│   │   └── TableView.tsx      # 表视图组件
│   ├── graph/
│   │   ├── GraphView.tsx      # 图视图组件
│   │   └── SimpleGraph.tsx    # 简化图视图
│   ├── dialogs/               # 对话框组件 ✅已实现
│   │   ├── CreateRequirementDialog.tsx # 创建需求对话框
│   │   └── EditRequirementDialog.tsx   # 编辑需求对话框
│   ├── layout/
│   │   └── MainLayout.tsx     # 主布局组件
│   └── common/
│       └── ErrorBoundary.tsx  # 错误边界
├── contexts/                  # 状态管理 ✅已实现
│   └── ModelContext.tsx       # 全局状态管理
├── services/                  # API服务 ✅已实现
│   ├── api.ts                 # 基础API服务
│   ├── requirementService.ts  # 需求服务
│   └── universalApi.ts        # 通用API服务
├── types/                     # TypeScript类型 ✅已实现
│   └── models.ts              # TypeScript类型定义
└── utils/                     # 工具函数
```

### 核心功能特性

#### 1. **三视图联动同步**
- **TreeView**: 层次结构展示，支持拖拽操作
- **TableView**: 表格形式的详细信息编辑
- **GraphView**: 基于React Flow的依赖关系可视化
- **实时同步**: Context API管理全局状态，视图间数据自动同步

#### 2. **状态管理** (`ModelContext.tsx`)
```typescript
interface ModelContextType {
  requirements: RequirementDefinition[];      // 需求列表
  traces: Trace[];                            // 追溯关系
  selectedRequirement: string | null;        // 选中的需求
  loading: boolean;                           // 加载状态
  error: string | null;                       // 错误信息
}
```

#### 3. **组件设计原则**
- **单一职责**: 每个组件专注一个特定功能
- **可复用性**: 通用组件支持多种使用场景
- **类型安全**: 完整的TypeScript类型定义
- **错误处理**: ErrorBoundary保证应用稳定性

## 🗄️ 数据存储架构

### 文件系统结构
```
data/
├── projects/
│   ├── {projectId}/
│   │   ├── model.json        # EMF模型数据
│   │   └── metadata.json     # 项目元数据
│   └── default/              # 默认项目
├── logs/                     # 应用日志
├── demo/                     # 演示数据
└── backups/                  # 数据备份
```

### EMF JSON格式示例
```json
{
  "eClass": "https://www.omg.org/spec/SysML/20240201#//RequirementDefinition",
  "reqId": "REQ-POWER-001",
  "name": "Battery Power Requirement",
  "text": "The battery system shall provide minimum 100kWh capacity",
  "tags": ["critical", "performance"],
  "isAbstract": false,
  "_id": "uuid-1234-5678-90ab"
}
```

## 🧪 测试架构

### 测试覆盖策略
```
backend/src/test/java/
├── controller/                # 控制器层测试 ✅100%通过
│   ├── RequirementControllerTest.java    # 11个测试用例
│   ├── TraceControllerTest.java          # 9个测试用例
│   ├── ValidationControllerTest.java     # 10个测试用例
│   ├── ProjectControllerTest.java        # 7个测试用例
│   └── AdvancedQueryControllerTest.java  # 14个测试用例
├── service/                  # 服务层测试
├── repository/               # 数据层测试
└── integration/              # 集成测试
```

### 测试结果汇总
- **总测试数**: 51个
- **成功率**: 100% (51/51)
- **覆盖范围**: Controller层完整覆盖
- **验证内容**: CRUD操作、异常处理、边界条件

### 前端测试
```
frontend/src/__tests__/
├── components/               # 组件测试 ✅已实现
├── contexts/                # 状态管理测试 ✅已实现
├── services/                # API服务测试 ✅已实现
└── integration/             # 集成测试
```

## 📊 性能与限制

### 性能目标
- **模型规模**: 支持≤500节点
- **API响应时间**: <500ms
- **并发用户**: ≤5用户
- **内存使用**: <2GB

### MVP版本限制
- ❌ 不支持分布式架构
- ❌ 不支持复杂权限管理
- ❌ 不支持实时协作
- ❌ 不支持完整SysML v2元模型
- ❌ 不支持数据库存储
- ❌ 不支持大规模并发

## 🔄 开发工作流

### Git分支策略
```
main            # 生产发布分支
├── develop     # 开发集成分支
├── feature/*   # 功能开发分支
├── hotfix/*    # 紧急修复分支
└── release/*   # 发布准备分支
```

### 开发命令
```bash
# 后端开发
mvn clean compile          # 编译项目
mvn spring-boot:run        # 启动服务 (端口8080)
mvn test                   # 运行测试
mvn clean package          # 构建JAR包

# 前端开发
npm install                # 安装依赖
npm run dev               # 开发模式 (端口5173)
npm run build             # 生产构建
npm run test              # 运行测试
```

## 📈 需求-测试对齐矩阵

基于`tracking/tracking-matrix.yaml`的追溯分析：
- **需求总数**: 51个
- **测试方法**: 182个
- **覆盖率**: 82.4%
- **高优先级缺口**: 4个 (REQ-B2-2, REQ-C2-5, REQ-B5-5, REQ-D1-1)

## 🚀 部署架构

### 开发环境
- **后端**: Spring Boot DevTools热重载
- **前端**: Vite开发服务器
- **数据**: 本地文件存储

### 生产环境（建议）
```
┌─────────────────┐    ┌─────────────────┐
│   Nginx         │────│  Spring Boot    │
│   (静态文件)     │    │  (API服务)      │
│   Port: 80/443  │    │  Port: 8080     │
└─────────────────┘    └─────────────────┘
                               │
                       ┌───────────────┐
                       │   File System │
                       │   (JSON存储)  │
                       └───────────────┘
```

## 📝 技术债务与改进建议

### 短期改进
1. **JSON序列化优化**: 解决EMF Jackson循环引用问题
2. **前端组件优化**: 提升大数据量渲染性能  
3. **API缓存**: 添加适当的响应缓存机制
4. **错误处理**: 完善前后端错误处理流程

### 长期规划
1. **数据库迁移**: 从文件存储迁移到PostgreSQL
2. **微服务架构**: 拆分为独立的服务模块
3. **实时协作**: WebSocket支持多用户协作
4. **完整元模型**: 支持完整的SysML v2规范

## 📁 项目目录结构

### 根目录结构
```
sysml2 v2/
├── backend/                    # Spring Boot后端 ✅完成
├── frontend/                   # React前端 ✅完成
├── docs/                      # 项目文档
├── tracking/                  # 追溯矩阵
├── scripts/                   # 开发脚本
├── data/                     # 数据存储
├── opensource/               # 开源参考项目
└── tests/                    # 集成测试
## 📄 相关文档

- [需求文档](docs/requirements/需求文档.md)
- [API文档](docs/design/mvp接口文档.md) 
- [Git工作流程](docs/GIT-WORKFLOW-GUIDE.md)
- [测试报告](docs/testing/TEST-REPORT.md)
- [发布说明](docs/releases/MVP-v1.0-release-notes.md)

---

**最后更新**: 2025-08-27  
**版本**: MVP v2.0  
**维护者**: SysML v2建模平台开发团队