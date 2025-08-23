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
- **EMF Model Registry**: 本地SysML2子集元模型，包含3个核心类（RequirementDefinition, RequirementUsage, Trace）
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

**当前项目状态**: 仅有设计文档，尚未实现代码

根据需求文档，预期的开发命令将包括：
- `mvn clean verify` - 构建后端项目
- `npm start` - 启动前端开发服务器
- `java -jar backend.jar` - 运行后端服务（无需数据库）
- Maven构建脚本待实现

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
- **Zero Pilot Dependency**: 不依赖SysML Pilot实现，使用本地元模型
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
2. **No External Dependencies**: 明确声明不依赖org.omg.sysml或Pilot实现，也不依赖PostgreSQL等外部数据库
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