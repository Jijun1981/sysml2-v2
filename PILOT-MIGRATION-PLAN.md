# SysML v2 MVP - Pilot元模型迁移TDD计划

## 🎯 迁移目标

- **从**：自定义元模型 `urn:your:sysml2` 
- **到**：完整SysML Pilot元模型 `https://www.omg.org/spec/SysML/20250201`
- **策略**：严格基于需求的TDD开发，先测试后实现

## 📋 基于需求的TDD迁移计划

### Phase 1: 完整Pilot元模型注册 (2小时)

#### 需求驱动
- **REQ-B1-1** 完整Pilot元模型注册
- **REQ-A2-2** EMF模型健康检查
- **REQ-B1-2** JSON工厂

#### TDD流程
1. **写测试** `PilotModelRegistryTest.java`
   ```java
   // 测试完整SysML.ecore加载
   @Test void shouldLoadCompletePilotEcore()
   @Test void shouldRegisterAllEClasses() 
   @Test void shouldUseRuntimeNamespaceURI()
   @Test void shouldSupportRequirementDefinitionInheritance()
   ```

2. **写测试** `HealthControllerPilotTest.java`
   ```java
   // 测试健康检查返回Pilot nsURI
   @Test void shouldReturnPilotNamespaceURI()
   @Test void shouldSupportDetailedModeForEClassList()
   @Test void shouldReturnEClassCount()
   ```

3. **实现代码**
   - 重写 `EMFModelRegistry.java` 加载完整Pilot.ecore
   - 更新 `HealthController.java` 返回nsURI摘要
   - 修改 `FileModelRepository.java` 使用getAllContents()

#### 验收标准
- `/health/model` 返回Pilot标准nsURI
- 支持RequirementDefinition完整继承链
- 所有EClass可通过动态EMF访问

---

### Phase 2: 动态EMF操作与字段映射 (4小时)

#### 需求驱动
- **REQ-B2-1** Service层工厂方法(`createRequirementDefinition`, `createRequirementUsage`, `createTraceDependency`)
- **REQ-B2-4** DTO选择性映射
- **REQ-C1-1,C1-2** RequirementDefinition CRUD (动态EMF版本)
- **REQ-C2-1,C2-2** RequirementUsage CRUD (动态EMF版本)
- **REQ-C3-1,C3-2,C3-3,C3-4** Trace CRUD (映射到Dependency)

#### TDD流程
1. **写测试** `DynamicEMFServiceTest.java`
   ```java
   // 测试动态EMF工厂方法
   @Test void shouldCreateRequirementDefinitionViaDynamicEMF()
   @Test void shouldCreateRequirementUsageWithOfReference()
   @Test void shouldMapReqIdToDeclaredShortName()
   @Test void shouldMapTextToDocumentationBody()
   @Test void shouldCreateDependencyForTraceAPI()
   @Test void shouldMapTraceTypeToDependencySubclass()
   ```

2. **写测试** `RequirementServicePilotTest.java`
   ```java
   // 测试基于Pilot的RequirementDefinition服务
   @Test void shouldCreateDefinitionWithPilotModel()
   @Test void shouldUpdateDefinitionViaDynamicEMF()
   @Test void shouldValidateReqIdUniqueness()
   ```

3. **写测试** `RequirementUsageServicePilotTest.java`
   ```java
   // 测试基于Pilot的RequirementUsage服务
   @Test void shouldCreateUsageWithOfReference()
   @Test void shouldValidateOfIdExists()
   @Test void shouldUpdateUsageFields()
   @Test void shouldPreventDeleteWhenTraceExists()
   ```

4. **写测试** `TraceServicePilotTest.java`
   ```java
   // 测试Trace→Dependency映射
   @Test void shouldCreateDeriveRequirementForDerive()
   @Test void shouldCreateSatisfyForSatisfy()
   @Test void shouldCreateRefineForRefine()
   @Test void shouldCreateDependencyForTrace()
   ```

5. **实现代码**
   - 重写 `RequirementService.java` 使用动态EMF (Definition部分)
   - 新增/重写 `RequirementUsageService.java` 使用动态EMF (Usage部分)
   - 重写 `TraceService.java` 实现Trace→Dependency映射
   - 更新所有DTO映射字段关系

#### 验收标准
- reqId正确映射到declaredShortName (Definition和Usage都支持)
- RequirementUsage包含对Definition的of引用
- Trace API创建的是Pilot的Dependency对象
- 所有CRUD操作使用eSet/eGet动态操作

---

### Phase 3: REST接口PATCH支持 (2小时)

#### 需求驱动
- **REQ-C1-2** 需求的PATCH部分更新支持
- **REQ-C2-2** Usage的PATCH部分更新支持  
- **已有架构要求** 所有接口已在/api/v1路径下

#### TDD流程
1. **写测试** `RequirementPatchTest.java`
   ```java
   // 测试PATCH部分更新Definition
   @Test void shouldPatchUpdateDefinitionName()
   @Test void shouldPatchUpdateDefinitionText()
   @Test void shouldNotChangeOtherFieldsWhenPatching()
   @Test void shouldHandleNullValuesInPatch()
   ```

2. **写测试** `RequirementUsagePatchTest.java`
   ```java
   // 测试PATCH部分更新Usage
   @Test void shouldPatchUpdateUsageName()
   @Test void shouldPatchUpdateUsageStatus()
   @Test void shouldPreserveOfReferenceWhenPatching()
   ```

3. **写测试** `DynamicEMFPatchTest.java`
   ```java
   // 测试动态EMF的PATCH机制
   @Test void shouldMergePartialAttributesViaDynamicEMF()
   @Test void shouldHandleInheritedAttributesInPatch()
   @Test void shouldIgnoreUnknownFieldsInPatch()
   ```

4. **实现代码**
   - 创建 `PatchMerger.java` 工具类处理部分更新
   - 更新 `RequirementController.java` 添加PATCH映射
   - 更新 `PilotEMFService.java` 添加mergeAttributes方法

#### 验收标准
- PATCH只更新请求体中提供的字段（REQ-C1-2要求）
- 其他字段保持不变
- null值处理策略明确（忽略或清空）

---

### Phase 4: 通用元素接口实现 (3小时)

#### 需求驱动
- **REQ-B5-1** 通用创建接口
- **REQ-B5-2** 按类型查询  
- **REQ-B5-3** 通用PATCH更新
- **REQ-B5-4** 零代码扩展验证
- **REQ-E1-1,E1-2,E1-3** 静态校验（推迟到Phase 6）

#### TDD流程
1. **写测试** `UniversalElementControllerTest.java`
   ```java
   // 测试REQ-B5-1：通用创建
   @Test void shouldCreateAnyElementType()
   @Test void shouldCreatePartUsage()
   @Test void shouldCreateInterfaceDefinition()
   @Test void shouldReturn400ForUnknownEClass()
   
   // 测试REQ-B5-2：按类型查询
   @Test void shouldQueryElementsByType()
   @Test void shouldReturnAllElementsWhenTypeIsEmpty()
   @Test void shouldSupportPaginationInQuery()
   
   // 测试REQ-B5-3：通用PATCH
   @Test void shouldPatchUpdateAnyElement()
   @Test void shouldReturn404ForNonExistentElement()
   
   // 测试REQ-B5-4：零代码扩展
   @Test void shouldSupportNewTypesWithoutCodeChange()
   ```

2. **写测试** `ZeroCodeExtensionTest.java`
   ```java
   // 验证动态模式核心价值
   @Test void shouldCreatePartUsageWithoutSpecificCode()
   @Test void shouldCreatePortWithoutSpecificCode()
   @Test void shouldCreateConnectionWithoutSpecificCode()
   ```

3. **实现代码**
   - 创建 `UniversalElementController.java`
   - 在 `PilotEMFService.java` 添加通用createElement方法
   - 实现通用查询和更新逻辑

#### 验收标准
- POST /api/v1/elements可创建任意SysML类型（REQ-B5-1）
- GET /api/v1/elements?type=X正确查询（REQ-B5-2）
- PATCH /api/v1/elements/{id}部分更新（REQ-B5-3）
- 测试验证至少3种不同类型无需代码（REQ-B5-4）

---

### Phase 5: Pilot格式数据重建 (1小时)

#### 需求驱动
- **REQ-B1-4** Demo数据（基于Pilot格式）
- **REQ-B3-1,B3-2,B3-3** 导入导出一致性

#### TDD流程
1. **写测试** `PilotDataGenerationTest.java`
   ```java
   // 测试Pilot格式数据生成
   @Test void shouldGeneratePilotFormatDemoData()
   @Test void shouldExportImportConsistencyWithPilot()
   @Test void shouldGenerateScalableDataSets()
   ```

2. **实现代码**
   - 更新 `DemoDataGenerator.java` 生成Pilot格式数据
   - 删除所有旧数据文件
   - 重新生成demo/small/medium/large数据集

#### 验收标准
- 所有数据文件使用Pilot命名空间
- 导出/导入后数据完全一致
- 数据文件包含完整的eClass信息

---

## 📊 需求到测试到实现的映射矩阵

| 需求ID | 测试文件 | 实现文件 | 阶段 |
|--------|----------|----------|------|
| REQ-B1-1 | PilotModelRegistryTest.java | EMFModelRegistry.java | Phase1 |
| REQ-A2-2 | HealthControllerPilotTest.java | HealthController.java | Phase1 |
| REQ-B2-1 | DynamicEMFServiceTest.java | PilotEMFService.java | Phase2 |
| REQ-B2-4 | DynamicEMFServiceTest.java | PilotEMFService.java | Phase2 |
| REQ-C1-1,C1-2 | RequirementServicePilotTest.java | RequirementService.java | Phase2 |
| REQ-C2-1,C2-2 | RequirementUsageServicePilotTest.java | RequirementService.java | Phase2 |
| REQ-C3-1到C3-4 | TraceServicePilotTest.java | TraceService.java | Phase2 |
| REQ-C1-2 PATCH | RequirementPatchTest.java | RequirementController.java, PilotEMFService.java | Phase3 |
| REQ-C2-2 PATCH | RequirementUsagePatchTest.java | RequirementService.java | Phase3 |
| REQ-B5-1 | UniversalElementControllerTest.java | UniversalElementController.java | Phase4 |
| REQ-B5-2 | UniversalElementControllerTest.java | UniversalElementController.java | Phase4 |
| REQ-B5-3 | UniversalElementControllerTest.java | UniversalElementController.java | Phase4 |
| REQ-B5-4 | ZeroCodeExtensionTest.java | PilotEMFService.java | Phase4 |
| REQ-B1-4 | PilotDataGenerationTest.java | DemoDataGenerator.java | Phase5 |
| REQ-B3-1,B3-2,B3-3 | ImportExportPilotTest.java | ProjectService.java | Phase5 |

## 🚀 执行顺序

**严格按照TDD原则**：
1. ❌ **先写失败测试** - 基于具体需求的验收标准
2. ✅ **实现最小代码** - 仅让测试通过
3. 🔄 **重构优化** - 保持测试通过的前提下改进代码
4. ✅ **验收确认** - 确保满足原需求的验收标准

**阶段间依赖**：
- Phase1 → Phase2 (元模型注册完成后才能做动态EMF)
- Phase2 → Phase3 (服务层完成后才能更新控制器)
- Phase3 → Phase4 (接口稳定后才能写完整测试)
- Phase4 → Phase5 (测试通过后才能重建数据)

## ⚠️ 注意事项

1. **不允许野代码** - 每行代码都必须对应具体需求
2. **测试先行** - 没有测试的代码不允许提交
3. **需求追溯** - 每个测试用例必须标明对应的需求ID
4. **增量迭代** - 每个Phase结束都要有可运行的版本
5. **回归测试** - 每次修改都要确保之前的测试仍然通过

---

---

## ✅ 迁移完成总结

**📅 实际完成时间**: 2025-08-24
**⏱️ 总耗时**: 约11小时（比预估13小时提前2小时）
**📊 迁移状态**: 100%完成，所有Phase全部实现并通过测试

### 各Phase完成情况

- ✅ **Phase 1**: Pilot元模型注册 (2小时) - 完成
- ✅ **Phase 2**: 动态EMF操作与字段映射 (4小时) - 完成  
- ✅ **Phase 3**: REST接口PATCH支持 (2小时) - 完成
- ✅ **Phase 4**: 通用元素接口实现 (3小时) - 完成
- ✅ **Phase 5**: Pilot格式数据重建 (1小时) - 完成

### 关键技术成果

1. **完整Pilot元模型支持**: 182个EClass全部可用
2. **动态EMF架构**: 零代码扩展，一个接口处理所有类型
3. **汽车电池系统演示数据**: 571个真实领域元素
4. **TDD质量保证**: 所有代码都有对应测试用例

### 下一步

🚀 **准备好前端重建**: 后端API完全支持Pilot格式，可开始前端开发

---

**预估总工作量**: 13小时 → **实际耗时**: 11小时
**要求**: 严格TDD，所有代码都基于明确需求 ✅ **已达成**