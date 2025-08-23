# SysML v2 MVP 项目结构规范

## 📁 目录结构定义

```
sysml2-v2/
├── 📂 backend/                    # 后端Spring Boot项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sysml/mvp/
│   │   │   │   ├── controller/   # REST控制器
│   │   │   │   ├── service/      # 业务服务层
│   │   │   │   ├── repository/   # 数据访问层
│   │   │   │   ├── model/        # EMF模型定义
│   │   │   │   ├── dto/          # 数据传输对象
│   │   │   │   ├── config/       # 配置类
│   │   │   │   ├── exception/    # 异常处理
│   │   │   │   ├── util/         # 工具类
│   │   │   │   └── validation/   # 校验逻辑
│   │   │   └── resources/
│   │   │       ├── model/        # Ecore模型文件
│   │   │       ├── data/         # 初始化数据
│   │   │       └── application.yml
│   │   └── test/                 # 测试代码
│   └── pom.xml
│
├── 📂 frontend/                   # 前端React项目
│   ├── src/
│   │   ├── components/           # React组件
│   │   │   ├── views/           # 视图组件
│   │   │   │   ├── TreeView/
│   │   │   │   ├── TableView/
│   │   │   │   └── GraphView/
│   │   │   ├── common/          # 通用组件
│   │   │   └── layout/          # 布局组件
│   │   ├── services/            # API服务
│   │   ├── contexts/            # React Context
│   │   ├── hooks/               # 自定义Hooks
│   │   ├── utils/               # 工具函数
│   │   ├── types/               # TypeScript类型
│   │   └── styles/              # 样式文件
│   ├── public/                  # 静态资源
│   └── package.json
│
├── 📂 docs/                       # 项目文档（核心文档）
│   ├── requirements/             # 需求相关
│   │   ├── 需求文档.md          # 需求规格说明
│   │   └── 用例文档.md          # 用例描述
│   ├── design/                  # 设计相关
│   │   ├── mvp架构设计文档.md   # 架构设计
│   │   ├── mvp接口文档.md       # API接口定义
│   │   └── 数据模型设计.md      # 数据模型
│   ├── guides/                  # 指南文档
│   │   ├── 开发指南.md
│   │   ├── 部署指南.md
│   │   └── 测试指南.md
│   └── meeting-notes/           # 会议记录
│
├── 📂 tracking/                   # 项目追踪文件
│   ├── tracking-matrix.yaml     # 需求追踪矩阵
│   ├── sprint-planning.yaml     # Sprint计划
│   └── test-report.yaml         # 测试报告
│
├── 📂 data/                       # 运行时数据目录
│   ├── projects/                # 项目数据文件
│   ├── backups/                 # 备份文件
│   ├── demo/                    # 演示数据
│   └── logs/                    # 日志文件
│
├── 📂 scripts/                    # 脚本文件
│   ├── dev.sh                   # 开发环境脚本
│   ├── dev.bat                  # Windows开发脚本
│   ├── build.sh                 # 构建脚本
│   └── deploy.sh                # 部署脚本
│
├── 📂 tests/                      # 端到端测试
│   ├── e2e/                     # E2E测试
│   ├── integration/             # 集成测试
│   └── performance/             # 性能测试
│
├── 📂 opensource/                 # 外部参考资源（只读）
│   ├── SysML-v2-*/              # SysML官方仓库
│   ├── syson/                   # Eclipse SysON
│   └── README-参考资源.md        # 资源说明
│
├── 📂 config/                     # 配置文件
│   ├── docker/                  # Docker配置
│   ├── nginx/                   # Nginx配置
│   └── ci-cd/                   # CI/CD配置
│
├── 📄 README.md                   # 项目说明
├── 📄 PROJECT-STRUCTURE.md        # 本文档
├── 📄 CLAUDE.md                   # Claude AI指导文档
├── 📄 .gitignore                  # Git忽略配置
└── 📄 .editorconfig              # 编辑器配置
```

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

### 临时文档（temp/）
- **临时存放**：草稿、临时笔记
- **定期清理**：Sprint结束后清理
- **不纳入版本控制**

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

## 📋 文档模板位置

- 需求模板：`docs/templates/requirement-template.md`
- 设计模板：`docs/templates/design-template.md`
- 测试模板：`docs/templates/test-template.md`
- 会议模板：`docs/templates/meeting-template.md`

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

## 🗂️ 文档归档策略

- **月度归档**：每月末归档过期文档
- **版本归档**：每个版本发布归档
- **归档位置**：`archive/YYYY-MM/`
- **保留期限**：归档文档保留1年

---

**最后更新**: 2025-01-15
**维护者**: Development Team
**版本**: 1.0