# SysML 2.0 字段继承完整分析文档

## 文档信息

- **版本**: 1.0
- **日期**: 2025-08-27
- **基于**: SysML-v2-Pilot-Implementation (https://www.omg.org/spec/SysML/20250201)
- **数据来源**: 运行时EMF元模型调试输出（182个EClass）

---

## 1. 完整继承层次结构

基于实际加载的SysML Pilot元模型，RequirementDefinition和RequirementUsage的完整继承层次：

### 1.1 RequirementDefinition 继承链

```
Element (根基类)
  ↓
Namespace
  ↓  
Type
  ↓
Classifier
  ↓
Definition
  ↓
Class
  ↓
OccurrenceDefinition  
  ↓
Behavior
  ↓
Function
  ↓
Predicate
  ↓
ConstraintDefinition
  ↓
RequirementDefinition (最终类)
```

### 1.2 RequirementUsage 继承链

```
Element (根基类)
  ↓
Namespace
  ↓
Type
  ↓
Feature
  ↓
Usage
  ↓
OccurrenceUsage
  ↓
Step
  ↓
Expression
  ↓
BooleanExpression
  ↓
ConstraintUsage
  ↓
RequirementUsage (最终类)
```

---

## 2. 各层级字段详细分析

### 2.1 Element (根基类) - 18个基础字段

**所有SysML元素的根基类，提供最基本的元素特征**

| 字段名                    | 类型                    | 说明                     | 我们是否使用         |
| ------------------------- | ----------------------- | ------------------------ | -------------------- |
| `owningMembership`      | OwningMembership        | 拥有的成员关系           | ❌ 基础关系          |
| `ownedRelationship`     | Relationship[]          | 拥有的关系               | ❌ 基础关系          |
| `owningRelationship`    | Relationship            | 所属关系                 | ❌ 基础关系          |
| `owningNamespace`       | Namespace               | 所属命名空间             | ❌ 基础关系          |
| `elementId`             | String                  | **元素唯一标识符** | ✅**核心字段** |
| `owner`                 | Element                 | 拥有者元素               | ❌ 基础关系          |
| `ownedElement`          | Element[]               | 拥有的元素               | ❌ 基础关系          |
| `documentation`         | Documentation[]         | **文档说明**       | ✅**核心字段** |
| `ownedAnnotation`       | Annotation[]            | 拥有的注解               | ❌ 基础关系          |
| `textualRepresentation` | TextualRepresentation[] | 文本表示                 | ❌ 基础关系          |
| `aliasIds`              | String[]                | 别名标识符               | ❌ 扩展字段          |
| `declaredShortName`     | String                  | **声明的短名称**   | ✅**核心字段** |
| `declaredName`          | String                  | **声明的名称**     | ✅**核心字段** |
| `shortName`             | String                  | 短名称（派生）           | ❌ 派生字段          |
| `name`                  | String                  | 名称（派生）             | ❌ 派生字段          |
| `qualifiedName`         | String                  | 限定名称（派生）         | ❌ 派生字段          |
| `isImpliedIncluded`     | Boolean                 | 是否隐含包含             | ❌ 高级特性          |
| `isLibraryElement`      | Boolean                 | 是否库元素               | ❌ 高级特性          |

**⭐ 核心发现**: 我们使用的基础字段(`elementId`, `declaredName`, `declaredShortName`, `documentation`)都是Element层级的标准字段！

### 2.2 Namespace (命名空间) - 6个字段

**提供命名空间和成员管理能力**

| 字段名                 | 类型         | 说明           | 我们是否使用 |
| ---------------------- | ------------ | -------------- | ------------ |
| `ownedMembership`    | Membership[] | 拥有的成员关系 | ❌ 高级关系  |
| `ownedMember`        | Element[]    | 拥有的成员     | ❌ 高级关系  |
| `membership`         | Membership[] | 成员关系       | ❌ 高级关系  |
| `ownedImport`        | Import[]     | 拥有的导入     | ❌ 高级关系  |
| `member`             | Element[]    | 成员           | ❌ 高级关系  |
| `importedMembership` | Membership[] | 导入的成员关系 | ❌ 高级关系  |

### 2.3 Type (类型) - 23个字段

**提供类型化、特征化和特化能力**

| 字段名                     | 类型                | 说明               | 我们是否使用 |
| -------------------------- | ------------------- | ------------------ | ------------ |
| `ownedSpecialization`    | Specialization[]    | 拥有的特化关系     | ❌ 高级关系  |
| `ownedFeatureMembership` | FeatureMembership[] | 拥有的特征成员关系 | ❌ 高级关系  |
| `feature`                | Feature[]           | 特征               | ❌ 高级关系  |
| `ownedFeature`           | Feature[]           | 拥有的特征         | ❌ 高级关系  |
| `input`                  | Feature[]           | 输入特征           | ❌ 高级关系  |
| `output`                 | Feature[]           | 输出特征           | ❌ 高级关系  |
| `isAbstract`             | Boolean             | 是否抽象           | ❌ 高级特性  |
| `inheritedMembership`    | Membership[]        | 继承的成员关系     | ❌ 高级关系  |
| `endFeature`             | Feature[]           | 端特征             | ❌ 高级关系  |
| `ownedEndFeature`        | Feature[]           | 拥有的端特征       | ❌ 高级关系  |
| `isSufficient`           | Boolean             | 是否充分           | ❌ 高级特性  |
| `ownedConjugator`        | Conjugation[]       | 拥有的共轭关系     | ❌ 高级关系  |
| `isConjugated`           | Boolean             | 是否共轭           | ❌ 高级特性  |
| `inheritedFeature`       | Feature[]           | 继承的特征         | ❌ 高级关系  |
| `multiplicity`           | Multiplicity        | 多重性             | ❌ 高级特性  |
| `unioningType`           | Type[]              | 联合类型           | ❌ 高级关系  |
| `ownedIntersecting`      | Intersecting[]      | 拥有的交集关系     | ❌ 高级关系  |
| `intersectingType`       | Type[]              | 交集类型           | ❌ 高级关系  |
| `ownedUnioning`          | Unioning[]          | 拥有的联合关系     | ❌ 高级关系  |
| `ownedDisjoining`        | Disjoining[]        | 拥有的分离关系     | ❌ 高级关系  |
| `featureMembership`      | FeatureMembership[] | 特征成员关系       | ❌ 高级关系  |
| `differencingType`       | Type[]              | 差异类型           | ❌ 高级关系  |
| `ownedDifferencing`      | Differencing[]      | 拥有的差异关系     | ❌ 高级关系  |
| `directedFeature`        | Feature[]           | 有向特征           | ❌ 高级关系  |

### 2.4 Definition (定义) - 18个字段

**提供定义和变体管理能力**

| 字段名                    | 类型                    | 说明                     | 我们是否使用         |
| ------------------------- | ----------------------- | ------------------------ | -------------------- |
| `isVariation`           | Boolean                 | 是否变体                 | ❌ 高级特性          |
| `variant`               | Usage[]                 | 变体                     | ❌ 高级关系          |
| `variantMembership`     | VariantMembership[]     | 变体成员关系             | ❌ 高级关系          |
| `usage`                 | Usage[]                 | 使用                     | ❌ 高级关系          |
| `directedUsage`         | Usage[]                 | 有向使用                 | ❌ 高级关系          |
| `ownedReference`        | ReferenceUsage[]        | 拥有的引用使用           | ❌ 高级关系          |
| `ownedAttribute`        | AttributeUsage[]        | 拥有的属性使用           | ❌ 高级关系          |
| `ownedEnumeration`      | EnumerationUsage[]      | 拥有的枚举使用           | ❌ 高级关系          |
| `ownedOccurrence`       | OccurrenceUsage[]       | 拥有的发生使用           | ❌ 高级关系          |
| `ownedItem`             | ItemUsage[]             | 拥有的项使用             | ❌ 高级关系          |
| `ownedPart`             | PartUsage[]             | 拥有的部件使用           | ❌ 高级关系          |
| `ownedPort`             | PortUsage[]             | 拥有的端口使用           | ❌ 高级关系          |
| `ownedConnection`       | ConnectorAsUsage[]      | 拥有的连接使用           | ❌ 高级关系          |
| `ownedFlow`             | FlowUsage[]             | 拥有的流使用             | ❌ 高级关系          |
| `ownedInterface`        | InterfaceUsage[]        | 拥有的接口使用           | ❌ 高级关系          |
| `ownedAllocation`       | AllocationUsage[]       | 拥有的分配使用           | ❌ 高级关系          |
| `ownedAction`           | ActionUsage[]           | 拥有的动作使用           | ❌ 高级关系          |
| `ownedState`            | StateUsage[]            | 拥有的状态使用           | ❌ 高级关系          |
| `ownedTransition`       | TransitionUsage[]       | 拥有的转换使用           | ❌ 高级关系          |
| `ownedCalculation`      | CalculationUsage[]      | 拥有的计算使用           | ❌ 高级关系          |
| `ownedConstraint`       | ConstraintUsage[]       | 拥有的约束使用           | ❌ 高级关系          |
| `ownedRequirement`      | RequirementUsage[]      | **拥有的需求使用** | ✅**重要关系** |
| `ownedConcern`          | ConcernUsage[]          | 拥有的关注点使用         | ❌ 高级关系          |
| `ownedCase`             | CaseUsage[]             | 拥有的用例使用           | ❌ 高级关系          |
| `ownedAnalysisCase`     | AnalysisCaseUsage[]     | 拥有的分析用例使用       | ❌ 高级关系          |
| `ownedVerificationCase` | VerificationCaseUsage[] | 拥有的验证用例使用       | ❌ 高级关系          |
| `ownedUseCase`          | UseCaseUsage[]          | 拥有的用例使用           | ❌ 高级关系          |
| `ownedView`             | ViewUsage[]             | 拥有的视图使用           | ❌ 高级关系          |
| `ownedViewpoint`        | ViewpointUsage[]        | 拥有的视点使用           | ❌ 高级关系          |
| `ownedRendering`        | RenderingUsage[]        | 拥有的渲染使用           | ❌ 高级关系          |
| `ownedMetadata`         | MetadataUsage[]         | 拥有的元数据使用         | ❌ 高级关系          |
| `ownedUsage`            | Usage[]                 | 拥有的使用               | ❌ 高级关系          |

### 2.5 RequirementDefinition (自身) - 2个字段

**需求定义的核心字段**

| 字段名    | 类型   | 说明                 | 我们是否使用         |
| --------- | ------ | -------------------- | -------------------- |
| `reqId` | String | **需求标识符** | ✅**核心字段** |
| `text`  | String | **需求文本**   | ✅**核心字段** |

### 2.6 RequirementDefinition 专用关系字段 - 6个字段

**需求定义的专门关系**

| 字段名                   | 类型              | 说明                 | 我们是否使用         |
| ------------------------ | ----------------- | -------------------- | -------------------- |
| `subjectParameter`     | Usage[]           | **主体参数**   | ❌**应该使用** |
| `actorParameter`       | PartUsage[]       | **参与者参数** | ❌**应该使用** |
| `stakeholderParameter` | PartUsage[]       | **干系人参数** | ❌**应该使用** |
| `assumedConstraint`    | ConstraintUsage[] | **假设约束**   | ❌**应该使用** |
| `requiredConstraint`   | ConstraintUsage[] | **必需约束**   | ❌**应该使用** |
| `framedConcern`        | ConcernUsage[]    | **框定关注点** | ❌**应该使用** |

---

## 3. RequirementUsage 详细字段分析

### 3.1 Feature 层增加的字段 - 20个字段

**作为特征的基本能力**

| 字段名                       | 类型                  | 说明               | 我们是否使用 |
| ---------------------------- | --------------------- | ------------------ | ------------ |
| `owningFeatureMembership`  | FeatureMembership     | 拥有的特征成员关系 | ❌ 高级关系  |
| `owningType`               | Type                  | 拥有的类型         | ❌ 高级关系  |
| `endOwningType`            | Type                  | 端拥有类型         | ❌ 高级关系  |
| `isUnique`                 | Boolean               | 是否唯一           | ❌ 高级特性  |
| `isOrdered`                | Boolean               | 是否有序           | ❌ 高级特性  |
| `type`                     | Type[]                | 类型               | ❌ 高级关系  |
| `ownedRedefinition`        | Redefinition[]        | 拥有的重定义       | ❌ 高级关系  |
| `ownedSubsetting`          | Subsetting[]          | 拥有的子集关系     | ❌ 高级关系  |
| `isComposite`              | Boolean               | 是否组合           | ❌ 高级特性  |
| `isEnd`                    | Boolean               | 是否端             | ❌ 高级特性  |
| `ownedTyping`              | FeatureTyping[]       | 拥有的类型关系     | ❌ 高级关系  |
| `featuringType`            | Type[]                | 特征类型           | ❌ 高级关系  |
| `ownedTypeFeaturing`       | TypeFeaturing[]       | 拥有的类型特征关系 | ❌ 高级关系  |
| `isDerived`                | Boolean               | 是否派生           | ❌ 高级特性  |
| `chainingFeature`          | Feature[]             | 链式特征           | ❌ 高级关系  |
| `ownedFeatureInverting`    | FeatureInverting[]    | 拥有的特征反转     | ❌ 高级关系  |
| `ownedFeatureChaining`     | FeatureChaining[]     | 拥有的特征链       | ❌ 高级关系  |
| `isPortion`                | Boolean               | 是否部分           | ❌ 高级特性  |
| `isVariable`               | Boolean               | 是否变量           | ❌ 高级特性  |
| `isConstant`               | Boolean               | 是否常量           | ❌ 高级特性  |
| `ownedReferenceSubsetting` | ReferenceSubsetting[] | 拥有的引用子集     | ❌ 高级关系  |
| `featureTarget`            | Feature[]             | 特征目标           | ❌ 高级关系  |
| `crossFeature`             | Feature[]             | 交叉特征           | ❌ 高级关系  |
| `direction`                | FeatureDirectionKind  | 方向               | ❌ 高级特性  |
| `ownedCrossSubsetting`     | CrossSubsetting[]     | 拥有的交叉子集     | ❌ 高级关系  |
| `isNonunique`              | Boolean               | 是否非唯一         | ❌ 高级特性  |

### 3.2 Usage 层增加的字段 - 32个字段

**作为使用的核心能力**

| 字段名                | 类型                | 说明           | 我们是否使用         |
| --------------------- | ------------------- | -------------- | -------------------- |
| `mayTimeVary`       | Boolean             | 可能随时间变化 | ❌ 高级特性          |
| `isReference`       | Boolean             | 是否引用       | ❌ 高级特性          |
| `variant`           | Usage[]             | 变体           | ❌ 高级关系          |
| `variantMembership` | VariantMembership[] | 变体成员关系   | ❌ 高级关系          |
| `owningDefinition`  | Definition          | 拥有的定义     | ❌ 高级关系          |
| `owningUsage`       | Usage               | 拥有的使用     | ❌ 高级关系          |
| `nestedUsage`       | Usage[]             | 嵌套使用       | ❌ 高级关系          |
| `definition`        | Classifier[]        | **定义** | ✅**重要字段** |
| `usage`             | Usage[]             | 使用           | ❌ 高级关系          |
| `directedUsage`     | Usage[]             | 有向使用       | ❌ 高级关系          |
| `isVariation`       | Boolean             | 是否变体       | ❌ 高级特性          |

### 3.3 RequirementUsage 关键引用字段

**需求使用的专门引用**

| 字段名                    | 类型                  | 说明                   | 我们是否使用                 |
| ------------------------- | --------------------- | ---------------------- | ---------------------------- |
| `requirementDefinition` | RequirementDefinition | **需求定义引用** | ❌**应该使用，替代of** |
| `reqId`                 | String                | **需求标识符**   | ✅**核心字段**         |
| `text`                  | String                | **需求文本**     | ✅**核心字段**         |
| `requiredConstraint`    | ConstraintUsage[]     | **必需约束**     | ❌**应该使用**         |
| `assumedConstraint`     | ConstraintUsage[]     | **假设约束**     | ❌**应该使用**         |
| `subjectParameter`      | Usage[]               | **主体参数**     | ❌**应该使用**         |
| `framedConcern`         | ConcernUsage[]        | **框定关注点**   | ❌**应该使用**         |
| `actorParameter`        | PartUsage[]           | **参与者参数**   | ❌**应该使用**         |
| `stakeholderParameter`  | PartUsage[]           | **干系人参数**   | ❌**应该使用**         |

---

## 4. 我们当前字段使用情况总结

### 4.1 正确使用的SysML标准字段 ✅

| 字段名                | 来源层级                    | 用途                 |
| --------------------- | --------------------------- | -------------------- |
| `elementId`         | Element                     | 元素唯一标识         |
| `declaredName`      | Element                     | 元素声明名称         |
| `declaredShortName` | Element                     | 元素短名称           |
| `documentation`     | Element                     | 元素文档（引用类型） |
| `reqId`             | RequirementDefinition/Usage | 需求标识符           |
| `text`              | RequirementDefinition/Usage | 需求文本             |

### 4.2 错误或不当使用的字段 ❌

| 字段名                  | 问题                   | 建议                           |
| ----------------------- | ---------------------- | ------------------------------ |
| `of`                  | 非标准字段名           | 改为 `requirementDefinition` |
| ~~`subject`~~        | 非标准字段             | ✅ 已删除，正确                |
| `priority`            | 非标准扩展             | 移除或标记为扩展               |
| `category`            | 非标准扩展             | 移除或标记为扩展               |
| `verificationMethod`  | 非标准扩展             | 移除或标记为扩展               |
| `source`              | 非标准扩展             | 移除或标记为扩展               |
| `riskLevel`           | 非标准扩展             | 移除或标记为扩展               |
| `renderedText`        | 非标准扩展             | 移除或标记为扩展               |
| `status`              | 在Definition层面非标准 | 移除或标记为扩展               |
| `createdAt/updatedAt` | 时间戳扩展             | 移除或标记为扩展               |

### 4.3 未充分利用的SysML标准字段 🚀

**高价值的需求建模字段，我们应该考虑使用**：

| 字段名                    | 类型                  | 用途     | 优势                        |
| ------------------------- | --------------------- | -------- | --------------------------- |
| `subjectParameter`      | Usage[]               | 需求主体 | 比简单subject字符串更强大   |
| `actorParameter`        | PartUsage[]           | 参与者   | 支持复杂参与者建模          |
| `stakeholderParameter`  | PartUsage[]           | 干系人   | 支持干系人管理              |
| `assumedConstraint`     | ConstraintUsage[]     | 假设约束 | 正式的假设建模              |
| `requiredConstraint`    | ConstraintUsage[]     | 必需约束 | 正式的约束建模              |
| `framedConcern`         | ConcernUsage[]        | 关注点   | 支持关注点分离              |
| `requirementDefinition` | RequirementDefinition | 定义引用 | 标准的Usage->Definition引用 |

---

## 5. 建议的字段对齐行动

### 5.1 立即修正（保持标准兼容）

1. **修正Usage->Definition引用**:

   ```java
   // 当前: 使用of字段
   private String of;

   // 修正: 使用requirementDefinition引用
   private String requirementDefinition; // 或者requirementDefinitionId
   ```
2. **修正documentation字段处理**:

   ```java
   // 当前: 作为String处理
   private String documentation;

   // 修正: 正确处理Documentation[]引用类型
   // 在DTO中仍可以保持String，但EMF层正确处理
   ```

### 5.2 渐进增强（利用SysML高级能力）

考虑逐步引入SysML 2.0的高级需求建模字段，替代我们的简化扩展：

```java
// Phase 2: 引入标准的需求建模字段
private List<String> subjectParameterIds;      // 替代简单的subject字符串
private List<String> actorParameterIds;        // 参与者建模
private List<String> stakeholderParameterIds;  // 干系人建模
private List<String> assumedConstraintIds;     // 假设约束
private List<String> requiredConstraintIds;    // 必需约束
```

### 5.3 扩展字段标记

对于确实需要保留的扩展字段，明确标记：

```java
// 明确标记为扩展字段，不是SysML标准
@JsonProperty("x-priority")      // 扩展字段前缀
private String priority;

@JsonProperty("x-createdAt")     // 时间戳扩展
private String createdAt;
```

---

## 6. 总结

### 6.1 关键发现

1. **reqId和text是官方标准字段** - 我们的担心是多余的
2. **基础字段来自Element层** - elementId, declaredName等都是官方字段
3. **SysML 2.0的需求建模能力很强大** - 提供了比我们当前使用更丰富的建模能力
4. **继承层次正确** - EMF完美处理了11层继承关系

### 6.2 行动优先级

**高优先级**:

- [ ] 修正 `of` → `requirementDefinition`引用
- [ ] 正确处理 `documentation`字段类型

**中优先级**:

- [ ] 移除或标记非标准扩展字段
- [ ] 考虑引入 `subjectParameter`等高级字段

**低优先级**:

- [ ] 利用其他高级SysML建模能力

**结论**: 我们的架构基本正确，主要是一些字段映射细节需要调整。SysML 2.0元模型确实"抽象已经非常好了"！
