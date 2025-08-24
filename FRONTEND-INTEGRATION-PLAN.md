# SysML v2 MVP - 前端集成与工程约束TDD计划

## 🎯 开发目标

- **从**：后端通用接口完成，前端基础框架存在
- **到**：完整的前后端集成，三视图联动，关键工程约束实现
- **策略**：严格基于需求的TDD开发，先测试后实现

## 📋 基于需求的TDD开发计划

### Phase 1: 前端通用接口集成 (4小时)

#### 需求驱动
- **REQ-A1-1** 数据源唯一 (视图间数据同步)
- **REQ-D0-1,D0-2,D0-3** 前端数据初始化
- **REQ-D1-3** 树视图联动
- **REQ-D2-2** 表视图联动

#### TDD流程
1. **写测试** `api.test.ts`
   ```typescript
   // 测试前端API服务调用通用接口
   describe('UniversalElementAPI', () => {
     test('should create RequirementDefinition via universal API')
     test('should query elements by type')
     test('should update element via PATCH')
     test('should handle API error responses correctly')
   })
   ```

2. **写测试** `ModelContext.test.tsx`
   ```typescript
   // 测试SSOT数据管理
   describe('ModelContext SSOT', () => {
     test('should sync data across views when element created')
     test('should update all views when element modified')
     test('should maintain single source of truth')
   })
   ```

3. **写测试** `ViewSync.integration.test.tsx`
   ```typescript
   // 测试三视图联动
   describe('Three View Sync', () => {
     test('tree selection should highlight in table and graph')
     test('table row click should update tree and graph')
     test('graph node selection should sync with tree and table')
   })
   ```

4. **实现代码**
   - 重写 `frontend/src/services/api.ts` 调用通用接口
   - 完善 `frontend/src/contexts/ModelContext.tsx` 实现SSOT
   - 更新 `frontend/src/components/views/` 三个视图组件的联动逻辑

#### 验收标准
- 前端完全使用 `/api/v1/elements` 通用接口
- 任一视图操作，其他视图实时同步（无需刷新）
- 三视图选中状态保持一致

---

### Phase 2: 工程约束与验证系统 (6小时)

#### 需求驱动
- **REQ-C2-1** RequirementUsage的subject必填验证
- **新增工程约束** QUDV单位量纲校验 (基于需求文档v3.1)
- **新增工程约束** 参数化文本渲染功能
- **新增工程约束** 追溯语义约束验证
- **REQ-E1-1,E1-2,E1-3** 核心静态校验规则

#### TDD流程
1. **写测试** `RequirementValidationTest.java`
   ```java
   // 测试需求工程约束
   @Test void shouldValidateSubjectIsMandatoryForUsage()
   @Test void shouldValidateQUDVUnitsForDuration()  
   @Test void shouldValidateQUDVUnitsForPower()
   @Test void shouldValidateQUDVUnitsForTemperature()
   @Test void shouldRejectInvalidUnitDimensionCombination()
   ```

2. **写测试** `ParameterizedTextRenderingTest.java`
   ```java
   // 测试参数化文本渲染
   @Test void shouldRenderParameterizedDefinitionText()
   @Test void shouldSubstituteUsageParametersInText()
   @Test void shouldHandleMissingParametersGracefully()
   ```

3. **写测试** `TraceSemanticValidationTest.java`
   ```java
   // 测试追溯语义约束
   @Test void shouldValidateSatisfyDirection() // 设计->需求
   @Test void shouldValidateDeriveRequirementDirection() // 需求->需求
   @Test void shouldValidateRefineDirection() // 需求->需求
   @Test void shouldRejectInvalidTraceSemantics()
   ```

4. **写测试** `StaticValidationServiceTest.java`
   ```java
   // 测试静态校验系统
   @Test void shouldDetectDuplicateReqIds()
   @Test void shouldDetectCircularDependencies() 
   @Test void shouldDetectBrokenReferences()
   @Test void shouldReturnValidationReport()
   ```

5. **实现代码**
   - 新增 `ValidationService.java` 实现静态校验
   - 新增 `QUDVValidationService.java` 实现单位校验
   - 新增 `ParameterizedTextRenderer.java` 实现文本渲染
   - 扩展 `UniversalElementService.java` 增加语义约束验证
   - 新增 `ValidationController.java` 提供校验API

#### 验收标准
- RequirementUsage创建时强制subject验证
- Duration类型参数仅接受s/min/h/day单位
- Power类型参数仅接受W/kW/MW/hp单位
- Temperature类型参数仅接受℃/K/℉单位
- Satisfy关系强制验证方向：设计元素→需求
- 参数化Definition可渲染为完整Usage文本
- POST /validate/static 返回完整校验报告

---

### Phase 3: 性能优化与用户体验 (4小时)

#### 需求驱动
- **REQ-A1-3** 性能底线 (500节点<500ms响应)
- **现有需求的UX改进** 基于REQ-D1-2, REQ-D2-1的用户体验优化
- **错误处理完善** 基于API契约的统一错误处理

#### TDD流程
1. **写测试** `PerformanceTest.java`
   ```java
   // 测试性能底线
   @Test void shouldHandleLargeModelUnder500ms()
   @Test void shouldImplementPaginationForLargeResults() 
   @Test void shouldEnableLazyLoadingWhenNeeded()
   ```

2. **写测试** `ErrorHandling.test.tsx`
   ```typescript
   // 测试前端错误处理
   describe('Unified Error Handling', () => {
     test('should display validation errors clearly')
     test('should handle network errors gracefully')
     test('should show loading states during operations')
   })
   ```

3. **写测试** `UserExperience.e2e.test.js`
   ```javascript
   // 测试端到端用户体验
   describe('User Experience Flow', () => {
     test('should create requirement with immediate feedback')
     test('should show progress during validation')
     test('should recover from errors gracefully')
   })
   ```

4. **实现代码**
   - 优化查询性能，添加必要的缓存机制
   - 实现前端loading状态和错误提示组件
   - 完善分页和虚拟滚动机制
   - 添加操作确认和撤销机制

#### 验收标准
- 500节点模型的视图响应时间<500ms
- 用户操作有清晰的反馈和状态指示
- 错误信息用户友好且可操作
- 支持大模型的分页浏览

---

## 🎯 总体验收标准

### MVP功能完整性
- ✅ 后端通用接口架构 (已完成)
- ✅ 前端三视图联动实现
- ✅ 关键工程约束验证
- ✅ 基本性能要求达标

### 质量标准  
- 测试覆盖率 > 80%
- 所有需求都有对应的测试用例
- 端到端功能流程验证通过
- API契约完全符合需求文档

### 技术债务
- 旧的专门化Controller标记@Deprecated
- 代码注释和文档同步更新
- 无安全漏洞和性能瓶颈

## 📊 工作量估算

| 阶段 | 工作量 | 关键里程碑 |
|-----|-------|-----------|
| Phase 1 | 4小时 | 前端通用接口集成完成 |
| Phase 2 | 6小时 | 工程约束验证系统完成 |
| Phase 3 | 4小时 | 性能优化和UX完善 |
| **总计** | **14小时** | **MVP功能闭环完成** |

## 🚀 开始建议

**推荐从Phase 1开始**，因为：
1. 前端集成能快速验证后端通用接口的价值
2. 三视图联动是用户最直观感受到的功能
3. 为后续工程约束提供展示和验证平台

你觉得这个计划合理吗？每个阶段都基于tracking matrix中的具体需求，严格按TDD流程执行。