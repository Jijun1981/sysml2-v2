# SysML 2.0元模型字段对齐检查

## 当前使用的字段分析

### RequirementDefinition使用的字段
1. `elementId` - 我们自己的UUID标识符 (可能不在元模型中)
2. `declaredShortName` - SysML 2.0标准字段 ✓
3. `declaredName` - SysML 2.0标准字段 ✓
4. `documentation` / `body` / `text` - SysML 2.0标准字段 ✓
5. `createdAt` / `updatedAt` - 我们的时间戳字段 (可能不在元模型中)

### RequirementUsage使用的字段
1. `elementId` - 我们自己的UUID标识符 (可能不在元模型中)
2. `declaredName` - SysML 2.0标准字段 ✓
3. `documentation` / `body` / `text` - SysML 2.0标准字段 ✓
4. `status` - 可能的SysML 2.0字段，需要确认
5. `of` - RequirementUsage引用Definition的关系，需要确认正确字段名
6. `createdAt` / `updatedAt` - 我们的时间戳字段 (可能不在元模型中)
7. ~~`subject`~~ - 已删除，非标准字段

## SysML 2.0 Pilot官方元模型分析结果

通过从已加载的SysML Pilot EMF元模型中获取完整字段信息，确定了官方标准字段：

### 命名空间信息
- **SysML版本**: https://www.omg.org/spec/SysML/20250201
- **加载的EClass数量**: 182个
- **数据来源**: 官方SysML-v2-Pilot-Implementation项目的SysML.ecore文件

### RequirementDefinition官方标准字段分析

**✅ SysML 2.0 Pilot标准字段**（直接来自元模型）：

**自身属性 (2个)**：
- `reqId: String (可选)` - **这是SysML 2.0标准字段！**
- `text: String (可选)` - **这是SysML 2.0标准字段！**

**继承的关键属性**（来自Element等父类）：
- `elementId: String` - 来自Element，SysML标准字段 
- `declaredName: String` - 来自Element，SysML标准字段
- `declaredShortName: String` - 来自Element，SysML标准字段
- `documentation: Documentation[]` - 来自Element，SysML标准字段（注意是引用类型）

**特殊需求字段**（来自RequirementDefinition）：
- `subjectParameter: Usage[]` - 需求主体参数
- `actorParameter: PartUsage[]` - 参与者参数
- `stakeholderParameter: PartUsage[]` - 干系人参数
- `assumedConstraint: ConstraintUsage[]` - 假设约束
- `requiredConstraint: ConstraintUsage[]` - 必需约束
- `framedConcern: ConcernUsage[]` - 框定关注点

**❌ 我们添加的非标准字段**：
- `priority, verificationMethod, category, source, riskLevel` - 需求工程扩展字段
- `renderedText` - 参数化文本渲染结果
- `status` - 在Definition中不是标准字段
- `createdAt, updatedAt` - 时间戳字段

### RequirementUsage官方标准字段分析

**✅ SysML 2.0 Pilot标准字段**：

**自身属性 (2个)**：
- `reqId: String (可选)` - SysML 2.0标准字段
- `text: String (可选)` - SysML 2.0标准字段

**关键引用字段**（来自RequirementUsage）：
- `requirementDefinition: RequirementDefinition` - **这是Usage引用Definition的正确方式！**
- `requiredConstraint: ConstraintUsage[]`
- `assumedConstraint: ConstraintUsage[]`
- `subjectParameter: Usage[]`
- `actorParameter: PartUsage[]`
- `stakeholderParameter: PartUsage[]`
- `framedConcern: ConcernUsage[]`

**继承的通用字段**：
- `elementId, declaredName, declaredShortName, documentation` - 来自Element
- `definition: Classifier` - 来自Usage（通用引用机制）

**❌ 我们添加的非标准字段**：
- 同RequirementDefinition的非标准字段

### 重要发现与纠正

**🎉 好消息 - reqId和text是标准字段！**：
1. **reqId**: 原以为是我们自定义的，实际上是SysML 2.0标准字段！
2. **text**: 原以为有问题，实际上也是SysML 2.0标准字段！
3. **RequirementUsage引用方式**: 应该使用`requirementDefinition`字段，不是我们之前的`of`字段

**⚠️ 需要修正的问题**：
1. **subject字段删除正确**: 确实不是标准字段，我们删除是对的
2. **Usage->Definition引用**: 当前使用`of`字段不标准，应改为`requirementDefinition`
3. **text字段类型问题**: EMF元模型中`text`是String类型，但我们的`setAttributeIfExists`提示类型不匹配

**❌ 确认的非标准扩展字段**：
- `priority, verificationMethod, category, source, riskLevel, renderedText`
- `status` - 在Definition层面不是标准字段
- `createdAt, updatedAt` - 时间戳扩展字段

## 修正行动计划

基于SysML 2.0 Pilot官方元模型分析，需要进行以下修正：

### 1. 保留和修正标准字段

**✅ 确认保留的SysML 2.0标准字段**：
```java
// SysML 2.0 Pilot官方标准字段 - 保留
private String elementId;        // 来自Element
private String reqId;           // ✅ 这是标准字段！来自RequirementDefinition/Usage
private String declaredName;    // 来自Element
private String declaredShortName; // 来自Element  
private String text;            // ✅ 这也是标准字段！来自RequirementDefinition/Usage
private String documentation;   // 来自Element（但类型是Documentation[]引用）
```

**🔧 需要修正的字段映射**：
- `of` → 改为 `requirementDefinition` (Usage引用Definition的正确字段)
- `documentation` 处理方式需要修正（引用类型vs字符串类型）

### 2. 移除确认的非标准字段

**❌ 需要移除的非标准扩展字段**：
```java
// 需求工程扩展字段 - 建议移除或标记为扩展
private String priority;         // 非标准字段
private String verificationMethod; // 非标准字段
private String category;         // 非标准字段
private String source;           // 非标准字段
private String riskLevel;        // 非标准字段
private String renderedText;     // 非标准字段
private String status;           // 在Definition中非标准
private String createdAt;        // 时间戳扩展
private String updatedAt;        // 时间戳扩展
```

### 3. 利用SysML 2.0标准的高级需求建模能力

**🚀 发现的标准需求建模字段**（我们可能没有充分利用）：
- `subjectParameter: Usage[]` - 需求主体，比简单的subject字段更强大
- `actorParameter: PartUsage[]` - 参与者建模
- `stakeholderParameter: PartUsage[]` - 干系人建模  
- `assumedConstraint: ConstraintUsage[]` - 假设约束
- `requiredConstraint: ConstraintUsage[]` - 必需约束
- `framedConcern: ConcernUsage[]` - 关注点框定

**结论**: SysML 2.0元模型确实"抽象已经非常好了"，提供了比我们当前使用的更丰富的需求建模能力！