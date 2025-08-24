# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

这是一个**SysML v2建模平台MVP项目**，采用Spring Boot后端 + React前端的单体架构。项目目标是实现一个支持需求定义、需求使用和追溯关系管理的建模平台，提供树视图、表视图和图视图三种联动视图。

## Architecture

### Technology Stack
- **Backend**: Spring Boot 3.2 + Java 17
- **Frontend**: React 18 + TypeScript + React Flow 11 + Ant Design 5
- **Data Layer**: EMF (Eclipse Modeling Framework) + JSON文件存储
- **Graph Visualization**: React Flow for dependency visualization

### Core Components
- **EMF Model Registry**: 从SysML Pilot项目注册标准元模型，包含3个核心类（RequirementDefinition, RequirementUsage, Trace），保持Pilot的完整继承结构
- **File System Storage**: JSON文件存储，每个项目对应一个model.json文件，使用EMF原生JSON格式
- **Three-View Sync**: 树视图、表视图、图视图的数据联动机制

## Project Structure (Expected)

```
backend/
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/     # File system data access
├── model/          # EMF model definitions
├── dto/            # Data transfer objects
└── exception/      # Exception handling

frontend/
├── components/     # UI components (TreeView, TableView, GraphView)
├── services/       # API calls
├── contexts/       # Context API state management
├── utils/          # Utility functions
└── types/          # TypeScript type definitions
```

## Development Commands

**当前项目状态**: 后端基础框架已搭建，EMF模型和CRUD接口部分实现

实际开发命令：
- `mvn clean compile` - 编译后端项目
- `mvn spring-boot:run` - 启动后端服务（端口8080）
- `mvn test` - 运行测试用例
- `curl http://localhost:8080/api/v1/requirements` - 测试REST API
- API基础路径: `/api/v1` (配置在application.yml)

## Data Model

项目核心数据模型包括：

### RequirementDefinition
- 标准化需求定义，包含reqId（唯一）、name、text、tags等字段

### RequirementUsage  
- 基于Definition的实例化使用，关联具体实现场景

### Trace
- 追溯关系，支持derive、satisfy、refine、trace四种类型

## Key Design Principles

- **Single Source of Truth (SSOT)**: EMF Resource作为唯一数据源
- **Pilot Metamodel Registration**: 从SysML Pilot项目注册标准元模型，使用相同的命名空间和EClass结构
- **Performance Targets**: 支持≤500节点模型，API响应<500ms
- **Validation Rules**: 实现3个核心校验规则（reqId唯一性、循环依赖、悬挂引用）

## API Conventions

- **Base URL**: `/api/v1`
- **Timestamps**: ISO-8601 UTC format
- **Pagination**: page从0开始，size默认50，最大200
- **ID Stability**: 导出/导入后ID保持不变
- **JSON Schema**: EMF JSON格式，循环引用使用$ref

## Development Notes

1. **EMF Integration**: 项目使用Eclipse Modeling Framework进行模型定义和序列化
2. **Pilot Metamodel**: 从SysML Pilot项目注册元模型到EMF Registry，使用标准命名空间https://www.omg.org/spec/SysML/20240201
3. **JSON File Storage**: 使用本地JSON文件存储模型数据，文件路径：data/projects/{projectId}/model.json
4. **Static Validation**: 硬编码实现3条核心校验规则，不采用动态规则引擎
5. **File Import/Export**: 支持JSON格式的项目导入导出，文件名格式：project-{pid}.json
6. **File System Structure**: data/projects/{projectId}/目录结构，包含model.json和metadata.json

## MVP Limitations

当前MVP版本明确不实现的功能：
- 分布式架构和微服务
- 复杂权限和多租户
- 实时协作和WebSocket
- 完整SysML v2元模型支持
- 数据库依赖（PostgreSQL等）
- 性能优化（索引、缓存）
- 大规模并发访问（建议≤5用户并发）

## Technical Decisions & Issues

### 2025-08-23 开发记录

1. **EMF集成方案**：
   - 从Pilot项目注册标准 `https://www.omg.org/spec/SysML/20240201` 命名空间
   - 创建自定义 `SysMLResourceFactory` 处理所有URI协议
   - 注册Pilot完整的EClass继承层次：Element→NamedElement→Feature→Type→Classifier→Definition→RequirementDefinition

2. **JSON序列化问题及解决**：
   - **问题**：EMF Jackson库与Spring Boot Jackson版本冲突导致循环引用
   - **参考方案**：研究Syson项目，发现其使用Sirius EMF JSON库
   - **当前状态**：实现自定义ResourceFactory绕过URN协议URL问题
   - **待解决**：JsonResource序列化时的ResourceSet循环引用

3. **已实现功能**：
   - ✅ REQ-C1-1: POST创建RequirementDefinition（含必填字段验证）
   - ✅ REQ-C1-2: GET/PUT/DELETE操作基本实现
   - ✅ REQ-C1-3: reqId唯一性验证（409冲突响应）
   - ✅ 全局异常处理器
   - ✅ 完整的测试用例编写

4. **关键代码文件**：
   - `EMFModelRegistry.java` - EPackage注册
   - `SysMLResourceFactory.java` - 统一的资源工厂
   - `FileModelRepository.java` - 文件系统持久化
   - `RequirementService.java` - 业务逻辑实现
   - `RequirementControllerTest.java` - 完整测试覆盖

5. **重要经验教训**：
   - 不要绕过问题，要深入理解并解决根本原因
   - 参考开源项目（如Syson）的成熟实现方案
   - EMF与Spring Boot集成需要特别处理Jackson版本兼容性