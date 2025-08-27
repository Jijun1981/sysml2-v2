# MVP v1.0 Release Notes
发布日期：2025-08-27

## 版本概述
SysML v2 建模平台MVP版本，实现了核心需求建模功能。

## 功能清单

### ✅ 已完成功能（93.3%）
1. **需求定义管理** (REQ-C1)
   - RequirementDefinition CRUD操作
   - RequirementUsage创建与管理
   - reqId唯一性验证

2. **追溯关系管理** (REQ-C2)
   - 四种关系类型：Satisfy、Derive、Refine、Trace
   - 关系创建与查询

3. **三视图展示** (REQ-D)
   - 树视图：层次结构展示
   - 表视图：数据表格（编辑功能待完善）
   - 图视图：依赖关系可视化

4. **文件导入导出** (REQ-B3)
   - JSON格式项目导入导出
   - EMF原生JSON序列化

### ⚠️ 待完善功能（6.7%）
1. **前端CRUD表单**
   - 创建/编辑对话框
   - 字段验证UI

## 技术架构
- **后端**：Spring Boot 3.2 + Java 17 + EMF
- **前端**：React 18 + TypeScript + React Flow
- **存储**：JSON文件系统

## 已知限制
- 最大支持500个节点
- 并发用户≤5
- 无实时协作功能

## 下一版本预告
v1.1 将增加需求模板功能，支持模板定义与实例化。