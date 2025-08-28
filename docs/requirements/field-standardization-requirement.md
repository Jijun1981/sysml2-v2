# ysML 2.0字段标准化修复需求书

## 文档信息

- **版本**: 1.0
- **日期**: 2025-08-28
- **优先级**: 高（阻塞Phase 1开发）
- **依赖**: MVP架构设计文档v2.0、SysML字段理解统一文档v1.0

---

## 1. 需求背景

### 1.1 问题现状

经过对回归测试的分析，发现当前MVP存在关键的字段不一致问题：

1. **破坏性问题**：`subject`字段已删除但测试仍在使用（2个测试失败）
2. **标准兼容问题**：`of`字段应为 `requirementDefinition`（15处不兼容）
3. **架构一致性问题**：字段使用未与现有动态EMF架构充分对齐

### 1.2 架构对齐要求

基于现有MVP架构（动态EMF + setAttributeIfExists机制），需要确保：

- ✅ 与 `UniversalElementService`通用接口架构兼容
- ✅ 与 `PilotEMFService.setAttributeIfExists()`机制兼容
- ✅ 与 `ElementDTO.setProperty()`动态属性机制兼容
- ✅ 与182个SysML类型的通用处理方式兼容

---

## 2. 功能需求

### REQ-FS-1: 字段分类标准化

**需求描述**：建立清晰的字段分类体系，确保语义字段与治理字段分离

**验收标准**：

- AC1：所有M2核心字段（elementId、declaredName、documentation等）必须通过 `setAttributeIfExists`正确设置
- AC2：所有治理字段必须通过Metadata机制存储，不能与语义字段混合
- AC3：字段分类文档必须与实际实现保持一致

**优先级**：高

### REQ-FS-2: 标准字段映射修复

**需求描述**：修复与SysML 2.0标准不兼容的字段映射

**验收标准**：

- AC1：所有 `of`字段引用必须改为 `requirementDefinition`
- AC2：删除已弃用的 `subject`字段相关代码和测试
- AC3：确保RequirementUsage正确引用RequirementDefinition

**影响范围**：

- RequirementServiceTest（2个测试用例）
- RequirementService业务逻辑
- 其他测试文件中的15处 `of`字段使用

**优先级**：紧急

### REQ-FS-3: 动态EMF兼容性保证

**需求描述**：确保字段使用与现有动态EMF架构完全兼容

**验收标准**：

- AC1：所有字段设置必须通过 `PilotEMFService.setAttributeIfExists()`
- AC2：字段不存在时必须优雅降级（静默忽略），不能抛出异常
- AC3：所有DTO转换必须通过 `UniversalElementService.convertToDTO()`

**技术约束**：

- 必须与现有的182个SysML类型通用处理机制兼容
- 必须支持EMF JSON序列化/反序列化
- 必须保持与FileModelRepository的兼容性

**优先级**：高

### REQ-FS-4: Metadata机制实现

**需求描述**：基于SysML 2.0原生Metadata机制实现治理字段管理

**验收标准**：

- AC1：实现 `MetadataDefinition`和 `MetadataUsage`的创建和管理
- AC2：支持核心治理字段：status、priority、owner、source、verificationMethod
- AC3：Metadata必须能够通过EMF JSON正确序列化和反序列化
- AC4：支持元素级别的Metadata查询和批量操作

**技术要求**：

- 利用已加载的SysML Pilot模型中的Metadata相关类
- 与 `ownedMetadata`字段正确集成
- 支持结构化的Metadata定义和验证

**优先级**：中

### REQ-FS-5: 回归测试完整性保证

**需求描述**：确保所有现有功能在字段标准化后仍然正常工作

**验收标准**：

- AC1：现有51个测试用例必须全部通过（修复破坏性变更后）
- AC2：API接口行为不能有破坏性变更
- AC3：EMF JSON序列化格式保持向后兼容

**测试覆盖**：

- RequirementDefinition CRUD操作
- RequirementUsage CRUD操作
- Trace关系管理
- 数据验证规则
- 导入导出功能

**优先级**：高

---

## 3. 非功能需求

### REQ-NFS-1: 性能要求

- 字段设置操作响应时间不得超过现有基准的110%
- Metadata查询操作不得显著影响整体API性能

### REQ-NFS-2: 可维护性要求

- 字段分类和使用规则必须有清晰的文档说明
- 新字段添加必须有标准化的流程和验证机制

### REQ-NFS-3: 兼容性要求

- 必须与现有MVP架构100%兼容
- 不得引入新的技术栈或框架依赖
- 必须保持与SysML 2.0 Pilot实现的兼容性

---

## 4. 实施约束

### 4.1 技术约束

- **EMF框架约束**：必须使用现有的Eclipse EMF基础设施
- **动态类型约束**：必须支持182个SysML类型的通用处理
- **序列化约束**：必须保持EMF JSON格式兼容

### 4.2 时间约束

- Phase 1：紧急修复（48小时内完成）- subject字段删除、of字段修正
- Phase 2：标准化实现（1周内完成）- Metadata机制、文档更新
- Phase 3：验证和优化（2周内完成）- 完整测试、性能验证

### 4.3 质量约束

- 代码覆盖率不得低于现有基准
- 所有API端点必须有对应的测试用例
- 必须通过完整的回归测试

---

## 5. 验收测试计划

### 5.1 单元测试

```java
// 字段设置兼容性测试
@Test
public void testFieldSettingCompatibility() {
    // 验证setAttributeIfExists对标准字段的处理
    // 验证Metadata字段的正确存储
    // 验证不存在字段的优雅降级
}

// 标准字段映射测试  
@Test
public void testStandardFieldMapping() {
    // 验证requirementDefinition字段正确性
    // 验证废弃字段已删除
    // 验证Usage->Definition引用正确
}
```

### 5.2 集成测试

```java
// EMF序列化兼容性测试
@Test
public void testEMFSerializationCompatibility() {
    // 验证Metadata能够正确序列化到JSON
    // 验证反序列化后字段完整性
    // 验证与现有数据格式兼容性
}

// API兼容性测试
@Test  
public void testAPICompatibility() {
    // 验证现有API行为不变
    // 验证新Metadata API正确工作
    // 验证错误处理机制
}
```

### 5.3 系统测试

- **完整CRUD流程测试**：创建、查询、更新、删除操作的端到端验证
- **三视图数据一致性测试**：树视图、表视图、图视图的数据同步验证
- **导入导出完整性测试**：数据导入导出后的字段完整性验证

---

## 6. 成功标准

### 6.1 必须达成（Go/No-Go）

- ✅ 51个回归测试全部通过
- ✅ subject字段相关错误完全修复
- ✅ of→requirementDefinition映射100%正确
- ✅ 与现有MVP架构100%兼容

### 6.2 质量目标

- ✅ 字段分类清晰，无语义/治理混合
- ✅ Metadata机制工作正常，支持核心治理需求
- ✅ 文档完整，团队理解统一
- ✅ 代码质量不降低，可维护性提升

### 6.3 长期价值

- ✅ 为Phase 1 TDD开发提供稳定基础
- ✅ 为未来SysML扩展奠定标准化基础
- ✅ 提升与其他SysML工具的互操作性
- ✅ 建立技术债务管控机制

---

## 7. 风险评估

### 7.1 技术风险

| 风险               | 影响 | 缓解措施                   |
| ------------------ | ---- | -------------------------- |
| Metadata机制复杂性 | 中   | 分阶段实施，先简化版本     |
| EMF序列化兼容性    | 高   | 充分测试，保留回滚方案     |
| 测试用例修复工作量 | 中   | 优先修复关键测试，批量处理 |

### 7.2 进度风险

| 风险                   | 影响 | 缓解措施              |
| ---------------------- | ---- | --------------------- |
| 回归测试修复时间超预期 | 中   | 并行处理，自动化验证  |
| Metadata实现复杂度高   | 低   | MVP版本简化，后续迭代 |

---

## 8. 交付物

### 8.1 代码交付

- [ ] 修复后的RequirementServiceTest
- [ ] 更新的RequirementService业务逻辑
- [ ] 基础MetadataService实现
- [ ] 字段验证和转换工具类

### 8.2 文档交付

- [X] SysML 2.0字段理解统一文档
- [ ] 字段标准化实施指南
- [ ] 测试用例修复报告
- [ ] API变更说明（如有）

### 8.3 测试交付

- [ ] 更新的单元测试套件
- [ ] 字段标准化集成测试
- [ ] 回归测试验证报告
- [ ] 性能基准对比报告

此需求的成功实施将为Phase 1 TDD开发提供坚实、标准化的基础设施支撑。
