package com.sysml.mvp.command;

import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * 验证导入后的数据结构和层次关系
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DataStructureVerificationTest {

    @Autowired
    private ImportDemoDataCommand importCommand;
    
    @Autowired
    private FileModelRepository repository;
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Test
    public void verifyImportedDataStructure() throws Exception {
        // 执行导入
        importCommand.run("import-demo");
        
        // 加载数据
        Resource resource = repository.loadProject("default");
        
        System.out.println("=== 导入后数据结构验证 ===");
        System.out.println("根级元素数量: " + resource.getContents().size());
        
        // 分析根级元素
        resource.getContents().forEach(obj -> {
            String type = obj.eClass().getName();
            String id = getElementId(obj);
            String name = getDeclaredName(obj);
            
            System.out.println("\n根级元素:");
            System.out.println("  类型: " + type);
            System.out.println("  ID: " + id);
            System.out.println("  名称: " + name);
            
            // 检查是否有子元素
            if ("RequirementDefinition".equals(type)) {
                checkOwnedElements(obj, "  ");
            }
        });
        
        // 验证text字段映射到名称
        resource.getContents().stream()
            .filter(obj -> "RequirementDefinition".equals(obj.eClass().getName()))
            .findFirst()
            .ifPresent(def -> {
                String name = getDeclaredName(def);
                System.out.println("\n第一个RequirementDefinition的名称: " + name);
                System.out.println("是否包含有意义内容: " + (name != null && name.contains("用户")));
            });
    }
    
    private void checkOwnedElements(EObject parent, String indent) {
        // 检查ownedFeature
        var ownedFeatureFeature = parent.eClass().getEStructuralFeature("ownedFeature");
        if (ownedFeatureFeature != null) {
            Object ownedFeatures = parent.eGet(ownedFeatureFeature);
            if (ownedFeatures instanceof java.util.List) {
                java.util.List<?> features = (java.util.List<?>) ownedFeatures;
                System.out.println(indent + "ownedFeature数量: " + features.size());
                
                features.forEach(feature -> {
                    if (feature instanceof EObject) {
                        EObject child = (EObject) feature;
                        System.out.println(indent + "  子元素: " + child.eClass().getName() + 
                            " ID=" + getElementId(child) + 
                            " Name=" + getDeclaredName(child));
                    }
                });
            }
        }
        
        // 检查ownedMember
        var ownedMemberFeature = parent.eClass().getEStructuralFeature("ownedMember");
        if (ownedMemberFeature != null) {
            Object ownedMembers = parent.eGet(ownedMemberFeature);
            if (ownedMembers instanceof java.util.List) {
                java.util.List<?> members = (java.util.List<?>) ownedMembers;
                System.out.println(indent + "ownedMember数量: " + members.size());
            }
        }
    }
    
    private String getElementId(EObject obj) {
        var idFeature = obj.eClass().getEStructuralFeature("elementId");
        if (idFeature != null) {
            Object id = obj.eGet(idFeature);
            return id != null ? id.toString() : "null";
        }
        return "no-id-field";
    }
    
    private String getDeclaredName(EObject obj) {
        var nameFeature = obj.eClass().getEStructuralFeature("declaredName");
        if (nameFeature != null) {
            Object name = obj.eGet(nameFeature);
            return name != null ? name.toString() : "null";
        }
        return "no-name-field";
    }
}