package com.sysml.mvp;

import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import com.sysml.mvp.service.PilotEMFService;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心EMF功能简化测试
 * 快速验证核心功能是否正常
 */
@SpringBootTest
public class EMFCoreSimpleTest {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Autowired
    private FileModelRepository fileRepository;
    
    @Autowired
    private PilotEMFService pilotEMFService;
    
    /**
     * TEST-B1-1: 验证元模型注册
     */
    @Test
    public void testMetamodelRegistration() {
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        assertNotNull(sysmlPackage, "SysML包应该已注册");
        assertEquals(182, sysmlPackage.getEClassifiers().size(), "应该有182个EClass");
        System.out.println("✅ 元模型注册成功: 182个EClass");
    }
    
    /**
     * TEST-B2-1: 验证创建API
     */
    @Test
    public void testCreateObjects() {
        // 测试创建RequirementDefinition
        EObject reqDef = pilotEMFService.createRequirementDefinition(
            "REQ-001", "需求1", "需求描述");
        assertNotNull(reqDef);
        assertEquals("RequirementDefinition", reqDef.eClass().getName());
        System.out.println("✅ 创建RequirementDefinition成功");
        
        // 测试通用创建方法
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("declaredName", "部件定义");
        EObject partDef = pilotEMFService.createElement("PartDefinition", attrs);
        assertNotNull(partDef);
        assertEquals("PartDefinition", partDef.eClass().getName());
        System.out.println("✅ 创建PartDefinition成功");
    }
    
    /**
     * TEST-B1-3: 验证保存和加载
     */
    @Test
    public void testSaveAndLoad() throws Exception {
        String projectId = "test-" + System.currentTimeMillis();
        
        // 创建对象
        EObject reqDef = pilotEMFService.createRequirementDefinition(
            "TEST-REQ", "测试需求", "测试描述");
        
        // 保存
        Resource resource = new ResourceImpl();
        resource.getContents().add(reqDef);
        fileRepository.saveProject(projectId, resource);
        System.out.println("✅ 保存项目成功: " + projectId);
        
        // 加载
        Resource loaded = fileRepository.loadProject(projectId);
        assertNotNull(loaded);
        assertEquals(1, loaded.getContents().size());
        System.out.println("✅ 加载项目成功，包含1个对象");
        
        // 验证属性
        EObject loadedObj = loaded.getContents().get(0);
        assertEquals("TEST-REQ", 
            pilotEMFService.getAttributeValue(loadedObj, "declaredShortName"));
        System.out.println("✅ 属性验证成功");
    }
}