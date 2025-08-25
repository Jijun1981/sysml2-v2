package com.sysml.mvp.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 导入Demo数据命令
 * 将demo-project.json中的数据转换为Pilot格式并导入到default项目
 */
@Component
public class ImportDemoDataCommand implements CommandLineRunner {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Autowired  
    private FileModelRepository repository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void run(String... args) throws Exception {
        // 检查是否应该运行导入 - 通过环境变量或程序参数
        boolean shouldImport = false;
        
        // 检查环境变量
        String importDemo = System.getProperty("import.demo");
        if ("true".equals(importDemo)) {
            shouldImport = true;
        }
        
        // 检查程序参数
        for (String arg : args) {
            if ("import-demo".equals(arg)) {
                shouldImport = true;
                break;
            }
        }
        
        // 临时：为了调试，总是执行导入
        shouldImport = true;
        
        if (!shouldImport) {
            return;
        }
        
        System.out.println("=== 开始导入Demo数据 ===");
        
        // 读取demo-project.json文件
        String demoFilePath = "/mnt/d/sysml2 v2/demo-project.json"; // 绝对路径
        if (!Files.exists(Paths.get(demoFilePath))) {
            System.out.println("找不到demo数据文件: " + demoFilePath);
            return;
        }
        
        String jsonContent = Files.readString(Paths.get(demoFilePath));
        Map<String, Object> demoData = objectMapper.readValue(jsonContent, Map.class);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) demoData.get("content");
        
        if (content == null || content.isEmpty()) {
            System.out.println("Demo数据为空");
            return;
        }
        
        // 加载default项目
        Resource resource = repository.loadProject("default");
        resource.getContents().clear(); // 清空现有数据
        
        int processedCount = 0;
        
        // 第一遍：创建所有对象，但不设置关系
        Map<String, EObject> objectMap = new HashMap<>();
        
        for (Map<String, Object> item : content) {
            try {
                String eClassName = (String) item.get("eClass");
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) item.get("data");
                
                if (eClassName != null && data != null) {
                    // 转换eClass名称：映射demo格式到Pilot标准类型
                    String pilotClassName = mapEClassName(eClassName);
                    
                    // 获取EClass
                    EClass eClass = (EClass) modelRegistry.getSysMLPackage().getEClassifier(pilotClassName);
                    if (eClass == null) {
                        System.out.println("未知的EClass: " + pilotClassName + " (原始: " + eClassName + ")");
                        continue;
                    }
                    
                    // 创建EMF对象
                    EObject eObject = eClass.getEPackage().getEFactoryInstance().create(eClass);
                    
                    // 设置基本属性（不包括关系）
                    setObjectProperties(eObject, data, false);
                    
                    // 存储到映射中
                    String elementId = (String) data.get("id");
                    if (elementId != null) {
                        objectMap.put(elementId, eObject);
                    }
                    
                    processedCount++;
                }
            } catch (Exception e) {
                System.out.println("处理元素失败: " + e.getMessage());
            }
        }
        
        // 第二遍：建立层次关系
        for (Map<String, Object> item : content) {
            try {
                String eClassName = (String) item.get("eClass");
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) item.get("data");
                
                if ("sysml:RequirementUsage".equals(eClassName) && data != null) {
                    String usageId = (String) data.get("id");
                    String parentId = (String) data.get("of");
                    
                    EObject usage = objectMap.get(usageId);
                    EObject parent = objectMap.get(parentId);
                    
                    if (usage != null && parent != null) {
                        // 建立包含关系：将Usage添加到Definition的ownedFeature中
                        establishContainment(parent, usage);
                    }
                }
            } catch (Exception e) {
                System.out.println("建立关系失败: " + e.getMessage());
            }
        }
        
        // 第三遍：只将顶级元素（没有父容器的）添加到资源
        for (EObject obj : objectMap.values()) {
            if (obj.eContainer() == null) {
                resource.getContents().add(obj);
            }
        }
        
        // 保存资源
        repository.saveProject("default", resource);
        
        System.out.println("=== Demo数据导入完成 ===");
        System.out.println("成功导入 " + processedCount + " 个元素");
        System.out.println("项目已保存到 default");
    }
    
    /**
     * 设置EMF对象的属性
     */
    private void setObjectProperties(EObject eObject, Map<String, Object> data) {
        setObjectProperties(eObject, data, true);
    }
    
    private void setObjectProperties(EObject eObject, Map<String, Object> data, boolean includeRelations) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            try {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // 映射字段名称
                String pilotFieldName = mapFieldName(key);
                
                var feature = eObject.eClass().getEStructuralFeature(pilotFieldName);
                if (feature != null && value != null) {
                    // 特殊处理不同字段类型
                    if ("elementId".equals(pilotFieldName) && value instanceof String) {
                        eObject.eSet(feature, value);
                    } else if ("declaredName".equals(pilotFieldName) && value instanceof String) {
                        eObject.eSet(feature, value);  
                    } else if ("declaredShortName".equals(pilotFieldName) && value instanceof String) {
                        eObject.eSet(feature, value);
                    } else if ("text".equals(key) && value instanceof String) {
                        // text字段映射到declaredName以显示有意义的内容，同时也创建documentation
                        var nameFeature = eObject.eClass().getEStructuralFeature("declaredName");
                        if (nameFeature != null) {
                            eObject.eSet(nameFeature, value); // 使用text内容作为显示名称
                        }
                        // 同时创建Documentation
                        handleTextDocumentation(eObject, (String) value);
                    } else if ("doc".equals(key) && value instanceof String) {
                        // doc字段处理为documentation
                        handleTextDocumentation(eObject, (String) value);
                    } else if ("of".equals(key) && value instanceof String && includeRelations) {
                        // RequirementUsage的of字段，建立包含关系（在第二遍处理）
                        handleOfReference(eObject, (String) value);
                    } else if ("fromId".equals(key) && value instanceof String) {
                        // Trace关系的fromId映射到source
                        handleTraceSource(eObject, (String) value);
                    } else if ("toId".equals(key) && value instanceof String) {
                        // Trace关系的toId映射到target
                        handleTraceTarget(eObject, (String) value);
                    } else if ("type".equals(key) && "Satisfy".equals(eObject.eClass().getName())) {
                        // Satisfy关系的type字段，忽略或记录
                        System.out.println("Satisfy关系类型: " + value);
                    } else if (feature != null && value != null) {
                        // 其他字段的默认处理
                        try {
                            eObject.eSet(feature, value);
                        } catch (Exception e) {
                            System.out.println("无法设置字段 " + pilotFieldName + ": " + e.getMessage());
                        }
                    }
                }
                
                // 确保每个对象都有elementId
                var elementIdFeature = eObject.eClass().getEStructuralFeature("elementId");
                if (elementIdFeature != null && eObject.eGet(elementIdFeature) == null) {
                    String existingId = (String) data.get("id");
                    if (existingId != null) {
                        eObject.eSet(elementIdFeature, existingId);
                    } else {
                        // 生成新ID
                        String newId = eObject.eClass().getName().toLowerCase() + "-" + UUID.randomUUID().toString();
                        eObject.eSet(elementIdFeature, newId);
                    }
                }
            } catch (Exception e) {
                // 忽略无法设置的属性
            }
        }
    }
    
    /**
     * 映射EClass名称：从demo格式到Pilot格式
     */
    private String mapEClassName(String demoClassName) {
        switch (demoClassName) {
            case "sysml:RequirementDefinition": return "RequirementDefinition";
            case "sysml:RequirementUsage": return "RequirementUsage";
            case "sysml:Trace": return "Dependency"; // Trace映射到Dependency关系
            default: 
                // 去掉sysml:前缀作为默认处理
                return demoClassName.replace("sysml:", "");
        }
    }
    
    /**
     * 映射字段名称：从demo格式到Pilot格式
     */
    private String mapFieldName(String demoField) {
        switch (demoField) {
            case "id": return "elementId";
            case "name": return "declaredName"; // name保持通用名称
            case "reqId": return "declaredShortName";
            case "doc": return "documentation";
            case "text": return "text"; // 需要特殊处理，映射到documentation的body
            default: return demoField;
        }
    }
    
    /**
     * 建立包含关系：将child添加到parent的适当容器中
     */
    private void establishContainment(EObject parent, EObject child) {
        try {
            // 查找合适的包含特性
            var ownedFeatureFeature = parent.eClass().getEStructuralFeature("ownedFeature");
            var ownedMemberFeature = parent.eClass().getEStructuralFeature("ownedMember");
            
            if (ownedFeatureFeature != null) {
                Object ownedFeatures = parent.eGet(ownedFeatureFeature);
                if (ownedFeatures instanceof List) {
                    ((List<Object>) ownedFeatures).add(child);
                    System.out.println("已将 " + getObjectId(child) + " 添加到 " + getObjectId(parent) + " 的ownedFeature中");
                    return;
                }
            }
            
            if (ownedMemberFeature != null) {
                Object ownedMembers = parent.eGet(ownedMemberFeature);
                if (ownedMembers instanceof List) {
                    ((List<Object>) ownedMembers).add(child);
                    System.out.println("已将 " + getObjectId(child) + " 添加到 " + getObjectId(parent) + " 的ownedMember中");
                    return;
                }
            }
            
            System.out.println("警告：无法为 " + parent.eClass().getName() + " 找到合适的包含特性");
        } catch (Exception e) {
            System.out.println("建立包含关系失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理text/doc字段：创建Documentation对象并设置body
     */
    private void handleTextDocumentation(EObject eObject, String text) {
        try {
            // 查找documentation字段
            var docFeature = eObject.eClass().getEStructuralFeature("documentation");
            if (docFeature != null) {
                // 创建Documentation对象
                var docEClass = modelRegistry.getSysMLPackage().getEClassifier("Documentation");
                if (docEClass instanceof EClass) {
                    var docObject = docEClass.getEPackage().getEFactoryInstance().create((EClass) docEClass);
                    
                    // 设置body字段
                    var bodyFeature = docObject.eClass().getEStructuralFeature("body");
                    if (bodyFeature != null) {
                        docObject.eSet(bodyFeature, text);
                    }
                    
                    // 将Documentation添加到对象的documentation列表
                    Object docValue = eObject.eGet(docFeature);
                    if (docValue instanceof List) {
                        ((List<Object>) docValue).add(docObject);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("无法处理documentation字段: " + e.getMessage());
        }
    }
    
    /**
     * 处理RequirementUsage的of字段：建立包含关系
     */
    private void handleOfReference(EObject eObject, String ofId) {
        // 暂时存储为字符串引用，后续可以建立实际的EMF引用
        // 这里简化处理，实际应该查找对应的Definition并建立包含关系
        System.out.println("RequirementUsage " + getObjectId(eObject) + " references Definition " + ofId);
    }
    
    /**
     * 处理Trace关系的source
     */
    private void handleTraceSource(EObject eObject, String sourceId) {
        var sourceFeature = eObject.eClass().getEStructuralFeature("source");
        if (sourceFeature != null) {
            // 暂时存储为字符串，实际应该解析为对象引用
            eObject.eSet(sourceFeature, sourceId);
        }
    }
    
    /**
     * 处理Trace关系的target
     */
    private void handleTraceTarget(EObject eObject, String targetId) {
        var targetFeature = eObject.eClass().getEStructuralFeature("target");
        if (targetFeature != null) {
            // 暂时存储为字符串，实际应该解析为对象引用
            eObject.eSet(targetFeature, targetId);
        }
    }
    
    /**
     * 获取对象ID的辅助方法
     */
    private String getObjectId(EObject obj) {
        var idFeature = obj.eClass().getEStructuralFeature("elementId");
        if (idFeature != null) {
            Object id = obj.eGet(idFeature);
            return id != null ? id.toString() : null;
        }
        return null;
    }
}