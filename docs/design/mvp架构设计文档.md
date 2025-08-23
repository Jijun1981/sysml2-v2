# SysML v2 建模平台 MVP 架构设计文档

## 文档信息

* **版本** : 1.1
* **日期** : 2025-01
* **状态** : MVP架构定稿（对齐需求文档）
* **关联文档** : 《MVP需求书v2.1》、《API接口文档v1.0》

---

## 1. 架构概述

### 1.1 MVP架构原则

* **最小可行** ：仅实现核心需求，不过度设计
* **技术收敛** ：单体架构，文件即模型，单一图形库
* **快速验证** ：4周可交付，技术风险可控
* **扩展预留** ：版本字段、时间戳、ID稳定性

### 1.2 技术边界（明确不做）

* ❌  **不做** ：分布式架构、微服务、消息队列
* ❌  **不做** ：复杂权限、多租户、审计日志
* ❌  **不做** ：实时协作、WebSocket、服务端推送（SSE除外）
* ❌  **不做** ：计算节点执行引擎（MVP硬编码校验）
* ❌  **不做** ：完整SysML v2元模型（仅3个核心类）
* ❌  **不做** ：性能优化（索引、缓存、查询优化）
* ❌  **不做** ：数据库依赖（PostgreSQL等）

---

## 2. 技术架构

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
│  │         Spring Boot 3.2 + Java 17                │  │
│  ├──────────────────────────────────────────────────┤  │
│  │  Controller → Service → FileRepository           │  │
│  │  EMF Core:  最小化 EPackage (3 EClass)          │  │
│  │  Validation: 硬编码3条规则                       │  │
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

#### 2.2.1 EMF模型（极简）

```java
// 仅注册3个EClass，不依赖SysML Pilot
@Component
public class EMFModelRegistry {
  
    @PostConstruct
    public void registerLocalEPackage() {
        EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
        pkg.setName("sysml");
        pkg.setNsPrefix("sysml");
        pkg.setNsURI("urn:your:sysml2");  // 本地URI
      
        // 仅3个类
        EClass reqDef = createRequirementDefinition();
        EClass reqUsage = createRequirementUsage();
        EClass trace = createTrace();
      
        pkg.getEClassifiers().addAll(List.of(reqDef, reqUsage, trace));
        EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg);
    }
}
```

#### 2.2.2 持久化设计（JSON文件）

```java
// 替换数据库为文件系统存储
@Component
public class FileModelRepository {
    
    private static final String DATA_ROOT = "data/projects";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ResourceSet> resourceCache = new ConcurrentHashMap<>();
    
    // 项目文件路径: data/projects/{projectId}/model.json
    private Path getProjectPath(String projectId) {
        return Paths.get(DATA_ROOT, projectId, "model.json");
    }
    
    // 加载项目模型
    public Resource loadProject(String projectId) {
        Path projectPath = getProjectPath(projectId);
        if (!Files.exists(projectPath)) {
            return createEmptyProject(projectId);
        }
        
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource resource = resourceSet.createResource(
            URI.createFileURI(projectPath.toString()));
        
        try {
            resource.load(Collections.emptyMap());
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
            
            resource.save(Collections.emptyMap());
            
            // 更新时间戳
            updateProjectTimestamp(projectId);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to save project: " + projectId, e);
        }
    }
    
    // 查找特定类型的对象
    public List<EObject> findByType(String projectId, String eClassName) {
        Resource resource = loadProject(projectId);
        return resource.getContents().stream()
            .filter(obj -> eClassName.equals(obj.eClass().getName()))
            .collect(Collectors.toList());
    }
}
```

#### 2.2.3 服务层设计（简化）

```java
@Service
public class RequirementService {
    
    @Autowired
    private FileModelRepository repository;
    
    @Autowired
    private EMFModelFactory modelFactory;
    
    // 核心CRUD - 直接操作EMF Resource
    public RequirementDefinition createDefinition(CreateRequest req) {
        Resource resource = repository.loadProject(req.getProjectId());
        
        // 1. 创建EMF对象
        RequirementDefinition definition = modelFactory.createRequirementDefinition();
        definition.setId(generateId("R-"));
        definition.setReqId(req.getReqId());
        definition.setName(req.getName());
        definition.setText(req.getText());
        definition.setCreatedAt(Instant.now());
        definition.setUpdatedAt(Instant.now());
        
        // 2. 添加到Resource
        resource.getContents().add(definition);
        
        // 3. 保存文件
        repository.saveProject(req.getProjectId(), resource);
        
        return definition;
    }
    
    // 查询 - 从文件系统加载
    public List<RequirementDefinition> findDefinitions(String projectId) {
        return repository.findByType(projectId, "RequirementDefinition")
            .stream()
            .map(obj -> (RequirementDefinition) obj)
            .collect(Collectors.toList());
    }
    
    // 更新
    public RequirementDefinition updateDefinition(String projectId, String id, UpdateRequest req) {
        Resource resource = repository.loadProject(projectId);
        RequirementDefinition definition = findById(resource, id);
        
        if (definition != null) {
            definition.setName(req.getName());
            definition.setText(req.getText());
            definition.setUpdatedAt(Instant.now());
            repository.saveProject(projectId, resource);
        }
        
        return definition;
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
  
    // 操作
    createRequirement: (req: CreateRequest) => Promise<void>;
    updateRequirement: (id: string, updates: any) => Promise<void>;
    deleteRequirement: (id: string) => Promise<void>;
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

```
用户操作 → React组件 → Fetch API → Spring Controller 
    ↓                                      ↓
状态更新 ← JSON响应 ← Service处理 ← FileRepository
    ↓                     ↓                ↓
视图刷新              EMF操作         JSON文件
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
        
        // 收集所有RequirementDefinition的reqId
        resource.getContents().stream()
            .filter(RequirementDefinition.class::isInstance)
            .map(RequirementDefinition.class::cast)
            .forEach(def -> {
                reqIdMap.computeIfAbsent(def.getReqId(), k -> new ArrayList<>()).add(def);
            });
        
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

### 4.1 为什么选择JSON文件而不是数据库？

* ✅  **简化部署** ：无需数据库安装和配置
* ✅  **零依赖** ：符合MVP快速验证目标
* ✅  **EMF原生** ：直接使用EMF JSON Resource
* ✅  **版本控制友好** ：文件可纳入Git管理
* ✅  **调试简单** ：直接查看JSON文件内容
* ❌  **并发限制** ：文件锁定机制简单（MVP可接受）
* ❌  **查询性能** ：无索引优化（MVP可接受）

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

### 10.1 代码结构

```
backend/
├── controller/     # REST端点
├── service/        # 业务逻辑
├── repository/     # 文件系统数据访问
├── model/          # EMF模型
├── dto/            # 传输对象
└── exception/      # 异常处理

frontend/
├── components/     # UI组件
├── services/       # API调用
├── contexts/       # 状态管理
├── utils/          # 工具函数
└── types/          # TypeScript类型
```

### 10.2 命名规范

* **REST URL** : 小写，复数，连字符 `/projects/{pid}/requirements`
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
    <!-- 移除PostgreSQL依赖 -->
</properties>
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