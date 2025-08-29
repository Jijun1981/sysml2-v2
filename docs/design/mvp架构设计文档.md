# SysML v2 建模平台 MVP 架构设计文档

## 文档信息

* **版本** : 2.0
* **日期** : 2025-08-24
* **状态** : 通用接口架构定稿（基于动态EMF）
* **关联文档** : 《MVP需求书v2.1》、《API接口文档v1.0》

---

## 1. 架构概述

### 1.1 MVP架构原则

* **最小可行** ：仅实现核心需求，不过度设计
* **技术收敛** ：单体架构，文件即模型，单一图形库
* **快速验证** ：4周可交付，技术风险可控
* **扩展预留** ：版本字段、时间戳、ID稳定性
* **分层隔离** ：领域逻辑与技术实现严格分离，领域层不依赖EMF
* **委托模式** ：领域层通过委托方式使用基础设施层的技术能力
* **单一职责** ：每层只负责自己的职责，不越界操作

### 1.2 技术边界（明确不做）

* ❌  **不做** ：分布式架构、微服务、消息队列
* ❌  **不做** ：复杂权限、多租户、审计日志
* ❌  **不做** ：实时协作、WebSocket、服务端推送（SSE除外）
* ❌  **不做** ：计算节点执行引擎（MVP硬编码校验）
* ✅  **已实现** ：完整SysML v2 Pilot元模型（182个EClass全部可用）
* ❌  **不做** ：性能优化（索引、缓存、查询优化）
* ❌  **不做** ：数据库依赖（PostgreSQL等）

---

## 2. 技术架构

### 2.1 元模型来源说明

**重要原则**：采用完整的SysML Pilot元模型，不做简化，保证100%标准兼容性。

- **元模型来源**：SysML v2 Pilot Implementation (https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation)
- **注册策略**：加载完整的SysML.ecore文件，注册所有EClass（包括暂时不用的）
- **命名空间**：以运行时加载的Pilot包nsURI为准（不写死日期），`/api/v1/health/model`返回`{name, nsURI, eClassCount}`
- **完整继承链**：保持所有层次关系（Element→NamedElement→Feature→Type→Classifier→Definition→OccurrenceDefinition→ConstraintDefinition→RequirementDefinition）
- **扩展性保证**：所有Pilot定义的类型都可用，未来需要Part、Port、Interface等可直接使用
- **使用策略**：通过通用元素接口支持所有182个SysML类型，实现零代码扩展

### 2.1 整体架构图

```
┌────────────────────────────────────────────────────────┐
│                   前端层 (SPA)                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │   React 18 + TypeScript + React Flow 11          │  │
│  ├──────────────────────────────────────────────────┤  │
│  │  Views: TreeView | TableView | GraphView         │  │
│  │  State: Context API (no Redux)                   │  │
│  │  HTTP:  Fetch API (no Axios)                     │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
                        ↓ REST + JSON
┌────────────────────────────────────────────────────────┐
│                   应用层 (单体)                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Controller层                         │  │
│  │   /api/v1/elements → AdvancedQueryController     │  │
│  │   /api/v1/requirements → RequirementController   │  │
│  │   /api/v1/traces → TraceController               │  │
│  ├──────────────────────────────────────────────────┤  │
│  │           领域业务层 (Domain Layer)              │  │
│  │   RequirementService: 需求业务逻辑              │  │
│  │   TraceService: 追溯关系管理                    │  │
│  │   ValidationService: 业务规则验证               │  │
│  │   ❌ 不直接操作EMF，只处理业务逻辑             │  │
│  ├────────────↓─────委托─────↓──────────────────────┤  │
│  │         基础设施层 (Infrastructure Layer)        │  │
│  │   UniversalElementService: 通用EMF操作          │  │
│  │   PilotEMFService: EMF元模型和工厂              │  │
│  │   FileModelRepository: EMF Resource持久化       │  │
│  │   ✅ 所有EMF操作都在这一层                      │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
                        ↓ EMF JSON + FileSystem
┌────────────────────────────────────────────────────────┐
│                   数据层 (文件系统)                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │           本地JSON文件存储                        │  │
│  ├──────────────────────────────────────────────────┤  │
│  │  格式: EMF JSON Resource                          │  │
│  │  结构: projects/{pid}/model.json                  │  │
│  │  索引: 内存Map缓存                               │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 2.2 核心组件设计

#### 2.2.1 EMF模型（完整Pilot注册）

```java
// 注册完整的SysML Pilot元模型，包含所有EClass
@Component
public class EMFModelRegistry {
  
    @PostConstruct
    public void registerCompletePilotMetamodel() {
        try {
            // 加载完整的SysML.ecore文件
            URI ecoreURI = URI.createFileURI("model/SysML.ecore"); 
            ResourceSet resourceSet = new ResourceSetImpl();
            
            // 注册Ecore资源工厂
            resourceSet.getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());
            
            // 加载Ecore文件
            Resource ecoreResource = resourceSet.getResource(ecoreURI, true);
            EPackage sysmlPackage = (EPackage) ecoreResource.getContents().get(0);
            
            // 注册完整的元模型包
            EPackage.Registry.INSTANCE.put(sysmlPackage.getNsURI(), sysmlPackage);
            
            // 日志记录已注册的EClass数量
            log.info("Registered {} EClasses from SysML Pilot", 
                     sysmlPackage.getEClassifiers().size());
            
            // 列出关键的EClass供参考
            logImportantEClasses(sysmlPackage);
            
        } catch (Exception e) {
            log.error("Failed to load SysML Pilot metamodel", e);
            // 降级方案：创建最小必需的EClass
            registerMinimalFallback();
        }
    }
    
    private void logImportantEClasses(EPackage pkg) {
        // 当前MVP使用的核心类
        log.info("Core classes for MVP:");
        log.info("- RequirementDefinition: {}", pkg.getEClassifier("RequirementDefinition"));
        log.info("- RequirementUsage: {}", pkg.getEClassifier("RequirementUsage"));
        log.info("- Dependency: {}", pkg.getEClassifier("Dependency"));
        
        // 未来可能使用的类
        log.info("Available for future use:");
        log.info("- PartDefinition: {}", pkg.getEClassifier("PartDefinition"));
        log.info("- PartUsage: {}", pkg.getEClassifier("PartUsage"));
        log.info("- InterfaceDefinition: {}", pkg.getEClassifier("InterfaceDefinition"));
        log.info("- PortUsage: {}", pkg.getEClassifier("PortUsage"));
        log.info("- ActionDefinition: {}", pkg.getEClassifier("ActionDefinition"));
    }
}
```

#### 2.2.2 持久化设计（JSON文件）

```java
// 使用sirius-emfjson库进行JSON序列化（参考Syson实现）
@Component
public class FileModelRepository {
    
    private static final String DATA_ROOT = "data/projects";
    private final Map<String, ResourceSet> resourceCache = new ConcurrentHashMap<>();
    
    // 项目文件路径: data/projects/{projectId}/model.json
    private Path getProjectPath(String projectId) {
        return Paths.get(DATA_ROOT, projectId, "model.json");
    }
    
    // 创建JsonResource（使用sirius-emfjson）
    private JsonResource createJsonResource(URI uri, ResourceSet resourceSet) {
        // 使用JsonResourceFactoryImpl创建资源
        JsonResource resource = (JsonResource) new JsonResourceFactoryImpl()
            .createResource(uri);
        resourceSet.getResources().add(resource);
        
        // 添加CrossReferenceAdapter处理引用
        resourceSet.eAdapters().add(new EditingContextCrossReferenceAdapter());
        
        return resource;
    }
    
    // 加载项目模型
    public Resource loadProject(String projectId) {
        Path projectPath = getProjectPath(projectId);
        ResourceSet resourceSet = new ResourceSetImpl();
        
        URI uri = URI.createFileURI(projectPath.toString());
        JsonResource resource = createJsonResource(uri, resourceSet);
        
        if (!Files.exists(projectPath)) {
            // 创建空项目
            return resource;
        }
        
        try {
            // 加载选项：避免循环引用
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            
            resource.load(options);
            resourceCache.put(projectId, resourceSet);
            return resource;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load project: " + projectId, e);
        }
    }
    
    // 保存项目模型
    public void saveProject(String projectId, Resource resource) {
        try {
            Path projectPath = getProjectPath(projectId);
            Files.createDirectories(projectPath.getParent());
            
            // 保存选项：生成标准EMF JSON格式
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            options.put(JsonResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
            
            resource.save(options);
            
            // 更新时间戳
            updateProjectTimestamp(projectId);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to save project: " + projectId, e);
        }
    }
    
    // 查找特定类型的对象（遍历所有嵌套元素）
    public List<EObject> findByType(String projectId, String eClassName) {
        Resource resource = loadProject(projectId);
        List<EObject> result = new ArrayList<>();
        
        // 使用getAllContents()遍历所有嵌套元素，避免遗漏
        for (TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
            EObject obj = it.next();
            if (eClassName.equals(obj.eClass().getName())) {
                result.add(obj);
            }
        }
        return result;
    }
}
```

#### 2.2.3 服务层设计（领域层 + 基础层）

##### A. 基础设施层（UniversalElementService）
```java
/**
 * 通用元素服务 - 基础设施层
 * 封装所有EMF操作，为领域层提供技术能力
 * 领域服务通过委托调用此服务，但不知道EMF细节
 */
@Service
public class UniversalElementService {
    
    @Autowired
    private PilotEMFService pilotService;
    
    @Autowired
    private FileModelRepository repository;
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    /**
     * 创建元素 - 通用EMF操作
     * @param eClassName SysML类型名
     * @param attributes 属性Map（领域层准备的数据）
     * @return ElementDTO（不返回EObject，隔离EMF）
     */
    public ElementDTO createElement(String eClassName, Map<String, Object> attributes) {
        // EMF操作1：创建EObject
        EObject element = pilotService.createElement(eClassName, attributes);
        
        // EMF操作2：持久化
        Resource resource = repository.loadProject("default");
        resource.getContents().add(element);
        repository.saveProject("default", resource);
        
        // 返回DTO而不是EObject，隔离EMF对象
        return convertToDTO(element);
    }
    
    /**
     * REQ-B5-2: 按类型查询元素（返回List）
     * 支持查询任意SysML类型：RequirementDefinition、PartUsage、InterfaceDefinition等
     */
    public List<ElementDTO> queryElements(String type) {
        Resource resource = repository.loadProject(DEFAULT_PROJECT_ID);
        
        List<ElementDTO> result = new ArrayList<>();
        
        // 遍历所有内容（包括嵌套）
        Iterator<EObject> iter = resource.getAllContents();
        while (iter.hasNext()) {
            EObject obj = iter.next();
            
            // 如果指定了类型，过滤
            if (type == null || type.isEmpty() || obj.eClass().getName().equals(type)) {
                result.add(convertToDTO(obj));
            }
        }
        
        return result;
    }
    
    /**
     * REQ-B5-3: PATCH更新元素
     * 支持部分更新任意SysML类型
     */
    public ElementDTO patchElement(String elementId, Map<String, Object> updates) {
        EObject element = findElementById(elementId);
        if (element == null) {
            return null;
        }
        
        // 使用PilotEMFService的合并方法
        pilotService.mergeAttributes(element, updates);
        
        // 保存
        Resource resource = repository.loadProject(DEFAULT_PROJECT_ID);
        repository.saveProject(DEFAULT_PROJECT_ID, resource);
        
        return convertToDTO(element);
    }
    
    /**
     * 动态EMF转换为通用DTO
     * 支持所有182个SysML类型的属性提取
     */
    private ElementDTO convertToDTO(EObject eObject) {
        ElementDTO dto = new ElementDTO();
        
        // 设置eClass
        dto.setEClass(eObject.eClass().getName());
        
        // 设置elementId
        Object idObj = pilotService.getAttributeValue(eObject, "elementId");
        String elementId = idObj != null ? idObj.toString() : null;
        dto.setElementId(elementId);
        
        // 动态复制所有属性
        for (EAttribute attr : eObject.eClass().getEAllAttributes()) {
            String name = attr.getName();
            Object value = eObject.eGet(attr);
            
            if (value != null && !"elementId".equals(name)) {
                // 处理EMF特殊类型
                if (value instanceof org.eclipse.emf.common.util.Enumerator) {
                    dto.setProperty(name, ((org.eclipse.emf.common.util.Enumerator) value).getLiteral());
                } else if (value instanceof List) {
                    // 处理List类型属性
                    dto.setProperty(name, processListValue((List<?>) value));
                } else {
                    dto.setProperty(name, value);
                }
            }
        }
        
        // 动态复制简单引用
        for (EReference ref : eObject.eClass().getEAllReferences()) {
            if (!ref.isContainment() && !ref.isMany()) {
                EObject target = (EObject) eObject.eGet(ref);
                if (target != null) {
                    Object targetIdObj = pilotService.getAttributeValue(target, "elementId");
                    String targetId = targetIdObj != null ? targetIdObj.toString() : null;
                    if (targetId != null) {
                        dto.setProperty(ref.getName() + "Id", targetId);
                    }
                }
            }
        }
        
        return dto;
    }
}

##### B. 领域业务层（委托模式示例）

**RequirementService - 需求管理领域服务**
```java
/**
 * 需求管理服务 - 领域层
 * 只处理业务逻辑，不知道EMF存在
 * 所有技术操作委托给基础层
 */
@Service
public class RequirementService {
    
    @Autowired
    private UniversalElementService elementService;  // 依赖基础层
    
    // 注意：没有import任何EMF相关的类
    
    /**
     * 创建需求 - 业务方法
     */
    public RequirementDTO createRequirement(CreateRequirementRequest request) {
        // 步骤1：业务验证（纯业务逻辑）
        validateBusinessRules(request);
        checkDuplicateReqId(request.getReqId());
        
        // 步骤2：准备数据（普通Map）
        Map<String, Object> data = new HashMap<>();
        data.put("elementId", generateRequirementId());
        data.put("declaredName", request.getName());
        data.put("reqId", request.getReqId());
        data.put("documentation", request.getText());
        
        // 步骤3：委托给基础层执行（不知道EMF）
        ElementDTO element = elementService.createElement("RequirementDefinition", data);
        
        // 步骤4：转换为领域DTO
        return convertToRequirementDTO(element);
    }
    
    private void validateBusinessRules(CreateRequirementRequest request) {
        // 纯业务规则验证
        if (request.getReqId() == null || request.getReqId().isEmpty()) {
            throw new BusinessException("reqId is required");
        }
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new BusinessException("name is required");
        }
    }
}
```

**TraceService - 追溯关系领域服务**
```java
/**
 * 追溯关系服务 - 领域层
 * 同样不知道EMF，委托给基础层
 */
@Service
public class TraceService {
    
    @Autowired
    private UniversalElementService elementService;
    
    /**
     * 创建追溯关系
     */
    public TraceDTO createTrace(String fromId, String toId, String traceType) {
        // 业务验证
        validateTraceType(traceType);
        checkCyclicDependency(fromId, toId);
        
        // 准备数据
        Map<String, Object> data = new HashMap<>();
        data.put("elementId", generateTraceId());
        data.put("source", fromId);
        data.put("target", toId);
        data.put("kind", traceType);
        
        // 委托创建
        ElementDTO element = elementService.createElement("Dependency", data);
        
        return convertToTraceDTO(element);
    }
    
    private void checkCyclicDependency(String fromId, String toId) {
        // 业务规则：检查是否形成循环依赖
        // 通过基础层查询，但不直接操作EMF
        List<ElementDTO> traces = elementService.queryElements("Dependency");
        // ... 循环检测逻辑
    }
}
```

### 2.3 分层架构设计

#### 2.3.1 架构原则 - 六边形架构思想

MVP采用**六边形架构**（Hexagonal Architecture）思想，实现领域逻辑与技术实现的分离：

```
         ┌─────────────────────┐
         │   领域核心（纯净）    │
         │  RequirementService  │
         │    TraceService      │
         │  ValidationService   │
         │   ❌ 不知道EMF存在    │
         └──────────┬──────────┘
                    │ 端口（接口）
                    ↓
         ┌─────────────────────┐
         │   适配器（技术实现）  │
         │ UniversalElementService │
         │   PilotEMFService    │
         │  FileModelRepository  │
         │   ✅ EMF操作在这里    │
         └─────────────────────┘
```

#### 2.3.2 领域层职责

**领域业务层**只关注业务逻辑，不依赖任何技术框架：

```java
// 领域服务示例 - 完全不知道EMF的存在
@Service
public class RequirementService {
    
    // 只依赖基础层接口，不import任何EMF类
    @Autowired
    private UniversalElementService elementService;
    
    // 业务方法
    public void validateRequirement(RequirementDTO req) {
        // 纯业务逻辑验证
        if (req.getReqId() == null || req.getReqId().isEmpty()) {
            throw new BusinessException("reqId is required");
        }
        
        // 检查重复 - 通过基础层查询，返回的是DTO而不是EObject
        List<ElementDTO> existing = elementService.queryElements("RequirementDefinition");
        boolean duplicate = existing.stream()
            .anyMatch(e -> req.getReqId().equals(e.getProperty("reqId")));
        if (duplicate) {
            throw new BusinessException("Duplicate reqId");
        }
    }
}
```

#### 2.3.3 基础层职责

**基础设施层**封装所有技术细节和EMF操作：

```java
// 基础层服务 - 所有EMF操作都在这里
@Service
public class UniversalElementService {
    
    // 这里可以import EMF相关类
    import org.eclipse.emf.ecore.EObject;
    import org.eclipse.emf.ecore.resource.Resource;
    
    // 提供通用的CRUD能力给领域层使用
    public ElementDTO createElement(String type, Map<String, Object> data) {
        // EMF操作：创建EObject
        EObject eObject = createEObject(type, data);
        
        // EMF操作：保存到Resource
        Resource resource = loadResource();
        resource.getContents().add(eObject);
        saveResource(resource);
        
        // 转换为DTO返回（隔离EMF对象）
        return convertToDTO(eObject);
    }
}
```

#### 2.3.4 委托模式实现

领域层通过委托模式使用基础层的能力：

```
用户请求
    ↓
RequirementController
    ↓
RequirementService.createRequirement()
    ├─ validateBusinessRules()     // 业务验证
    ├─ prepareData()               // 准备数据
    └─ delegate to ──→ UniversalElementService.createElement()
                              ├─ createEObject()    // EMF操作
                              ├─ saveToResource()   // EMF持久化
                              └─ return DTO         // 返回DTO
```

#### 2.3.5 分层的好处

1. **技术隔离**：领域层不受EMF版本变化影响
2. **测试简化**：领域层可以用简单Mock测试，不需要EMF环境
3. **职责清晰**：业务逻辑和技术实现各司其职
4. **可替换性**：未来可以替换底层实现（如从EMF换到其他技术）
5. **团队协作**：业务开发人员不需要学习EMF

@Service
public class PilotEMFService {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    /**
     * 通用元素创建工厂
     * 支持创建任意SysML类型：RequirementDefinition、PartUsage、InterfaceDefinition、Connection等
     */
    public EObject createElement(String eClassName, Map<String, Object> attributes) {
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass eClass = (EClass) sysmlPackage.getEClassifier(eClassName);
        if (eClass == null) {
            throw new IllegalArgumentException("未知的eClass类型: " + eClassName);
        }
        
        EObject element = sysmlPackage.getEFactoryInstance().create(eClass);
        
        // 设置elementId
        if (!attributes.containsKey("elementId")) {
            String elementId = generateElementId(eClassName);
            element.eSet(eClass.getEStructuralFeature("elementId"), elementId);
        }
        
        // 动态设置所有属性
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            EStructuralFeature feature = eClass.getEStructuralFeature(name);
            if (feature != null && feature instanceof EAttribute) {
                setAttributeValue(element, feature, value);
            }
        }
        
        return element;
    }
    
    /**
     * PATCH支持：合并部分属性更新
     */
    public void mergeAttributes(EObject element, Map<String, Object> updates) {
        EClass eClass = element.eClass();
        
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            EStructuralFeature feature = eClass.getEStructuralFeature(name);
            if (feature != null && feature instanceof EAttribute) {
                setAttributeValue(element, feature, value);
            }
        }
    }
}
```

### 2.3 前端架构

#### 2.3.1 组件结构

```typescript
src/
├── components/
│   ├── TreeView.tsx       // Ant Design Tree
│   ├── TableView.tsx      // Ant Design Table + 虚拟滚动
│   └── GraphView.tsx      // React Flow
├── services/
│   └── api.ts            // Fetch封装
├── contexts/
│   └── ModelContext.tsx  // 全局状态
└── App.tsx
```

#### 2.3.2 状态管理（Context API）

```typescript
// 简单的Context，不用Redux
interface ModelContextType {
    requirements: RequirementDefinition[];
    traces: Trace[];
    selectedId: string | null;
    loading: boolean;
  
    // 需求操作
    createRequirement: (req: CreateRequest) => Promise<void>;
    updateRequirement: (id: string, updates: any) => Promise<void>;
    deleteRequirement: (id: string) => Promise<void>;
    
    // Trace操作（REQ-C3-1至REQ-C3-4）
    createTrace: (fromId: string, req: CreateTraceRequest) => Promise<TraceDTO>;
    getTracesByRequirement: (id: string, dir: 'in'|'out'|'both') => Promise<TraceDTO[]>;
    deleteTrace: (traceId: string) => Promise<void>;
    
    // 视图联动
    selectElement: (id: string) => void;
}

// 三视图共享同一个Context
export const ModelContext = React.createContext<ModelContextType>();
```

#### 2.3.3 视图联动机制

```typescript
// 简单的事件广播
class ViewSync {
    private listeners: Map<string, Function[]> = new Map();
  
    on(event: string, callback: Function) {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, []);
        }
        this.listeners.get(event)!.push(callback);
    }
  
    emit(event: string, data: any) {
        const callbacks = this.listeners.get(event) || [];
        callbacks.forEach(cb => cb(data));
    }
}

// 使用
viewSync.on('element.selected', (id) => {
    // 树、表、图同时高亮
    highlightInTree(id);
    highlightInTable(id);
    highlightInGraph(id);
});
```

---

## 3. 数据流设计

### 3.1 CRUD数据流

**分层数据流架构：**
```
┌──────────────────────────────────────────────────────────┐
│                     前端层                                │
│  用户操作 → React组件 → Fetch API                         │
│                           ↓                               │
│              发送请求: {reqId, name, text}                │
└──────────────────────────────────────────────────────────┘
                           ↓ HTTP
┌──────────────────────────────────────────────────────────┐
│                    Controller层                           │
│  RequirementController.create(@RequestBody DTO)           │
│                           ↓                               │
│              调用: requirementService.create(DTO)         │
└──────────────────────────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────┐
│                   领域业务层                              │
│  RequirementService {                                     │
│    1. validateBusinessRules()  // 业务验证               │
│    2. Map<String,Object> data  // 准备数据               │
│    3. elementService.create()  // 委托调用               │
│  }                                                        │
│  ❌ 不操作EObject，只用Map和DTO                          │
└──────────────────────────────────────────────────────────┘
                           ↓ 委托
┌──────────────────────────────────────────────────────────┐
│                   基础设施层                              │
│  UniversalElementService {                                │
│    1. EObject obj = createEObject(data)  // EMF创建      │
│    2. resource.getContents().add(obj)    // EMF存储      │
│    3. return convertToDTO(obj)           // 返回DTO      │
│  }                                                        │
│  ✅ 所有EMF操作都在这里                                   │
└──────────────────────────────────────────────────────────┘
                           ↓ EMF
┌──────────────────────────────────────────────────────────┐
│                    数据存储层                             │
│              JSON文件 (EMF Resource)                      │
└──────────────────────────────────────────────────────────┘
```

**数据转换流程：**
```
前端DTO → Controller DTO → Map<String,Object> → EObject → JSON
         ↓                ↓                   ↓         ↓
      (JSON)          (Java对象)          (EMF对象)  (文件)
```

**支持任意SysML类型（完整CRUD）：**
```
创建元素 → POST /api/v1/elements {"eClass": "RequirementDefinition", ...}
查询元素 → GET /api/v1/elements?type={eClassName}
获取单个 → GET /api/v1/elements/{elementId}
更新元素 → PUT /api/v1/elements/{elementId} {...updates}
删除元素 → DELETE /api/v1/elements/{elementId}

支持所有182种SysML类型：
- RequirementDefinition, RequirementUsage
- PartDefinition, PartUsage
- InterfaceDefinition, Connection
- ActionDefinition, StateDefinition
... 完全一致的处理流程
```

**领域特定接口（可选，内部委托给UniversalElementService）：**
```
需求管理 → /api/v1/requirements → 委托 → UniversalElementService
追溯管理 → /api/v1/traces → 委托 → UniversalElementService
项目管理 → /api/v1/projects → 直接处理
```

### 3.2 导入导出流程

```
导出：文件系统 → Resource → EMF对象 → JSON序列化 → 文件下载
导入：文件上传 → JSON解析 → EMF对象 → Resource → 文件系统
```

### 3.3 校验流程（硬编码）

```java
@Component
public class ValidationEngine {
  
    @Autowired
    private FileModelRepository repository;
    
    // MVP仅3条规则，硬编码实现
    public ValidationResult validate(String projectId) {
        List<Violation> violations = new ArrayList<>();
        Resource resource = repository.loadProject(projectId);
      
        // 规则1: reqId唯一性
        violations.addAll(checkReqIdUniqueness(resource));
      
        // 规则2: 循环依赖
        violations.addAll(checkCyclicDependency(resource));
      
        // 规则3: 悬挂引用
        violations.addAll(checkBrokenReferences(resource));
      
        return new ValidationResult(violations);
    }
  
    private List<Violation> checkReqIdUniqueness(Resource resource) {
        Map<String, List<RequirementDefinition>> reqIdMap = new HashMap<>();
        
        // 收集所有RequirementDefinition的reqId（遍历所有嵌套元素）
        for (TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
            EObject obj = it.next();
            if (obj instanceof RequirementDefinition) {
                RequirementDefinition def = (RequirementDefinition) obj;
                reqIdMap.computeIfAbsent(def.getReqId(), k -> new ArrayList<>()).add(def);
            }
        }
        
        // 检查重复
        return reqIdMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> new Violation("DUP_REQID", entry.getKey(), 
                "Duplicate reqId: " + entry.getKey()))
            .collect(Collectors.toList());
    }
}
```

---

## 4. 技术决策记录

### 4.1 为什么采用通用接口而不是专门接口？

* ✅ **零代码扩展**：新增SysML类型无需修改任何代码
* ✅ **99%代码减少**：一个接口替代182个专门接口
* ✅ **维护简单**：只需维护一套CRUD逻辑
* ✅ **EMF威力**：充分发挥动态元模型的核心价值
* ✅ **一致性强**：所有类型使用相同的API模式
* ✅ **扩展性强**：未来Pilot更新自动支持新类型
* ❌ **类型安全**：编译时无法检查特定类型的约束（通过运行时验证解决）
* ❌ **IDE支持**：缺少针对具体类型的智能提示（通过文档补偿）

**技术对比**：
```java
// 传统方式：需要为每个类型写专门代码
@PostMapping("/requirements") // RequirementDefinition专门接口
@PostMapping("/parts")        // PartUsage专门接口  
@PostMapping("/interfaces")   // InterfaceDefinition专门接口
... 182个专门接口

// MVP实现方式：通用接口支持完整CRUD
@GetMapping("/elements")       // 查询所有或按类型过滤
@PostMapping("/elements")      // 创建任意类型元素
@GetMapping("/elements/{id}")  // 获取单个元素
@PutMapping("/elements/{id}")  // 更新元素
@DeleteMapping("/elements/{id}") // 删除元素

// 领域接口作为便利层（可选）
@PostMapping("/requirements")  // 内部调用 universalElementService.createElement("RequirementDefinition", ...)
```

### 4.2 为什么采用完整Pilot元模型而不是简化版？

* ✅ **标准兼容**：100%兼容SysML 2.0标准
* ✅ **继承完整**：EMF自动处理所有继承关系
* ✅ **扩展性强**：未来需要Part、Port等直接可用
* ✅ **维护简单**：不需要自己定义和维护元模型
* ✅ **DTO隔离**：通过DTO层隔离复杂性，API保持简洁
* ❌ **存储开销**：会保存一些暂时不用的字段（可接受）
* ❌ **学习成本**：需要理解Pilot的继承层次（通过文档解决）

### 4.2 为什么选择JSON文件而不是数据库？

* ✅  **简化部署** ：无需数据库安装和配置
* ✅  **零依赖** ：符合MVP快速验证目标
* ✅  **EMF原生** ：直接使用EMF JSON Resource
* ✅  **版本控制友好** ：文件可纳入Git管理
* ✅  **调试简单** ：直接查看JSON文件内容
* ❌  **并发限制** ：文件锁定机制简单（MVP可接受）
* ❌  **查询性能** ：无索引优化（MVP可接受）

### 4.2 继承链处理方案

#### 4.2.1 完整继承层次

Pilot元模型必须保持完整的继承链，从Element到RequirementDefinition：

```
Element (根基类)
  ├─ elementId              // 元素唯一标识
  ├─ qualifiedName          // 完全限定名
  └─ documentation          // 文档
      ↓
NamedElement
  ├─ name (declaredName)    // 元素名称
  └─ shortName (declaredShortName) // 短名称
      ↓
Feature
  ├─ direction              // 方向性
  └─ isAbstract            // 是否抽象
      ↓
Type
  └─ isConjugated          // 是否共轭
      ↓
Classifier
  └─ isSufficient          // 是否充分
      ↓
Definition
  └─ isVariation           // 是否变体
      ↓
OccurrenceDefinition
  └─ isIndividual          // 是否个体
      ↓
ConstraintDefinition
  └─ (同时继承Predicate)
      ↓
RequirementDefinition
  ├─ reqId (redefines declaredShortName) // 需求ID
  ├─ text (derived from documentation)    // 需求文本
  └─ subjectParameter等                   // 主题参数等
```

#### 4.2.2 EMF自动处理继承

```java
// EMF会自动处理所有继承关系
EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");

// 创建实例时，所有继承的属性都可用
EObject reqDef = EcoreUtil.create(reqDefClass);

// 可以访问任何层级的属性
reqDef.eSet(reqDefClass.getEStructuralFeature("elementId"), "R-001");        // 从Element继承
reqDef.eSet(reqDefClass.getEStructuralFeature("declaredName"), "功能需求");   // 从NamedElement继承
reqDef.eSet(reqDefClass.getEStructuralFeature("declaredShortName"), "REQ-001"); // 作为reqId使用
```

#### 4.2.3 DTO选择性映射

```java
@Service
public class RequirementMapper {
    
    // 从完整的EObject映射到简化的DTO
    public RequirementDTO toDTO(EObject reqDef) {
        RequirementDTO dto = new RequirementDTO();
        
        // 只取需要的字段，其他继承字段忽略
        dto.setId(getString(reqDef, "elementId"));           // Element层
        dto.setName(getString(reqDef, "declaredName"));      // NamedElement层
        dto.setReqId(getString(reqDef, "declaredShortName")); // RequirementDefinition层
        
        // 处理派生字段
        EList<EObject> docs = (EList<EObject>) reqDef.eGet("documentation");
        if (!docs.isEmpty()) {
            dto.setText(getString(docs.get(0), "body"));
        }
        
        // 其他继承的字段（如isAbstract、direction等）不映射到DTO
        return dto;
    }
    
    // 从DTO更新EObject（只更新需要的字段）
    public void updateFromDTO(EObject reqDef, RequirementDTO dto) {
        if (dto.getName() != null) {
            reqDef.eSet("declaredName", dto.getName());
        }
        if (dto.getReqId() != null) {
            reqDef.eSet("declaredShortName", dto.getReqId());
        }
        // 不在DTO中的继承字段保持默认值
    }
}
```

### 4.3 Pilot元模型字段映射

#### RequirementDefinition（Pilot标准结构）
```
继承层次：Element → NamedElement → Feature → Type → Classifier → Definition → RequirementDefinition

字段映射：
- id (from Element)              -> 我们的id
- qualifiedName (from Element)   -> 完整路径名
- name (from NamedElement)       -> 我们的name  
- shortName (from NamedElement)  -> 简称
- documentation (from Element)   -> 我们的doc
- reqId (from RequirementDef)    -> 我们的reqId
- text (from RequirementDef)     -> 我们的text
- constraint (from RequirementDef) -> 约束条件
- assumedConstraint              -> 假设条件
- derivedRequirement             -> 派生需求引用
- satisfiedRequirement           -> 满足需求引用
```

#### RequirementUsage（Pilot标准结构）
```
继承层次：Element → NamedElement → Feature → Type → Usage → RequirementUsage

字段映射：
- 继承所有RequirementDefinition的字段
- definition (from Usage)        -> 指向的Definition引用
- variant (from Usage)           -> 变体信息
- variation (from Usage)         -> 变化信息
```

#### Trace/Dependency（Pilot标准结构）
```
继承层次：Element → Relationship → Dependency

字段映射：
- source (from Relationship)     -> 我们的fromId
- target (from Relationship)     -> 我们的toId  
- kind (from Dependency)         -> 我们的type（derive/satisfy/refine/trace）
```

### 4.2 为什么不用Spring Data REST？

* ✅  **控制力强** ：手写Controller便于调试
* ✅  **定制灵活** ：特殊逻辑容易实现
* ✅  **学习成本低** ：团队容易理解

### 4.3 为什么不用WebSocket？

* ✅  **简化架构** ：无需维护长连接
* ✅  **部署简单** ：不需要特殊网关配置
* ✅  **MVP够用** ：轮询/手动刷新可接受

---

## 5. 性能设计

### 5.1 性能指标（MVP降低标准）

| 指标     | 目标值    | 测量方法        |
| -------- | --------- | --------------- |
| 模型规模 | ≤500节点 | 超出提示用户    |
| API响应  | <500ms    | 开发机测试      |
| 页面切换 | <1s       | Chrome DevTools |
| 导入导出 | <5s       | 500节点文件     |

### 5.2 性能瓶颈与缓解

```yaml
瓶颈1: JSON文件IO
缓解: 内存缓存Resource，批量保存

瓶颈2: React Flow渲染
缓解: 虚拟化、懒加载、视口裁剪

瓶颈3: EMF序列化
缓解: 限制模型深度，避免大对象

瓶颈4: 并发文件访问
缓解: 文件锁机制，错误重试
```

---

## 6. 安全设计（MVP最简）

### 6.1 基础安全

* **路径遍历** ：严格校验项目ID格式
* **文件权限** ：限制数据目录访问权限
* **XSS防护** ：React自动转义
* **CSRF** ：暂不实现（单机使用）
* **认证授权** ：暂不实现（MVP无需）

### 6.2 数据校验

```java
// 入参校验 - Spring Validation
@PostMapping("/requirements")
public ResponseEntity create(@Valid @RequestBody CreateRequest req) {
    // @NotBlank, @Size等注解校验
}

// 安全校验 - 路径遍历防护
private void validateProjectId(String projectId) {
    if (!projectId.matches("^[a-zA-Z0-9_-]+$")) {
        throw new SecurityException("Invalid project ID format");
    }
}
```

---

## 7. 部署架构

### 7.1 开发环境（简化）

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data  # JSON文件存储
    environment:
      SPRING_PROFILES_ACTIVE: dev

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    environment:
      REACT_APP_API_URL: http://localhost:8080
```

### 7.2 生产部署（单机）

```bash
# 最简部署 - 单个服务器
/opt/sysml/
├── backend.jar       # Spring Boot fat jar
├── frontend/         # 静态文件
├── data/             # JSON数据目录
│   └── projects/     # 项目文件
└── start.sh         # 启动脚本
```

### 7.3 文件系统结构

```
data/
├── projects/
│   ├── proj-001/
│   │   ├── model.json          # EMF Resource JSON
│   │   └── metadata.json       # 项目元数据
│   └── proj-002/
│       ├── model.json
│       └── metadata.json
└── demo/
    ├── demo-project.json       # 演示数据
    ├── small-dataset.json      # 小数据集(10)
    ├── medium-dataset.json     # 中数据集(100)
    └── large-dataset.json      # 大数据集(500)
```

---

## 8. 技术风险

### 8.1 风险清单

| 风险                | 概率 | 影响 | 缓解措施                |
| ------------------- | ---- | ---- | ----------------------- |
| EMF学习曲线陡峭     | 高   | 高   | 提供示例代码，团队培训  |
| 文件IO性能差        | 中   | 中   | 内存缓存，异步保存      |
| 文件锁冲突          | 中   | 中   | 重试机制，错误处理      |
| React Flow大图崩溃  | 中   | 高   | 节点数限制，虚拟化      |
| 循环引用死锁        | 低   | 高   | $ref机制，最大深度限制  |

### 8.2 风险监控

```java
// 性能监控点
@Component
public class PerformanceMonitor {
  
    @EventListener
    public void onModelLoad(ModelLoadEvent event) {
        if (event.getNodeCount() > 500) {
            log.warn("Large model loaded: {} nodes", event.getNodeCount());
        }
    }
  
    @Scheduled(fixedDelay = 60000)
    public void checkDataSize() {
        long size = getDataDirectorySize();
        if (size > 100_000_000) { // 100MB
            log.warn("Data directory size exceeds 100MB: {}", size);
        }
    }
}
```

---

## 9. 技术债务记录

### 9.1 MVP技术债务清单

```markdown
## 必须在v1.0解决
1. [ ] 添加基础认证授权
2. [ ] 文件并发访问优化
3. [ ] 前端状态管理规范化
4. [ ] 错误处理统一化

## 可以延后解决
1. [ ] 内存缓存优化
2. [ ] 查询DSL设计
3. [ ] WebSocket实时同步
4. [ ] 完整EMF模型支持
5. [ ] 数据库迁移路径
```

### 9.2 升级路径规划

```
MVP (v0.1) → 性能优化 (v0.2) → 多用户 (v0.3) → 完整模型 (v1.0)
     ↓              ↓               ↓              ↓
   4周内         +2周            +4周          +8周
                              可选：迁移到数据库
```

---

## 10. 开发规范

### 10.1 代码结构（分层组织）

```
backend/
├── controller/                         # 表现层
│   ├── AdvancedQueryController.java   # 通用元素REST接口
│   ├── RequirementController.java     # 需求领域REST接口
│   ├── TraceController.java          # 追溯领域REST接口
│   ├── ProjectController.java        # 项目管理接口
│   └── HealthController.java         # 健康检查
│
├── service/                           
│   ├── domain/                       # 领域业务层（不依赖EMF）
│   │   ├── RequirementService.java   # 需求业务逻辑
│   │   ├── TraceService.java        # 追溯业务逻辑
│   │   ├── ValidationService.java   # 业务规则验证
│   │   └── ProjectService.java      # 项目管理逻辑
│   │
│   └── infrastructure/               # 基础设施层（EMF操作）
│       ├── UniversalElementService.java  # 通用EMF操作
│       ├── PilotEMFService.java         # EMF工厂和元模型
│       └── DemoDataGenerator.java       # 演示数据生成
│
├── repository/                        # 持久化层
│   └── FileModelRepository.java      # EMF Resource文件操作
│
├── model/                            # 元模型注册
│   └── EMFModelRegistry.java        # SysML Pilot元模型
│
├── dto/                              # 数据传输对象
│   ├── ElementDTO.java              # 通用元素DTO
│   ├── RequirementDTO.java          # 需求DTO
│   └── TraceDTO.java               # 追溯DTO
│
└── exception/                        # 异常处理
    ├── BusinessException.java       # 业务异常
    └── TechnicalException.java      # 技术异常

frontend/
├── components/     # UI组件
├── services/       # API调用
├── contexts/       # 状态管理
├── utils/          # 工具函数
└── types/          # TypeScript类型
```

**分层原则**：
1. **domain包**：纯业务逻辑，不import EMF类
2. **infrastructure包**：所有EMF操作和技术实现
3. **controller**：只做参数转换和响应封装
4. **repository**：只在infrastructure层使用

### 10.2 命名规范

* **REST URL** : 小写，复数，连字符 `/api/v1/elements`（通用接口）
* **Java类** : PascalCase `RequirementService`
* **TypeScript** : PascalCase组件，camelCase函数
* **文件名** : 小写下划线 `model.json`, `metadata.json`

---

## 附录A：技术栈版本锁定

```xml
<!-- pom.xml -->
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.0</spring-boot.version>
    <emf.version>2.35.0</emf.version>
    <sirius.emfjson.version>2.5.3</sirius.emfjson.version>
    <!-- 移除PostgreSQL依赖 -->
</properties>

<dependencies>
    <!-- EMF JSON支持 - 使用sirius-emfjson替代emfjson-jackson -->
    <dependency>
        <groupId>org.eclipse.sirius</groupId>
        <artifactId>sirius-emfjson</artifactId>
        <version>${sirius.emfjson.version}</version>
    </dependency>
    <!-- 其他依赖... -->
</dependencies>
```

```json
// package.json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-flow-renderer": "^11.10.0",
    "antd": "^5.12.0",
    "typescript": "^5.3.0"
  }
}
```

---