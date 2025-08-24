# SysML v2 MVP 项目结构规范

## 📁 实际目录结构

```
sysml2-v2/
├── 📂 backend/                    # 后端Spring Boot项目 ✅已创建
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sysml/mvp/
│   │   │   │   ├── controller/   # REST控制器 ✅
│   │   │   │   │   ├── ApiController.java ✅             # 前端数据初始化API
│   │   │   │   │   ├── HealthController.java ✅          # 健康检查
│   │   │   │   │   ├── ProjectController.java ✅         # 项目导入导出
│   │   │   │   │   ├── RequirementController.java ✅     # 需求CRUD
│   │   │   │   │   ├── TraceController.java ✅           # 追溯关系
│   │   │   │   │   └── ViewController.java ✅            # 三视图API
│   │   │   │   ├── service/      # 业务服务层 ✅
│   │   │   │   │   ├── ProjectService.java ✅            # 项目服务
│   │   │   │   │   ├── RequirementService.java ✅        # 需求服务
│   │   │   │   │   ├── TraceService.java ✅              # 追溯服务
│   │   │   │   │   └── ViewService.java ✅               # 视图服务
│   │   │   │   ├── repository/   # 数据访问层 ✅
│   │   │   │   │   └── FileModelRepository.java ✅       # 文件存储
│   │   │   │   ├── model/        # EMF模型定义 ✅
│   │   │   │   │   └── EMFModelRegistry.java ✅          # EMF注册
│   │   │   │   ├── dto/          # 数据传输对象 ✅
│   │   │   │   │   ├── GraphDataDTO.java ✅              # 图数据DTO
│   │   │   │   │   ├── GraphEdgeDTO.java ✅              # 图边DTO
│   │   │   │   │   ├── GraphNodeDTO.java ✅              # 图节点DTO
│   │   │   │   │   ├── HealthResponse.java ✅            # 健康响应
│   │   │   │   │   ├── ModelHealthResponse.java ✅       # 模型健康响应
│   │   │   │   │   ├── RequirementDefinitionDTO.java ✅  # 需求定义DTO
│   │   │   │   │   ├── RequirementUsageDTO.java ✅       # 需求使用DTO
│   │   │   │   │   ├── TableRowDTO.java ✅               # 表格行DTO
│   │   │   │   │   ├── TraceDTO.java ✅                  # 追溯DTO
│   │   │   │   │   └── TreeNodeDTO.java ✅               # 树节点DTO
│   │   │   │   ├── command/      # 命令模式 ✅
│   │   │   │   │   └── DataGenerationCommand.java ✅     # 数据生成命令
│   │   │   │   ├── exception/    # 异常处理 ✅
│   │   │   │   │   └── GlobalExceptionHandler.java ✅    # 全局异常处理
│   │   │   │   ├── util/         # 工具类 ✅
│   │   │   │   │   ├── DemoDataGenerator.java ✅         # Demo数据生成
│   │   │   │   │   └── EObjectSerializer.java ✅         # 对象序列化
│   │   │   │   └── Application.java ✅                   # 主类
│   │   │   └── resources/
│   │   │       └── application.yml ✅                    # 配置文件
│   │   └── test/                 # 测试代码 ✅
│   │       ├── java/com/sysml/mvp/
│   │       │   ├── controller/
│   │       │   │   ├── ProjectApiControllerTest.java ✅  # 项目API测试
│   │       │   │   └── ViewControllerTest.java ✅        # 视图控制器测试
│   │       │   ├── ApiControllerTest.java ✅             # API控制器测试
│   │       │   ├── DemoDataGeneratorTest.java ✅         # Demo数据测试
│   │       │   ├── HealthControllerTest.java ✅          # 健康检查测试
│   │       │   ├── ProjectControllerTest.java ✅         # 项目控制器测试
│   │       │   ├── RequirementUsageControllerTest.java ✅ # 需求使用测试
│   │       │   ├── RequirementsComplianceUnitTest.java ✅ # 合规性测试
│   │       │   ├── TraceControllerTest.java ✅           # 追溯控制器测试
│   │       │   └── ViewControllerTest.java ✅            # 视图控制器测试
│   │       └── resources/
│   │           └── application-test.yml ✅               # 测试配置
│   ├── data/                     # 运行时数据
│   │   ├── projects/             # 项目数据
│   │   │   └── default/
│   │   │       ├── model.json ✅
│   │   │       └── metadata.json ✅
│   │   ├── demo-project.json ✅   # Demo项目数据
│   │   ├── small-project.json ✅  # 小型项目数据(10节点)
│   │   ├── medium-project.json ✅ # 中型项目数据(100节点)
│   │   └── large-project.json ✅  # 大型项目数据(500节点)
│   ├── target/                   # Maven构建目录（.gitignore）
│   └── pom.xml ✅                 # Maven配置
│
├── 📂 frontend/                   # 前端React项目 ✅
│   ├── src/
│   │   ├── components/           # React组件
│   │   │   ├── views/           # 视图组件
│   │   │   │   ├── TreeView/
│   │   │   │   │   └── TreeView.tsx ✅
│   │   │   │   ├── TableView/
│   │   │   │   │   ├── TableView.tsx ✅
│   │   │   │   │   └── TableView.test.tsx ✅
│   │   │   │   └── GraphView/
│   │   │   │       ├── GraphView.tsx ✅
│   │   │   │       └── GraphView.test.tsx ✅
│   │   │   └── layout/          # 布局组件
│   │   │       └── MainLayout.tsx ✅
│   │   ├── services/            # API服务
│   │   │   └── api.ts ✅
│   │   ├── contexts/            # React Context
│   │   │   └── ModelContext.tsx ✅
│   │   ├── types/               # TypeScript类型
│   │   │   └── models.ts ✅
│   │   ├── test/                # 测试配置
│   │   │   └── setup.ts ✅
│   │   ├── App.tsx ✅            # 主应用组件
│   │   └── main.tsx ✅           # 入口文件
│   ├── public/                  # 静态资源
│   ├── node_modules/            # 依赖包（.gitignore）
│   ├── package.json ✅
│   ├── package-lock.json ✅
│   ├── tsconfig.json ✅
│   ├── tsconfig.node.json ✅
│   ├── vite.config.ts ✅
│   └── vite.config.test.ts ✅
│
├── 📂 docs/                       # 项目文档
│   ├── requirements/             # 需求相关
│   │   └── 需求文档.md ✅        # 需求规格说明
│   ├── design/                  # 设计相关
│   │   ├── mvp架构设计文档.md ✅  # 架构设计
│   │   ├── mvp接口文档.md ✅      # API接口定义
│   │   └── SystemDesignDocument2.md ✅ # 系统设计文档
│   ├── guides/                  # 指南文档
│   │   └── API测试示例.md ✅      # API测试指南
│   ├── templates/               # 文档模板
│   │   ├── design-template.md ✅
│   │   ├── requirement-template.md ✅
│   │   └── test-template.md ✅
│   └── rules.md ✅                # 规则文档
│
├── 📂 tracking/                   # 项目追踪文件
│   └── tracking-matrix.yaml ✅   # 需求追踪矩阵
│
├── 📂 data/                       # 根目录数据文件
│   ├── demo-project.json ✅      # Demo项目数据
│   ├── small-project.json ✅     # 小型项目数据
│   ├── medium-project.json ✅    # 中型项目数据
│   └── large-project.json ✅     # 大型项目数据
│
├── 📂 opensource/                 # 外部参考资源（只读）
│   ├── SysML-v2-API-Services/   # SysML官方API服务
│   │   └── ... (众多Java文件)
│   └── README-参考资源.md ✅      # 资源说明
│
├── 📄 README.md ✅                 # 项目说明
├── 📄 PROJECT-STRUCTURE.md ✅      # 本文档
├── 📄 CLAUDE.md ✅                 # Claude AI指导文档
└── 📄 demo-project.json ✅        # 根目录Demo文件
```

## 📊 项目统计

### 后端文件统计
- **控制器**: 6个文件
- **服务层**: 4个文件
- **DTO**: 10个文件
- **测试文件**: 11个文件
- **工具类**: 2个文件
- **总Java文件**: 38个

### 前端文件统计
- **组件文件**: 6个TSX文件
- **测试文件**: 3个测试文件
- **配置文件**: 6个
- **总TypeScript文件**: 12个

### 文档统计
- **需求文档**: 1个
- **设计文档**: 3个
- **模板文档**: 3个
- **指南文档**: 1个
- **总文档**: 11个

## 📝 文档命名规范

### 1. 文档类型前缀
- `REQ-` : 需求文档
- `ARCH-` : 架构文档
- `API-` : 接口文档
- `TEST-` : 测试文档
- `GUIDE-` : 指南文档
- `SPEC-` : 规格说明

### 2. 文件命名规则
- 使用中文或英文均可，但同一目录下保持一致
- 使用连字符分隔单词：`mvp-架构设计文档.md`
- 版本号用v前缀：`需求文档-v2.1.md`
- 日期格式：`meeting-notes-2025-01-15.md`

### 3. 代码文件命名
- Java类：PascalCase `RequirementService.java`
- TypeScript组件：PascalCase `TreeView.tsx`
- 工具函数：camelCase `dateUtils.ts`
- 常量文件：UPPER_CASE `API_CONSTANTS.ts`

## 🔖 文档分类规则

### 核心文档（docs/）
- **永久保存**：需求、设计、架构文档
- **版本控制**：每次重大修改创建新版本
- **必须评审**：修改需要记录和评审

### 追踪文档（tracking/）
- **实时更新**：追踪矩阵、测试报告
- **YAML格式**：便于程序解析
- **自动生成**：部分内容可自动生成

### 参考文档（opensource/）
- **只读资源**：外部克隆的代码库
- **不要修改**：保持原始状态
- **定期更新**：跟踪上游更新

## 🚫 禁止事项

1. **不要**在根目录随意创建文件
2. **不要**将生成的文件提交到Git
3. **不要**将敏感信息写入文档
4. **不要**修改opensource目录的内容
5. **不要**将大文件（>10MB）提交到仓库

## ✅ 最佳实践

1. **文档先行**：编码前先更新设计文档
2. **及时更新**：代码变更同步更新文档
3. **保持简洁**：文档应该清晰、简洁
4. **使用模板**：统一的文档模板
5. **定期整理**：每个Sprint结束整理文档

## 🔄 文档更新流程

1. **创建分支**：`doc/feature-name`
2. **更新文档**：在相应目录更新
3. **更新追踪**：更新tracking-matrix.yaml
4. **提交评审**：创建Pull Request
5. **合并主干**：评审通过后合并

## 📊 文档质量要求

- **完整性**：包含所有必要信息
- **准确性**：与代码实现一致
- **可读性**：结构清晰，易于理解
- **可追踪**：需求到实现可追踪
- **时效性**：及时更新，避免过时

---

## 📅 版本历史

### v1.3 (2025-08-24)
- ✅ 根据实际文件扫描结果更新项目结构
- ✅ 添加所有已实现的控制器、服务、DTO文件
- ✅ 更新测试文件列表（62个测试全部通过）
- ✅ 添加前端组件实际文件路径
- ✅ 更新项目统计数据
- ✅ 标记所有已完成的文件状态

### v1.2 (2025-08-23)
- ✅ Trace CRUD功能完整实现（REQ-C3-1到REQ-C3-4）
- ✅ TraceController和TraceService完成
- ✅ TraceControllerTest综合测试编写
- ✅ API文档更新，四个文档完全对齐
- ✅ 项目结构规范化，文档归位
- ✅ 代码覆盖率提升到23.4%

### v1.1 (2025-08-23)
- ✅ 后端基础框架搭建完成
- ✅ EMF模型注册实现（urn:your:sysml2）
- ✅ RequirementDefinition CRUD接口实现
- ✅ 测试用例编写（REQ-C1-1, REQ-C1-2, REQ-C1-3）
- ✅ EMF JSON序列化问题已解决（使用sirius-emfjson）

### v1.0 (2025-01-15)
- 初始项目结构定义
- 文档规范制定

---

**最后更新**: 2025-08-24
**维护者**: Development Team
**版本**: 1.3