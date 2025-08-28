# SysML 2.0字段理解统一文档

## 文档信息

- **版本**: 1.0
- **日期**: 2025-08-28
- **目的**: 统一对SysML 2.0字段分类和使用的理解，为Phase 1开发提供准确指导

---

## 1. 核心理解原则

### 1.1 三层字段结构

```
需求元素
├── 原生语义字段（SysML 2.0 M2核心）     → 参与模型计算、约束、验证
├── 标准库扩展字段（SysML Systems Library） → 工程约定，如reqId
└── Metadata治理字段（项目管理信息）       → 管理、追踪、报表、权限
```

### 1.2 决策原则

**判断字段应该放在哪里**：

```
这个字段会参与约束求解吗？ 
├── 是 → 原生语义字段
└── 否 ↓

这个字段影响模型语义吗？
├── 是 → 原生语义字段  
└── 否 ↓

这个字段是SysML标准定义的吗？
├── 是 → 原生字段
└── 否 ↓

这个字段是项目管理/治理信息吗？
├── 是 → Metadata
└── 否 → 重新评估
```

---

## 2. 字段分类详解

### 2.1 原生语义字段（M2核心）

**定义**：SysML 2.0元模型（M2层）直接定义的核心字段

```sysml
requirement def MyRequirement {
    // === M2核心字段（所有Element都有） ===
    // declaredName: String         // 元素名称（自动生成，无需手动设置）
    // elementId: String            // 唯一标识（自动生成）
    
    // === 需求专用的M2字段（继承自AnnotatingElement等） ===
    doc "需求的自然语言描述（shall语句）"    // documentation属性，存储业务意图
    
    // === 我们建议添加的语义字段 ===
    attribute requiredConstraints : String[*];  // require constraint的文本表示
    attribute assumedConstraints : String[*];   // assume constraint的文本表示
}
```

**关键特点**：
- ✅ 参与SysML语义和约束求解
- ✅ 其他SysML工具必须支持
- ✅ 导入导出时不会丢失
- ✅ EMF `setAttributeIfExists` 能正确处理

### 2.2 标准库字段（Systems Library约定）

**定义**：SysML Systems Library定义的工程约定字段

```sysml
// 来自SysML.sysml标准库
requirement def Requirement {
    attribute reqId : String[0..1] redefines declaredShortName;
    //        ^^^^^                    ^^^^^^^^^^^^^^^^^^
    //        工程约定                   真实的M2字段
}
```

**关键理解**：
- `reqId` **不是M2核心字段**
- 它是对 `declaredShortName` 的重新定义（别名）
- 如果不使用标准库，可以直接用 `declaredShortName` 或放入Metadata

### 2.3 Metadata治理字段

**定义**：使用SysML 2.0原生的MetadataDefinition/MetadataUsage机制存储的治理信息

```sysml
requirement def MyRequirement {
    doc "需求描述"
    
    // === 使用Metadata机制 ===
    metadata Lifecycle {
        status : RequirementStatus;     // draft|approved|implemented|verified|retired
        priority : Priority;            // Low|Medium|High|Critical
    }
    
    metadata Governance {
        owner : String;                 // 负责团队/组件
        source : String;                // 需求来源
        verificationMethod : VMethod;   // Test|Analysis|Inspection|Demo
        reqId : String;                 // 工程编号（如果不用declaredShortName）
    }
}
```

**关键特点**：
- ✅ 不影响SysML语义
- ✅ 用于项目管理和治理
- ✅ 支持结构化定义和验证
- ✅ 可以批量操作和查询
- ✅ SysML 2.0原生支持，标准兼容

---

## 3. 具体字段决策

### 3.1 我们的最终字段方案

```javascript
// RequirementDefinition 字段设计
{
  "elementId": "req-def-001",           // M2核心：自动生成
  "eClass": "RequirementDefinition",   // M2核心：类型标识
  
  // === 原生语义字段 ===
  "declaredName": "FastChargeTime",           // M2核心：需求名称
  "declaredShortName": "BR-001",              // M2核心：短名（可选用作编号）
  "documentation": "10%→80% SOC充电时间不得超过30分钟", // M2核心：doc字段
  
  // === 语义扩展字段（渐进式实现） ===
  "requiredConstraints": [                    // 我们扩展：require constraint文本
    "chargeTime <= 30 [min]"
  ],
  "assumedConstraints": [                     // 我们扩展：assume constraint文本
    "ambient in [20, 30] [degC]",
    "batterySOC >= 10 [percent]"
  ],
  
  // === Metadata治理字段 ===
  "metadata": {
    "Lifecycle": {
      "status": "approved",
      "priority": "High"
    },
    "Governance": {
      "owner": "Battery Team",
      "source": "Customer RFP Section 4.2",
      "verificationMethod": "Test",
      "reqId": "BR-001"                       // 工程编号（与shortName可以不同）
    }
  }
}
```

### 3.2 关系管理（独立存储）

```javascript
// 统一关系表（不存储在元素内部）
{
  "id": "rel-001",
  "type": "derive",                 // derive|satisfy|refine|verify|trace
  "from": "req-def-001",           // 源元素ID
  "to": "req-def-002",             // 目标元素ID
  "attributes": {                   // 关系属性（可选）
    "rationale": "细化充电时间要求",
    "confidence": 0.9,
    "reviewed": true
  }
}
```

---

## 4. 与现有代码的对应关系

### 4.1 当前问题

```java
// ❌ 当前有问题的字段使用
// 1. subject字段：已删除但测试仍在使用
// 2. "of"字段：应该是"requirementDefinition"
// 3. priority等：放在DTO中，导入导出时丢失

// ✅ 正确的使用方式
pilotService.setAttributeIfExists(requirement, "declaredName", "FastChargeTime");
pilotService.setAttributeIfExists(requirement, "declaredShortName", "BR-001");
pilotService.setAttributeIfExists(requirement, "documentation", "需求描述");

// Metadata使用（利用SysML 2.0原生机制）
MetadataUsage priorityMeta = createMetadataUsage("Priority", Map.of("level", "High"));
requirement.ownedMetadata.add(priorityMeta);
```

### 4.2 EMF中的对应

```java
// 验证字段存在性
EClass reqDefClass = sysmlPackage.getRequirementDefinition();
EStructuralFeature nameFeature = reqDefClass.getEStructuralFeature("declaredName");     // ✅ 存在
EStructuralFeature docFeature = reqDefClass.getEStructuralFeature("documentation");    // ✅ 存在  
EStructuralFeature shortFeature = reqDefClass.getEStructuralFeature("declaredShortName"); // ✅ 存在
EStructuralFeature metaFeature = reqDefClass.getEStructuralFeature("ownedMetadata");   // ✅ 存在

EStructuralFeature reqIdFeature = reqDefClass.getEStructuralFeature("reqId");          // ❌ 不存在（标准库定义）
EStructuralFeature priorityFeature = reqDefClass.getEStructuralFeature("priority");   // ❌ 不存在（应该用Metadata）
```

---

## 5. 实施指导

### 5.1 立即修复

1. **删除subject字段相关测试**：字段已删除，测试必须同步
2. **修正"of"→"requirementDefinition"**：确保Usage正确引用Definition
3. **验证基础字段使用**：确保使用M2核心字段

### 5.2 短期实现

1. **实现基础Metadata机制**：
   - MetadataService
   - 基础的Priority、Status等定义
   - 批量操作支持

2. **前端适配**：
   - Metadata编辑面板
   - 基于metadata的过滤
   - 批量治理操作

### 5.3 长期优化

1. **约束形式化**：将文本约束转换为可计算的表达式
2. **完整Metadata库**：建立项目级的标准Metadata定义
3. **高级查询**：支持复杂的metadata查询和报表

---

## 6. 总结

### 核心理念

**语义 vs 治理 分离**：
- **语义字段**：影响模型行为，参与约束求解，SysML工具间互操作
- **治理字段**：项目管理信息，不影响模型语义，支持筛选和报表

**标准优先**：
- 优先使用SysML 2.0 M2核心字段
- 充分利用原生Metadata机制
- 避免自创字段导致的兼容性问题

**渐进实施**：
- 先文本形式存储约束，后续可形式化
- 先基础Metadata，逐步丰富治理能力
- 保持现有功能不中断

这个理解确保我们：
- ✅ 100% SysML 2.0标准兼容
- ✅ 充分利用已有的EMF能力
- ✅ 为后续扩展奠定坚实基础
- ✅ 避免技术债务积累