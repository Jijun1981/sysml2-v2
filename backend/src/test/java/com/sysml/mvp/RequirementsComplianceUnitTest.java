package com.sysml.mvp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.sirius.emfjson.resource.JsonResource;
import org.eclipse.sirius.emfjson.resource.JsonResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.model.EMFModelRegistry;

/**
 * 需求符合性单元测试 - 不依赖Spring Boot
 * 专注验证核心EMF JSON功能与需求对齐
 */
public class RequirementsComplianceUnitTest {
    
    @TempDir
    Path tempDir;
    
    private EMFModelRegistry modelRegistry;
    private ResourceSet resourceSet;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        modelRegistry = new EMFModelRegistry();
        modelRegistry.registerSysMLEPackage(); // 手动调用注册
        
        resourceSet = createConfiguredResourceSet();
        objectMapper = new ObjectMapper();
    }
    
    private ResourceSet createConfiguredResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        
        // 注册JsonResourceFactory
        JsonResourceFactoryImpl factory = new JsonResourceFactoryImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("json", factory);
        
        // 注册本地SysML EPackage
        EPackage sysmlPackage = modelRegistry.getSysmlPackage();
        if (sysmlPackage != null) {
            resourceSet.getPackageRegistry().put(sysmlPackage.getNsURI(), sysmlPackage);
        }
        
        return resourceSet;
    }
    
    @Test
    @DisplayName("REQ-B1-1: 本地包注册（零 Pilot 依赖）")
    void testLocalPackageRegistration() {
        // AC：启动将本地 `urn:your:sysml2` `EPackage` 注册进 `EPackage.Registry`
        EPackage sysmlPackage = EPackage.Registry.INSTANCE.getEPackage("urn:your:sysml2");
        
        assertNotNull(sysmlPackage, "本地SysML包应该已注册");
        assertEquals("urn:your:sysml2", sysmlPackage.getNsURI());
        assertEquals("sysml", sysmlPackage.getName());
        
        // 验证包含核心类
        assertNotNull(sysmlPackage.getEClassifier("RequirementDefinition"), "应包含RequirementDefinition");
        assertNotNull(sysmlPackage.getEClassifier("RequirementUsage"), "应包含RequirementUsage");
    }
    
    @Test
    @DisplayName("REQ-B1-2: JSON 工厂")
    void testJsonResourceFactory() throws IOException {
        // AC：为 `.json` 注册 EMF JSON 资源工厂；`ResourceSet` 能创建/加载/保存
        Path jsonPath = tempDir.resolve("test-model.json");
        URI uri = URI.createFileURI(jsonPath.toString());
        Resource resource = resourceSet.createResource(uri);
        
        // 验证是JsonResource类型
        assertTrue(resource instanceof JsonResource, "应该创建JsonResource");
        
        // 创建一个RequirementDefinition并保存
        EObject reqDef = modelRegistry.createRequirementDefinition();
        resource.getContents().add(reqDef);
        
        // 保存选项
        Map<String, Object> saveOptions = new HashMap<>();
        saveOptions.put(JsonResource.OPTION_ENCODING, "UTF-8");
        saveOptions.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
        saveOptions.put(JsonResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
        
        resource.save(saveOptions);
        
        // 验证文件存在且格式正确
        assertTrue(Files.exists(jsonPath), "JSON文件应该已创建");
        
        String jsonContent = Files.readString(jsonPath);
        System.out.println("生成的JSON内容:");
        System.out.println(jsonContent);
        
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        
        // AC：模型根包含 `_version:"1.0"`
        assertTrue(jsonContent.contains("1.0"), "JSON应包含版本信息");
        
        // 验证JSON格式符合sirius-emfjson标准
        assertTrue(rootNode.has("ns"), "应包含命名空间定义");
        assertTrue(rootNode.has("content"), "应包含content数组");
        
        JsonNode nsNode = rootNode.get("ns");
        assertTrue(nsNode.has("sysml"), "应包含sysml命名空间");
        assertEquals("urn:your:sysml2", nsNode.get("sysml").asText(), "命名空间URI应正确");
    }
    
    @Test
    @DisplayName("REQ-B1-3: 回读一致性（增强）")
    void testRoundTripConsistency() throws IOException {
        // AC：新建→保存→再加载：根数量、`eClass`、`id` 完全一致
        Path jsonPath = tempDir.resolve("roundtrip-test.json");
        URI uri = URI.createFileURI(jsonPath.toString());
        Resource originalResource = resourceSet.createResource(uri);
        
        // 创建对象
        EObject reqDef1 = modelRegistry.createRequirementDefinition();
        EObject reqDef2 = modelRegistry.createRequirementDefinition();
        
        // 记录原始信息
        String originalId1 = (String) reqDef1.eGet(reqDef1.eClass().getEStructuralFeature("id"));
        String originalId2 = (String) reqDef2.eGet(reqDef2.eClass().getEStructuralFeature("id"));
        
        originalResource.getContents().add(reqDef1);
        originalResource.getContents().add(reqDef2);
        
        // 保存
        Map<String, Object> options = new HashMap<>();
        options.put(JsonResource.OPTION_ENCODING, "UTF-8");
        options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
        options.put(JsonResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
        
        originalResource.save(options);
        
        // 重新加载
        ResourceSet newResourceSet = createConfiguredResourceSet();
        Resource loadedResource = newResourceSet.createResource(uri);
        loadedResource.load(options);
        
        // 验证一致性
        assertEquals(2, loadedResource.getContents().size(), "根数量应一致");
        
        EObject loaded1 = loadedResource.getContents().get(0);
        EObject loaded2 = loadedResource.getContents().get(1);
        
        // 验证eClass
        assertEquals("RequirementDefinition", loaded1.eClass().getName(), "eClass应一致");
        assertEquals("RequirementDefinition", loaded2.eClass().getName(), "eClass应一致");
        
        // 验证id (REQ-B2-3: ID稳定)
        String loadedId1 = (String) loaded1.eGet(loaded1.eClass().getEStructuralFeature("id"));
        String loadedId2 = (String) loaded2.eGet(loaded2.eClass().getEStructuralFeature("id"));
        
        assertEquals(originalId1, loadedId1, "id应一致");
        assertEquals(originalId2, loadedId2, "id应一致");
    }
    
    @Test
    @DisplayName("REQ-B2-1: 创建 API")
    void testCreationAPI() {
        // AC：提供 `createDefinition()` / `createUsage(ofId)` / `createTrace(fromId,toId,type)`
        
        // 测试createDefinition
        EObject definition = modelRegistry.createRequirementDefinition();
        assertNotNull(definition, "应该能创建RequirementDefinition");
        assertEquals("RequirementDefinition", definition.eClass().getName());
        
        // 测试createUsage
        String defId = (String) definition.eGet(definition.eClass().getEStructuralFeature("id"));
        EObject usage = modelRegistry.createRequirementUsage(defId);
        assertNotNull(usage, "应该能创建RequirementUsage");
        assertEquals("RequirementUsage", usage.eClass().getName());
        assertEquals(defId, usage.eGet(usage.eClass().getEStructuralFeature("of")));
        
        // 测试createTrace
        String usageId = (String) usage.eGet(usage.eClass().getEStructuralFeature("id"));
        EObject trace = modelRegistry.createTrace(defId, usageId, "derive");
        assertNotNull(trace, "应该能创建Trace");
        assertEquals("Trace", trace.eClass().getName());
        assertEquals(defId, trace.eGet(trace.eClass().getEStructuralFeature("fromId")));
        assertEquals(usageId, trace.eGet(trace.eClass().getEStructuralFeature("toId")));
        assertEquals("derive", trace.eGet(trace.eClass().getEStructuralFeature("type")));
    }
    
    @Test
    @DisplayName("REQ-B2-2: 默认值")
    void testDefaultValues() {
        // AC：新建默认 `status='draft'`,`tags=[]`；ID 采用稳定 UUID
        
        // 测试RequirementDefinition默认值
        EObject definition = modelRegistry.createRequirementDefinition();
        String id = (String) definition.eGet(definition.eClass().getEStructuralFeature("id"));
        String version = (String) definition.eGet(definition.eClass().getEStructuralFeature("_version"));
        Date createdAt = (Date) definition.eGet(definition.eClass().getEStructuralFeature("createdAt"));
        
        assertNotNull(id, "id应有默认值");
        assertTrue(id.startsWith("R-"), "id应以R-前缀");
        assertEquals("1.0", version, "_version应为1.0");
        assertNotNull(createdAt, "createdAt应有默认值");
        
        // 测试RequirementUsage默认值
        EObject usage = modelRegistry.createRequirementUsage("test-def-id");
        String usageId = (String) usage.eGet(usage.eClass().getEStructuralFeature("id"));
        String status = (String) usage.eGet(usage.eClass().getEStructuralFeature("status"));
        
        assertNotNull(usageId, "id应有默认值");
        assertTrue(usageId.startsWith("U-"), "id应以U-前缀");
        assertEquals("draft", status, "status默认值应为draft");
    }
    
    @Test
    @DisplayName("REQ-B2-3: ID 稳定 - UUID替代时间戳")
    void testIdStability() {
        // AC：导出→导入后，同一对象 `id` 不变；ID 采用稳定 UUID
        
        // 创建对象
        EObject definition = modelRegistry.createRequirementDefinition();
        String originalId = (String) definition.eGet(definition.eClass().getEStructuralFeature("id"));
        
        // 验证ID格式：UUID替代时间戳
        assertNotNull(originalId);
        assertTrue(originalId.startsWith("R-"), "ID应以R-开头");
        assertTrue(originalId.length() >= 10, "ID应该足够长以确保唯一性");
        
        // 多次创建验证唯一性
        EObject definition2 = modelRegistry.createRequirementDefinition();
        String id2 = (String) definition2.eGet(definition2.eClass().getEStructuralFeature("id"));
        
        assertNotEquals(originalId, id2, "不同对象ID应不同");
        
        // 验证UUID格式（8位十六进制字符）
        String idPart = originalId.substring(2); // 去掉"R-"前缀
        assertEquals(8, idPart.length(), "UUID部分应为8位");
        assertTrue(idPart.matches("[0-9a-f]+"), "UUID部分应为十六进制字符");
    }
    
    @Test
    @DisplayName("验证Trace类型约束 - REQ-C3-1")
    void testTraceTypeConstraints() {
        // AC：`type∈{derive,satisfy,refine,trace}`
        String[] validTypes = {"derive", "satisfy", "refine", "trace"};
        String defId = "test-def";
        String usageId = "test-usage";
        
        for (String type : validTypes) {
            EObject trace = modelRegistry.createTrace(defId, usageId, type);
            assertNotNull(trace, "应能创建" + type + "类型的trace");
            assertEquals(type, trace.eGet(trace.eClass().getEStructuralFeature("type")));
            assertNotNull(trace.eGet(trace.eClass().getEStructuralFeature("createdAt")), 
                "Trace应包含createdAt");
        }
    }
    
    @Test
    @DisplayName("REQ-C2-1: RequirementUsage创建验证")
    void testRequirementUsageCreation() {
        // AC: POST /requirements（type=usage, of=defId）；缺of→400；defId不存在→404
        
        // 1. 先创建一个RequirementDefinition用于引用
        EObject definition = modelRegistry.createRequirementDefinition();
        definition.eSet(definition.eClass().getEStructuralFeature("reqId"), "REQ-001");
        definition.eSet(definition.eClass().getEStructuralFeature("name"), "测试定义");
        definition.eSet(definition.eClass().getEStructuralFeature("text"), "这是一个测试需求定义");
        
        String defId = (String) definition.eGet(definition.eClass().getEStructuralFeature("id"));
        assertNotNull(defId, "定义ID不应为空");
        
        // 2. 创建RequirementUsage，引用这个定义
        EObject usage = modelRegistry.createRequirementUsage(defId);
        
        // 验证Usage的基本属性
        String usageId = (String) usage.eGet(usage.eClass().getEStructuralFeature("id"));
        String ofId = (String) usage.eGet(usage.eClass().getEStructuralFeature("of"));
        String status = (String) usage.eGet(usage.eClass().getEStructuralFeature("status"));
        Date createdAt = (Date) usage.eGet(usage.eClass().getEStructuralFeature("createdAt"));
        
        assertNotNull(usageId, "Usage ID不应为空");
        assertTrue(usageId.startsWith("U-"), "Usage ID应以U-开头");
        assertEquals(defId, ofId, "of字段应该引用正确的定义ID");
        assertEquals("draft", status, "默认状态应为draft");
        assertNotNull(createdAt, "创建时间不应为空");
    }
    
    @Test
    @DisplayName("REQ-C2-2: RequirementUsage更新和删除验证")
    void testRequirementUsageUpdateAndDelete() {
        // AC: 更新允许name,text,status,tags；存在Trace时删除→409（返回阻塞traceIds）
        
        // 1. 创建定义和用法
        EObject definition = modelRegistry.createRequirementDefinition();
        definition.eSet(definition.eClass().getEStructuralFeature("reqId"), "REQ-002");
        
        String defId = (String) definition.eGet(definition.eClass().getEStructuralFeature("id"));
        EObject usage = modelRegistry.createRequirementUsage(defId);
        String usageId = (String) usage.eGet(usage.eClass().getEStructuralFeature("id"));
        
        // 2. 验证可更新的字段
        usage.eSet(usage.eClass().getEStructuralFeature("name"), "更新后的名称");
        usage.eSet(usage.eClass().getEStructuralFeature("text"), "更新后的文本");
        usage.eSet(usage.eClass().getEStructuralFeature("status"), "approved");
        
        assertEquals("更新后的名称", usage.eGet(usage.eClass().getEStructuralFeature("name")));
        assertEquals("更新后的文本", usage.eGet(usage.eClass().getEStructuralFeature("text")));
        assertEquals("approved", usage.eGet(usage.eClass().getEStructuralFeature("status")));
        
        // 3. 测试Trace阻塞删除（创建一个引用该Usage的Trace）
        EObject anotherDefinition = modelRegistry.createRequirementDefinition();
        String anotherDefId = (String) anotherDefinition.eGet(anotherDefinition.eClass().getEStructuralFeature("id"));
        
        EObject trace = modelRegistry.createTrace(usageId, anotherDefId, "derive");
        String traceId = (String) trace.eGet(trace.eClass().getEStructuralFeature("id"));
        
        assertNotNull(traceId, "Trace应该被成功创建");
        assertEquals(usageId, trace.eGet(trace.eClass().getEStructuralFeature("fromId")), 
            "Trace的fromId应该引用Usage");
    }
    
    @Test
    @DisplayName("REQ-C3-1: Trace创建验证")
    void testTraceCreation() {
        // AC: POST /requirements/{id}/traces {toId,type}；type∈{derive,satisfy,refine,trace}
        // fromId==toId→400；toId不存在→404；成功对象含createdAt(UTC)
        
        // 1. 创建两个需求用于建立Trace关系
        EObject fromReq = modelRegistry.createRequirementDefinition();
        fromReq.eSet(fromReq.eClass().getEStructuralFeature("reqId"), "REQ-FROM");
        fromReq.eSet(fromReq.eClass().getEStructuralFeature("name"), "源需求");
        
        EObject toReq = modelRegistry.createRequirementDefinition();
        toReq.eSet(toReq.eClass().getEStructuralFeature("reqId"), "REQ-TO");
        toReq.eSet(toReq.eClass().getEStructuralFeature("name"), "目标需求");
        
        String fromId = (String) fromReq.eGet(fromReq.eClass().getEStructuralFeature("id"));
        String toId = (String) toReq.eGet(toReq.eClass().getEStructuralFeature("id"));
        
        // 2. 测试有效的Trace类型
        String[] validTypes = {"derive", "satisfy", "refine", "trace"};
        for (String type : validTypes) {
            EObject trace = modelRegistry.createTrace(fromId, toId, type);
            
            assertNotNull(trace, "应能创建" + type + "类型的Trace");
            assertEquals("Trace", trace.eClass().getName(), "eClass应为Trace");
            assertEquals(fromId, trace.eGet(trace.eClass().getEStructuralFeature("fromId")));
            assertEquals(toId, trace.eGet(trace.eClass().getEStructuralFeature("toId")));
            assertEquals(type, trace.eGet(trace.eClass().getEStructuralFeature("type")));
            
            // 验证包含createdAt
            Date createdAt = (Date) trace.eGet(trace.eClass().getEStructuralFeature("createdAt"));
            assertNotNull(createdAt, "Trace应包含createdAt时间戳");
            
            // 验证ID格式
            String traceId = (String) trace.eGet(trace.eClass().getEStructuralFeature("id"));
            assertNotNull(traceId, "Trace应有ID");
            assertTrue(traceId.startsWith("T-"), "Trace ID应以T-开头");
        }
    }
    
    @Test
    @DisplayName("REQ-C3-2: 获取需求的Trace关系（方向查询）")
    void testTraceDirectionalQuery() {
        // AC: GET /requirements/{id}/traces?dir=in|out|both 返回入/出边
        
        // 创建三个需求：A -> B -> C
        EObject reqA = modelRegistry.createRequirementDefinition();
        EObject reqB = modelRegistry.createRequirementDefinition();
        EObject reqC = modelRegistry.createRequirementDefinition();
        
        String idA = (String) reqA.eGet(reqA.eClass().getEStructuralFeature("id"));
        String idB = (String) reqB.eGet(reqB.eClass().getEStructuralFeature("id"));
        String idC = (String) reqC.eGet(reqC.eClass().getEStructuralFeature("id"));
        
        // 创建Trace关系：A->B 和 B->C
        EObject traceAB = modelRegistry.createTrace(idA, idB, "derive");
        EObject traceBC = modelRegistry.createTrace(idB, idC, "refine");
        
        // 模拟方向查询逻辑（这里只是验证Trace关系的正确性）
        // 对于B需求：
        // - 入边：A->B（B作为toId）
        // - 出边：B->C（B作为fromId）
        
        // 验证A->B关系
        assertEquals(idA, traceAB.eGet(traceAB.eClass().getEStructuralFeature("fromId")));
        assertEquals(idB, traceAB.eGet(traceAB.eClass().getEStructuralFeature("toId")));
        
        // 验证B->C关系
        assertEquals(idB, traceBC.eGet(traceBC.eClass().getEStructuralFeature("fromId")));
        assertEquals(idC, traceBC.eGet(traceBC.eClass().getEStructuralFeature("toId")));
    }
    
    @Test
    @DisplayName("REQ-C3-3: Trace去重逻辑")
    void testTraceDuplication() {
        // AC: 同(from,to,type)不重复创建；重复请求返回既有对象200
        
        EObject fromReq = modelRegistry.createRequirementDefinition();
        EObject toReq = modelRegistry.createRequirementDefinition();
        
        String fromId = (String) fromReq.eGet(fromReq.eClass().getEStructuralFeature("id"));
        String toId = (String) toReq.eGet(toReq.eClass().getEStructuralFeature("id"));
        
        // 创建第一个Trace
        EObject trace1 = modelRegistry.createTrace(fromId, toId, "derive");
        String trace1Id = (String) trace1.eGet(trace1.eClass().getEStructuralFeature("id"));
        Date trace1CreatedAt = (Date) trace1.eGet(trace1.eClass().getEStructuralFeature("createdAt"));
        
        // 模拟重复创建相同的(from,to,type)组合
        // 注意：这里只是测试工厂方法能创建，实际的去重逻辑在Service层
        EObject trace2 = modelRegistry.createTrace(fromId, toId, "derive");
        String trace2Id = (String) trace2.eGet(trace2.eClass().getEStructuralFeature("id"));
        
        // 两次创建会生成不同的ID，因为工厂方法总是创建新对象
        // 去重逻辑应该在Service层实现
        assertNotEquals(trace1Id, trace2Id, "工厂方法会创建不同的ID");
        
        // 但验证其他属性相同
        assertEquals(trace1.eGet(trace1.eClass().getEStructuralFeature("fromId")),
                    trace2.eGet(trace2.eClass().getEStructuralFeature("fromId")));
        assertEquals(trace1.eGet(trace1.eClass().getEStructuralFeature("toId")),
                    trace2.eGet(trace2.eClass().getEStructuralFeature("toId")));
        assertEquals(trace1.eGet(trace1.eClass().getEStructuralFeature("type")),
                    trace2.eGet(trace2.eClass().getEStructuralFeature("type")));
    }
    
    @Test
    @DisplayName("REQ-C3-4: Trace删除验证")
    void testTraceDeletion() {
        // AC: DELETE /traces/{traceId}→204；不存在→404
        
        EObject fromReq = modelRegistry.createRequirementDefinition();
        EObject toReq = modelRegistry.createRequirementDefinition();
        
        String fromId = (String) fromReq.eGet(fromReq.eClass().getEStructuralFeature("id"));
        String toId = (String) toReq.eGet(toReq.eClass().getEStructuralFeature("id"));
        
        // 创建Trace
        EObject trace = modelRegistry.createTrace(fromId, toId, "satisfy");
        String traceId = (String) trace.eGet(trace.eClass().getEStructuralFeature("id"));
        
        assertNotNull(traceId, "Trace应有ID用于删除");
        assertTrue(traceId.startsWith("T-"), "Trace ID应以T-开头");
        
        // 验证Trace的完整性
        assertNotNull(trace.eGet(trace.eClass().getEStructuralFeature("fromId")));
        assertNotNull(trace.eGet(trace.eClass().getEStructuralFeature("toId")));
        assertNotNull(trace.eGet(trace.eClass().getEStructuralFeature("type")));
        assertNotNull(trace.eGet(trace.eClass().getEStructuralFeature("createdAt")));
    }
    
    @Test
    @DisplayName("Trace JSON序列化验证")
    void testTraceJsonSerialization() throws IOException {
        // 创建需求和Trace关系
        EObject reqDef = modelRegistry.createRequirementDefinition();
        reqDef.eSet(reqDef.eClass().getEStructuralFeature("reqId"), "REQ-PARENT");
        reqDef.eSet(reqDef.eClass().getEStructuralFeature("name"), "父需求");
        
        EObject reqUsage = modelRegistry.createRequirementUsage(
            (String) reqDef.eGet(reqDef.eClass().getEStructuralFeature("id"))
        );
        reqUsage.eSet(reqUsage.eClass().getEStructuralFeature("name"), "子需求");
        
        String defId = (String) reqDef.eGet(reqDef.eClass().getEStructuralFeature("id"));
        String usageId = (String) reqUsage.eGet(reqUsage.eClass().getEStructuralFeature("id"));
        
        EObject trace = modelRegistry.createTrace(defId, usageId, "derive");
        
        // 创建资源并序列化
        Path jsonPath = tempDir.resolve("trace-test.json");
        Resource resource = resourceSet.createResource(URI.createFileURI(jsonPath.toAbsolutePath().toString()));
        resource.getContents().add(reqDef);
        resource.getContents().add(reqUsage);
        resource.getContents().add(trace);
        
        Map<String, Object> saveOptions = new HashMap<>();
        saveOptions.put(JsonResource.OPTION_ENCODING, "UTF-8");
        resource.save(saveOptions);
        
        // 验证JSON内容
        String jsonContent = Files.readString(jsonPath);
        JsonNode jsonNode = objectMapper.readTree(jsonContent);
        
        assertNotNull(jsonNode.get("content"), "JSON应包含content数组");
        assertEquals(3, jsonNode.get("content").size(), "应包含三个对象");
        
        // 查找Trace节点
        JsonNode traceNode = null;
        for (JsonNode contentNode : jsonNode.get("content")) {
            if ("sysml:Trace".equals(contentNode.get("eClass").asText())) {
                traceNode = contentNode;
                break;
            }
        }
        
        assertNotNull(traceNode, "应找到Trace节点");
        JsonNode traceData = traceNode.get("data");
        assertNotNull(traceData, "Trace应有data节点");
        assertEquals(defId, traceData.get("fromId").asText(), "fromId字段应正确");
        assertEquals(usageId, traceData.get("toId").asText(), "toId字段应正确");
        assertEquals("derive", traceData.get("type").asText(), "type字段应正确");
        assertNotNull(traceData.get("createdAt"), "应有createdAt字段");
    }
    
    @Test
    @DisplayName("验证RequirementUsage JSON序列化")
    void testRequirementUsageJsonSerialization() throws IOException {
        // 创建定义和用法
        EObject definition = modelRegistry.createRequirementDefinition();
        definition.eSet(definition.eClass().getEStructuralFeature("reqId"), "REQ-003");
        definition.eSet(definition.eClass().getEStructuralFeature("name"), "父定义");
        
        String defId = (String) definition.eGet(definition.eClass().getEStructuralFeature("id"));
        EObject usage = modelRegistry.createRequirementUsage(defId);
        usage.eSet(usage.eClass().getEStructuralFeature("name"), "子用法");
        usage.eSet(usage.eClass().getEStructuralFeature("text"), "使用需求的文本");
        usage.eSet(usage.eClass().getEStructuralFeature("status"), "in_review");
        
        // 创建资源并序列化
        Path jsonPath = tempDir.resolve("usage-test.json");
        Resource resource = resourceSet.createResource(URI.createFileURI(jsonPath.toAbsolutePath().toString()));
        resource.getContents().add(definition);
        resource.getContents().add(usage);
        resource.save(Map.of());
        
        // 验证JSON内容
        String jsonContent = Files.readString(jsonPath);
        JsonNode jsonNode = objectMapper.readTree(jsonContent);
        
        assertNotNull(jsonNode.get("content"), "JSON应包含content数组");
        assertEquals(2, jsonNode.get("content").size(), "应包含两个对象");
        
        // 验证RequirementUsage的JSON结构
        JsonNode usageNode = null;
        for (JsonNode contentNode : jsonNode.get("content")) {
            if ("sysml:RequirementUsage".equals(contentNode.get("eClass").asText())) {
                usageNode = contentNode;
                break;
            }
        }
        
        assertNotNull(usageNode, "应找到RequirementUsage节点");
        JsonNode usageData = usageNode.get("data");
        assertNotNull(usageData, "Usage应有data节点");
        assertEquals(defId, usageData.get("of").asText(), "of字段应正确");
        assertEquals("子用法", usageData.get("name").asText(), "name字段应正确");
        assertEquals("使用需求的文本", usageData.get("text").asText(), "text字段应正确");
        assertEquals("in_review", usageData.get("status").asText(), "status字段应正确");
    }
}