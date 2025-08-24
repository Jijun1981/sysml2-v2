# SysML 2.0 + EMF 动态模式架构指南

## 核心价值

SysML 2.0 + EMF动态模式是本项目的架构核心，它通过动态EMF（Eclipse Modeling Framework）技术实现了对完整SysML 2.0 Pilot元模型（182个EClass）的统一操作，无需为每个类型编写专门的代码。

## 一、架构概览

```
┌─────────────────────────────────────────────────┐
│           REST API Layer (极简)                  │
│         /api/v1/elements (通用接口)              │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│         Service Layer (单一服务)                 │
│          PilotEMFService (工厂)                  │
│    ┌─────────────────────────────────┐          │
│    │ • createElement(type, attrs)    │          │
│    │ • getAttributeValue(obj, name)  │          │
│    │ • setAttributeIfExists()        │          │
│    └─────────────────────────────────┘          │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│         EMF Dynamic Layer                        │
│     SysML.ecore (182 EClasses)                   │
│    ┌─────────────────────────────────┐          │
│    │ RequirementDefinition/Usage     │          │
│    │ PartDefinition/Usage            │          │
│    │ InterfaceDefinition/Usage       │          │
│    │ Package, Port, Connection...    │          │
│    └─────────────────────────────────┘          │
└──────────────────────────────────────────────────┘
```

## 二、核心模式：动态工厂

### 2.1 传统静态方式（❌ 我们不用）

```java
// 需要为每个类型写专门的Service
public class RequirementService { ... }
public class PartService { ... }
public class InterfaceService { ... }
// ... 182个Service类！

// 需要为每个类型写专门的Controller
@RestController
public class RequirementController { ... }
@RestController  
public class PartController { ... }
// ... 182个Controller类！
```

### 2.2 动态EMF方式（✅ 我们的选择）

```java
// 一个通用服务处理所有类型
@Service
public class PilotEMFService {
    
    // 通用创建方法 - 可创建任何SysML元素
    public EObject createElement(String eClassName, Map<String, Object> attributes) {
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass eClass = (EClass) sysmlPackage.getEClassifier(eClassName);
        
        EObject element = sysmlPackage.getEFactoryInstance().create(eClass);
        
        // 动态设置所有属性
        attributes.forEach((key, value) -> 
            setAttributeIfExists(element, key, value)
        );
        
        return element;
    }
    
    // 为常用类型提供便捷方法（可选）
    public EObject createRequirementDefinition(String reqId, String name, String text) {
        return createElement("RequirementDefinition", Map.of(
            "declaredShortName", reqId,
            "declaredName", name,
            "documentation", createDocumentation(text)
        ));
    }
}
```

## 三、字段映射策略

### 3.1 API字段 → Pilot字段映射

| API层字段（简化） | Pilot元模型字段（标准） | 说明 |
|-----------------|----------------------|------|
| `id` | `elementId` | 元素唯一标识 |
| `name` | `declaredName` | 元素名称 |
| `reqId` | `declaredShortName` | 需求短名称 |
| `text` | `documentation.body` | 文档内容 |
| `type` | `eClass().getName()` | 元素类型 |
| `of` | `definition` | Usage对Definition的引用 |

### 3.2 映射实现

```java
private boolean setAttributeIfExists(EObject eObject, String attributeName, Object value) {
    EClass eClass = eObject.eClass();
    
    // 查找属性（包括继承的）
    for (EStructuralFeature feature : eClass.getEAllStructuralFeatures()) {
        if (attributeName.equals(feature.getName())) {
            eObject.eSet(feature, value);
            return true;
        }
    }
    return false;
}
```

## 四、REST API设计

### 4.1 通用接口模式

```
# 创建任意元素
POST /api/v1/elements
{
    "eClass": "PartUsage",
    "attributes": {
        "declaredName": "Engine",
        "definition": "part-def-123"
    }
}

# 查询特定类型
GET /api/v1/elements?type=PartUsage

# 更新元素
PATCH /api/v1/elements/{id}
{
    "attributes": {
        "declaredName": "UpdatedEngine"
    }
}

# 删除元素
DELETE /api/v1/elements/{id}
```

### 4.2 专用接口（为了用户友好）

虽然可以完全通用化，但为了API的易用性，我们仍为核心类型提供专用接口：

```
POST /api/v1/requirements     # 创建需求（自动判断Definition/Usage）
GET /api/v1/requirements/{id}/traces  # 获取追溯关系
POST /api/v1/traces           # 创建追溯
```

## 五、扩展新类型（零代码）

### 5.1 支持新的SysML类型

无需修改代码！例如要支持`ActionUsage`：

```javascript
// 前端直接调用
await fetch('/api/v1/elements', {
    method: 'POST',
    body: JSON.stringify({
        eClass: 'ActionUsage',
        attributes: {
            declaredName: 'ValidateInput',
            declaredShortName: 'ACT-001'
        }
    })
});
```

### 5.2 添加便捷方法（可选）

如果某类型使用频繁，可在PilotEMFService添加便捷方法：

```java
public EObject createActionUsage(String name, String behavior) {
    return createElement("ActionUsage", Map.of(
        "declaredName", name,
        "behavior", behavior
    ));
}
```

## 六、核心优势

### 6.1 极简的代码量
- **1个Service** vs 182个Service
- **1个通用Controller** vs 182个Controller
- **代码减少99%**

### 6.2 完全的灵活性
- 支持Pilot全部182个EClass
- 新增类型零代码
- 字段映射可配置

### 6.3 标准兼容性
- 完全符合OMG SysML 2.0标准
- 使用官方Pilot元模型
- 可与其他SysML工具交换数据

### 6.4 易于维护
- 所有逻辑集中在PilotEMFService
- 字段映射规则清晰
- 测试简单（只需测试一个服务）

## 七、实际应用示例

### 7.1 创建复杂的SysML模型

```java
// 创建Package
EObject pkg = pilotService.createElement("Package", Map.of(
    "declaredName", "VehicleSystem"
));

// 创建PartDefinition
EObject engineDef = pilotService.createElement("PartDefinition", Map.of(
    "declaredName", "EngineDefinition",
    "owner", pkg  // 设置包含关系
));

// 创建PartUsage
EObject engineUsage = pilotService.createElement("PartUsage", Map.of(
    "declaredName", "mainEngine",
    "definition", engineDef  // 引用Definition
));

// 创建Connection
EObject connection = pilotService.createElement("ConnectionUsage", Map.of(
    "declaredName", "PowerConnection",
    "source", engineUsage,
    "target", transmissionUsage
));
```

### 7.2 查询和遍历模型

```java
// 查找所有PartUsage
List<EObject> parts = repository.findByType("PartUsage");

// 动态访问属性
for (EObject part : parts) {
    String name = (String) pilotService.getAttributeValue(part, "declaredName");
    EObject definition = (EObject) pilotService.getAttributeValue(part, "definition");
    
    System.out.println("Part: " + name + " of type: " + 
        pilotService.getAttributeValue(definition, "declaredName"));
}
```

## 八、最佳实践

### 8.1 使用工厂方法而非直接EMF

```java
// ✅ 好 - 使用PilotEMFService
EObject req = pilotService.createRequirementDefinition(reqId, name, text);

// ❌ 避免 - 直接操作EMF
EObject req = ePackage.getEFactoryInstance().create(reqClass);
req.eSet(feature, value);  // 容易出错
```

### 8.2 保持API简洁

```java
// ✅ 好 - 简化的API字段
{
    "name": "Engine",
    "type": "part"
}

// ❌ 避免 - 暴露内部字段
{
    "declaredName": "Engine",
    "eClass": "PartUsage"
}
```

### 8.3 利用继承层次

SysML 2.0的继承层次自动生效：
- `Element` → 所有元素的基类
- `NamedElement` → 有名称的元素
- `Definition` → 所有Definition的基类
- `Usage` → 所有Usage的基类

## 九、性能考虑

### 9.1 缓存策略
```java
@Service
public class PilotEMFService {
    // 缓存EClass查找
    private final Map<String, EClass> classCache = new ConcurrentHashMap<>();
    
    private EClass getEClass(String name) {
        return classCache.computeIfAbsent(name, 
            n -> (EClass) sysmlPackage.getEClassifier(n));
    }
}
```

### 9.2 批量操作
```java
// 批量创建元素
public List<EObject> createElements(List<ElementRequest> requests) {
    return requests.parallelStream()
        .map(req -> createElement(req.getEClass(), req.getAttributes()))
        .collect(Collectors.toList());
}
```

## 十、总结

SysML 2.0 + EMF动态模式是一个**革命性的架构选择**：

1. **一套代码，支持所有SysML元素** - 不是为每个类型写代码，而是写一个通用引擎
2. **标准驱动，而非代码驱动** - 元模型定义行为，代码只是执行器
3. **扩展零成本** - 新增SysML类型不需要改代码
4. **维护极简** - 所有逻辑集中在一处

这就是为什么我们选择动态EMF而不是静态代码生成 - **它让我们用最少的代码，实现最大的灵活性**。

## 附录：相关文件

- `/backend/src/main/java/com/sysml/mvp/service/PilotEMFService.java` - 核心工厂服务
- `/backend/src/main/java/com/sysml/mvp/model/EMFModelRegistry.java` - 元模型注册
- `/backend/src/main/resources/metamodel/SysML.ecore` - Pilot元模型文件
- `/PILOT-MIGRATION-PLAN.md` - 迁移计划文档