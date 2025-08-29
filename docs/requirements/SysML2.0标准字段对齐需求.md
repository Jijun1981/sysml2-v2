# SysML 2.0 标准字段对齐需求文档

## 需求概述

**需求ID**: REQ-FIELD-ALIGN-001  
**需求名称**: SysML 2.0 元模型标准字段对齐  
**优先级**: 高  
**类型**: 技术债务修复 + 标准合规性  

## 背景与问题

### 当前问题
1. **RequirementUsage 字段不标准**: 使用 `"of"` 字段而非标准的 `"requirementDefinition"`
2. **Dependency 字段不标准**: 使用 `"fromId"`/`"toId"` 而非标准的 `"client"`/`"supplier"`  
3. **Dependency 类型处理不标准**: 使用通用 `Dependency` + `type` 属性，而非具体的 EClass 类型

### 标准要求
基于 SysML 2.0 规范和已注册的 EMF 元模型，需要对齐以下标准：
- RequirementUsage → RequirementDefinition 引用使用 `requirementDefinition` 字段
- Dependency 关系使用 `client`/`supplier` 字段指向关系两端
- 依赖关系类型通过具体 EClass 体现（如 `sysml:DeriveRequirement`）

## 功能需求

### REQ-FIELD-001: RequirementUsage 字段标准化
**描述**: RequirementUsage 必须使用标准的 `requirementDefinition` 字段关联 RequirementDefinition

**验收标准**:
- [ ] 数据生成脚本使用 `"requirementDefinition"` 字段，移除 `"of"` 字段
- [ ] 后端 ElementMapper 正确处理 `requirementDefinition` 字段映射
- [ ] 前端可以正确显示 Usage → Definition 的关联关系
- [ ] API 测试验证字段名称正确性

**示例**:
```json
// 修正前
{"eClass": "sysml:RequirementUsage", "data": {"of": "DEF-001"}}

// 修正后  
{"eClass": "sysml:RequirementUsage", "data": {"requirementDefinition": "DEF-001"}}
```

### REQ-FIELD-002: Dependency 字段标准化
**描述**: Dependency 关系必须使用标准的 `client`/`supplier` 字段，而非 `fromId`/`toId`

**验收标准**:
- [ ] TraceService 创建 Dependency 时使用 `client`/`supplier` 字段
- [ ] PilotEMFService.setDependencyReferences() 正确设置标准字段
- [ ] 数据生成脚本使用标准字段名
- [ ] 前端 API 调用适配新字段名

**示例**:
```json
// 修正前
{"eClass": "sysml:Dependency", "data": {"fromId": "A", "toId": "B", "type": "derive"}}

// 修正后
{"eClass": "sysml:DeriveRequirement", "data": {"client": "A", "supplier": "B"}}
```

### REQ-FIELD-003: Dependency 类型标准化  
**描述**: 依赖关系类型必须通过具体 EClass 体现，而非 type 属性

**验收标准**:
- [ ] TraceService 直接创建具体 EClass（DeriveRequirement, Satisfy 等）
- [ ] 移除 Dependency 的 `type` 属性字段
- [ ] API 层类型枚举映射到正确的 EClass 名称
- [ ] 数据序列化使用具体的 EClass 类型

**类型映射表**:
| API 枚举 | EClass 类型 |
|----------|------------|
| DERIVE | sysml:DeriveRequirement |
| SATISFY | sysml:Satisfy |
| REFINE | sysml:Refine |
| TRACE | sysml:Trace |

## 非功能需求

### REQ-FIELD-004: 向后兼容性
**描述**: 修改过程中保持数据迁移的平滑性

**验收标准**:
- [ ] ElementMapper 能够处理旧字段名（临时兼容）
- [ ] 数据迁移脚本处理现有数据
- [ ] 前端逐步适配，不影响现有功能

### REQ-FIELD-005: 领域层简化原则
**描述**: 前端领域层保持简单的枚举设计，复杂性在后端处理

**设计原则**:
```typescript
// 前端领域层保持简单
enum DependencyType {
  DERIVE = 'derive',
  SATISFY = 'satisfy', 
  REFINE = 'refine',
  TRACE = 'trace'
}

// 后端负责映射到具体 EClass
TYPE_TO_ECLASS_MAPPING.put("derive", "DeriveRequirement");
```

## 技术实现

### 修改文件清单

**数据生成层**:
- [ ] `/generate-simple-data.js` - 修正字段名

**后端服务层**:
- [ ] `TraceService.java` - 使用标准字段和具体 EClass
- [ ] `PilotEMFService.java` - 完善 setDependencyReferences 实现
- [ ] `ElementMapper.java` - 确认字段映射正确性

**测试验证**:
- [ ] 更新相关测试用例验证标准字段
- [ ] 端到端测试验证 API 正确性

### 实施步骤

1. **Phase 1**: 修正数据生成脚本（低风险）
2. **Phase 2**: 后端服务适配标准字段（中等风险）  
3. **Phase 3**: 移除兼容性代码（低风险）
4. **Phase 4**: 全面测试验证（必需）

## 验收测试

### 测试用例 TC-001: RequirementUsage 字段验证
```bash
# 创建 RequirementUsage
curl -X POST /api/v1/requirements/usages \
  -d '{"requirementDefinition": "DEF-001", "declaredName": "测试需求"}'

# 验证返回数据包含正确字段名
```

### 测试用例 TC-002: Dependency 创建验证
```bash
# 创建派生关系
curl -X POST /api/v1/traces \
  -d '{"fromId": "REQ-A", "toId": "REQ-B", "type": "derive"}'

# 验证创建的是 DeriveRequirement 类型，包含 client/supplier 字段
```

## 影响评估

**正面影响**:
- ✅ 与 SysML 2.0 标准完全对齐
- ✅ 为未来 EMF 模型扩展奠定基础
- ✅ 提高数据模型的语义精确性

**风险控制**:
- ⚠️ 需要协调前后端字段名变更
- ⚠️ 现有数据需要迁移处理
- ⚠️ 测试覆盖需要全面更新

## 交付标准

**完成定义**:
1. 所有新生成的数据使用标准字段名
2. API 接口正确处理标准字段
3. 前端显示正常，无功能回归
4. 测试用例全部通过
5. 代码 Review 通过

**质量指标**:
- 单元测试覆盖率 ≥ 90%
- 集成测试通过率 = 100%
- 字段名称 100% 符合 SysML 2.0 标准

---

**创建人**: Claude  
**创建时间**: 2025-08-29  
**最后更新**: 2025-08-29