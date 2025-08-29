# REQ-TDD-001: RequirementDefinition引用功能

## 需求描述
RequirementUsage对象必须能够正确引用RequirementDefinition对象，且该引用关系在创建、查询、序列化过程中都能正确工作。

## 详细需求

### REQ-TDD-001-1: EMF引用创建
- **given**: 存在RequirementDefinition对象 "DEF-PERF"
- **when**: 创建RequirementUsage并设置 `requirementDefinition: "DEF-PERF"`  
- **then**: 
  - RequirementUsage的requirementDefinition字段应该是EMF对象引用，不是字符串
  - 该引用应该指向正确的RequirementDefinition对象
  - 引用对象的elementId应该是 "DEF-PERF"

### REQ-TDD-001-2: API响应序列化
- **given**: RequirementUsage对象正确引用了RequirementDefinition
- **when**: 通过API查询该RequirementUsage
- **then**:
  - API响应中应该包含 `requirementDefinition` 字段
  - 该字段值应该是目标Definition的elementId
  - 响应格式符合ElementDTO规范

### REQ-TDD-001-3: 引用验证
- **given**: RequirementUsage引用了不存在的RequirementDefinition
- **when**: 创建该对象
- **then**: 
  - 应该抛出IllegalArgumentException
  - 错误消息应该包含 "引用目标对象未找到"

### REQ-TDD-001-4: 空引用处理
- **given**: RequirementUsage的requirementDefinition为null
- **when**: 创建该对象
- **then**: 
  - 对象创建成功
  - requirementDefinition字段为null
  - API响应中不包含该字段

## 验收标准
1. 所有测试用例通过
2. API响应包含正确的requirementDefinition字段
3. EMF引用关系在JSON序列化中正确处理
4. 错误情况有适当的异常处理

## 技术约束
- 严格按照EMF标准：EReference vs EAttribute区别
- 保持现有API格式兼容性
- 不破坏现有的TraceService和ValidationService功能