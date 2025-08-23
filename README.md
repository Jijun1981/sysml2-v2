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
mvn test                    # 运行测试
mvn package                 # 打包
java -jar target/*.jar      # 运行JAR包
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

## 相关文档

- [需求文档](docs/需求文档.md)
- [架构设计文档](docs/mvp架构设计文档.md)
- [API接口文档](docs/mvp接口文档.md)
- [参考资源清单](opensource/README-参考资源.md)